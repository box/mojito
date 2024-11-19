package com.box.l10n.mojito.service.thirdparty;

import static com.box.l10n.mojito.android.strings.AndroidPluralQuantity.MANY;
import static com.box.l10n.mojito.service.thirdparty.smartling.SmartlingFileUtils.getOutputSourceFile;
import static com.box.l10n.mojito.service.thirdparty.smartling.SmartlingFileUtils.getOutputTargetFile;
import static com.box.l10n.mojito.service.thirdparty.smartling.SmartlingFileUtils.isPluralFile;
import static com.google.common.collect.Streams.mapWithIndex;

import com.box.l10n.mojito.android.strings.AndroidStringDocumentMapper;
import com.box.l10n.mojito.android.strings.AndroidStringDocumentWriter;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.ai.translation.AITranslationConfiguration;
import com.box.l10n.mojito.service.ai.translation.AITranslationService;
import com.box.l10n.mojito.service.assetExtraction.AssetTextUnitToTMTextUnitRepository;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingFile;
import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingFileUtils;
import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingOptions;
import com.box.l10n.mojito.service.thirdparty.smartling.SmartlingResultProcessor;
import com.box.l10n.mojito.service.thirdparty.smartling.quartz.SmartlingPullTranslationsJob;
import com.box.l10n.mojito.service.thirdparty.smartling.quartz.SmartlingPullTranslationsJobInput;
import com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService;
import com.box.l10n.mojito.service.tm.search.SearchType;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import com.box.l10n.mojito.smartling.AssetPathAndTextUnitNameKeys;
import com.box.l10n.mojito.smartling.SmartlingClient;
import com.box.l10n.mojito.smartling.SmartlingClientException;
import com.box.l10n.mojito.smartling.request.Binding;
import com.box.l10n.mojito.smartling.request.Bindings;
import com.box.l10n.mojito.smartling.response.File;
import com.box.l10n.mojito.smartling.response.StringInfo;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * tmTextUnitId are not preserved in Smartling plural localized files so we have to import based on
 * name which causes some challenges when there are ambiguities (eg. same name different comment).
 *
 * <p>In singular file, the id is preserved hence used during import.
 *
 * <p>Smartling accept android files with entries where the name is dupplicated. It maps to a single
 * text unit in Smartling. Both entries get the same translation in the localized files.
 *
 * <p>There is no constrain in mojito database that insure the plural text units are valid. This can
 * cause issue with the current implementation. we group by plural form other and the comment.
 */
@ConditionalOnProperty(value = "l10n.ThirdPartyTMS.impl", havingValue = "ThirdPartyTMSSmartling")
@Component
public class ThirdPartyTMSSmartling implements ThirdPartyTMS {

  public static final String PUBLISHED = "PUBLISHED";
  public static final String POST_TRANSLATION = "POST_TRANSLATION";

  static Logger logger = LoggerFactory.getLogger(ThirdPartyTMSSmartling.class);

  /*
  Devisible by 6 so that the plurals won't be broken up between files
  We force pull all 6 forms regardless of language so that the files
  contain the same keys and can be properly connected to their source
  file in smartling.
  */
  private static final int DEFAULT_BATCH_SIZE = 5004;
  private static final String LOCALE_EN = "en";

  private final SmartlingClient smartlingClient;
  private final AssetPathAndTextUnitNameKeys assetPathAndTextUnitNameKeys;
  TextUnitSearcher textUnitSearcher;
  private final TextUnitBatchImporterService textUnitBatchImporterService;
  private final SmartlingResultProcessor resultProcessor;
  private final Integer batchSize;
  private final ThirdPartyTMSSmartlingWithJson thirdPartyTMSSmartlingWithJson;
  private final ThirdPartyTMSSmartlingGlossary thirdPartyTMSSmartlingGlossary;
  private final AssetTextUnitToTMTextUnitRepository assetTextUnitToTMTextUnitRepository;
  private final AITranslationService aiTranslationService;

  private final MeterRegistry meterRegistry;

  private final QuartzPollableTaskScheduler quartzPollableTaskScheduler;

  private final AITranslationConfiguration aiTranslationConfiguration;

  private final Set<String> supportedImageExtensions =
      Sets.newHashSet("png", "jpg", "jpeg", "gif", "tiff");

  protected static String getSmartlingLocale(Map<String, String> localeMapping, String localeTag) {
    return localeMapping.getOrDefault(localeTag, localeTag);
  }

  @Autowired
  public ThirdPartyTMSSmartling(
      SmartlingClient smartlingClient,
      TextUnitSearcher textUnitSearcher,
      AssetPathAndTextUnitNameKeys assetPathAndTextUnitNameKeys,
      TextUnitBatchImporterService textUnitBatchImporterService,
      SmartlingResultProcessor resultProcessor,
      ThirdPartyTMSSmartlingWithJson thirdPartyTMSSmartlingWithJson,
      ThirdPartyTMSSmartlingGlossary thirdPartyTMSSmartlingGlossary,
      AssetTextUnitToTMTextUnitRepository assetTextUnitToTMTextUnitRepository,
      MeterRegistry meterRegistry,
      QuartzPollableTaskScheduler quartzPollableTaskScheduler,
      AITranslationConfiguration aiTranslationConfiguration,
      @Autowired(required = false) AITranslationService aiTranslationService) {
    this(
        smartlingClient,
        textUnitSearcher,
        assetPathAndTextUnitNameKeys,
        textUnitBatchImporterService,
        resultProcessor,
        thirdPartyTMSSmartlingWithJson,
        thirdPartyTMSSmartlingGlossary,
        assetTextUnitToTMTextUnitRepository,
        DEFAULT_BATCH_SIZE,
        meterRegistry,
        quartzPollableTaskScheduler,
        aiTranslationConfiguration,
        aiTranslationService);
  }

  public ThirdPartyTMSSmartling(
      SmartlingClient smartlingClient,
      TextUnitSearcher textUnitSearcher,
      AssetPathAndTextUnitNameKeys assetPathAndTextUnitNameKeys,
      TextUnitBatchImporterService textUnitBatchImporterService,
      SmartlingResultProcessor resultProcessor,
      ThirdPartyTMSSmartlingWithJson thirdPartyTMSSmartlingWithJson,
      ThirdPartyTMSSmartlingGlossary thirdPartyTMSSmartlingGlossary,
      AssetTextUnitToTMTextUnitRepository assetTextUnitToTMTextUnitRepository,
      int batchSize,
      MeterRegistry meterRegistry,
      QuartzPollableTaskScheduler quartzPollableTaskScheduler,
      AITranslationConfiguration aiTranslationConfiguration,
      AITranslationService aiTranslationService) {
    this.smartlingClient = smartlingClient;
    this.assetPathAndTextUnitNameKeys = assetPathAndTextUnitNameKeys;
    this.textUnitBatchImporterService = textUnitBatchImporterService;
    this.textUnitSearcher = textUnitSearcher;
    this.resultProcessor = resultProcessor;
    this.batchSize = batchSize < 1 ? DEFAULT_BATCH_SIZE : batchSize;
    this.thirdPartyTMSSmartlingWithJson = thirdPartyTMSSmartlingWithJson;
    this.thirdPartyTMSSmartlingGlossary = thirdPartyTMSSmartlingGlossary;
    this.assetTextUnitToTMTextUnitRepository = assetTextUnitToTMTextUnitRepository;
    this.meterRegistry = meterRegistry;
    this.quartzPollableTaskScheduler = quartzPollableTaskScheduler;
    this.aiTranslationConfiguration = aiTranslationConfiguration;
    this.aiTranslationService = aiTranslationService;
  }

  @Override
  public void removeImage(String projectId, String imageId) {
    logger.debug(
        "remove image (screenshot) from Smartling, project id: {}, imageId: {}",
        projectId,
        imageId);

    Mono.fromRunnable(() -> smartlingClient.deleteContext(projectId, imageId))
        .retryWhen(
            smartlingClient
                .getRetryConfiguration()
                .doBeforeRetry(
                    e ->
                        logger.info(
                            String.format(
                                "Retrying remove image (screenshot) from Smartling; projectId %s, imageId %s",
                                projectId, imageId),
                            e.failure())))
        .doOnError(
            e -> {
              String msg =
                  String.format(
                      "Error removing image (screenshot) from Smartling; projectId %s, imageId %s",
                      projectId, imageId);
              logger.error(msg, e);
              throw new SmartlingClientException(msg, e);
            })
        .block();
  }

  @Override
  @Timed("SmartlingSync.uploadImage")
  public ThirdPartyTMSImage uploadImage(String projectId, String name, byte[] content) {
    logger.debug("Upload image to Smartling, project id: {}, name: {}", projectId, name);
    if (!isImageExtensionSupported(name)) {
      logger.warn(
          "Skipping upload of {} in project {} to Smartling as image format is not supported",
          name,
          projectId);
      throw new UnsupportedImageFormatException(
          Files.getFileExtension(name) + " is an unsupported file extension.");
    }
    return Mono.fromCallable(() -> smartlingClient.uploadContext(projectId, name, content))
        .retryWhen(
            smartlingClient
                .getRetryConfiguration()
                .doBeforeRetry(
                    e ->
                        logger.info(
                            "Retrying after image upload to Smartling failed, project id: {}, name: {}, error: {}",
                            projectId,
                            name,
                            e.failure())))
        .doOnError(
            e -> {
              String msg =
                  String.format(
                      "Error uploading image to Smartling, project id: %s, name: %s",
                      projectId, name);
              logger.error(msg, e);
              throw new SmartlingClientException(msg, e);
            })
        .blockOptional()
        .map(
            contextUp -> {
              ThirdPartyTMSImage thirdPartyTMSImage = new ThirdPartyTMSImage();
              thirdPartyTMSImage.setId(contextUp.getContextUid());
              return thirdPartyTMSImage;
            })
        .orElseThrow(
            () ->
                new SmartlingClientException(
                    "Error with image upload to Smartling, context upload information is not present."));
  }

  @Override
  @Timed("SmartlingSync.getThirdPartyTextUnits")
  public List<ThirdPartyTextUnit> getThirdPartyTextUnits(
      Repository repository, String projectId, List<String> optionList) {

    SmartlingOptions options = SmartlingOptions.parseList(optionList);

    if (options.isJsonSync()) {
      return thirdPartyTMSSmartlingWithJson.getThirdPartyTextUnits(
          repository, projectId, optionList);
    }

    if (options.isGlossarySync()) {
      return thirdPartyTMSSmartlingGlossary.getThirdPartyTextUnits(projectId);
    }

    logger.debug(
        "Get third party text units for repository: {} and project id: {}",
        repository.getId(),
        projectId);

    Pattern filePattern = SmartlingFileUtils.getFilePattern(repository.getName());

    List<File> files =
        Mono.fromCallable(
                () ->
                    smartlingClient.getFiles(projectId).getItems().stream()
                        .filter(file -> filePattern.matcher(file.getFileUri()).matches())
                        .collect(Collectors.toList()))
            .retryWhen(
                smartlingClient
                    .getRetryConfiguration()
                    .doBeforeRetry(
                        e ->
                            logger.info(
                                String.format(
                                    "Retrying after failure to get files from Smartling for project id: %s",
                                    projectId),
                                e.failure())))
            .doOnError(
                e -> {
                  String msg =
                      String.format(
                          "Error getting files from Smartling for project id: %s", projectId);
                  logger.error(msg, e);
                  throw new SmartlingClientException(msg, e);
                })
            .blockOptional()
            .orElseThrow(
                () ->
                    new SmartlingClientException(
                        String.format(
                            "Error getting files from Smartling for project id: %s, files optional is not present.",
                            projectId)));

    List<ThirdPartyTextUnit> thirdPartyTextUnits =
        files.stream()
            .flatMap(
                file -> {
                  Stream<StringInfo> stringInfos =
                      Mono.fromCallable(
                              () -> smartlingClient.getStringInfos(projectId, file.getFileUri()))
                          .retryWhen(
                              smartlingClient
                                  .getRetryConfiguration()
                                  .doBeforeRetry(
                                      e ->
                                          logger.info(
                                              String.format(
                                                  "Retrying after failure to get string information from Smartling for project id: %s, file: %s",
                                                  projectId, file.getFileUri()),
                                              e.failure())))
                          .doOnError(
                              e -> {
                                String msg =
                                    String.format(
                                        "Error getting string information from Smartling for project id: %s, file: %s",
                                        projectId, file.getFileUri());
                                logger.error(msg, e);
                                throw new SmartlingClientException(msg, e);
                              })
                          .blockOptional()
                          .orElseThrow(
                              () ->
                                  new SmartlingClientException(
                                      String.format(
                                          "Error getting string information from Smartling for projectId: %s, string infos stream is not present",
                                          projectId)));

                  return stringInfos.map(
                      stringInfo -> {
                        logger.debug(
                            "hashcode: {}\nvariant: {}\nparsed string: {}",
                            stringInfo.getHashcode(),
                            stringInfo.getStringVariant(),
                            stringInfo.getParsedStringText());

                        AssetPathAndTextUnitNameKeys.Key key =
                            assetPathAndTextUnitNameKeys.parse(stringInfo.getStringVariant());
                        ThirdPartyTextUnit thirdPartyTextUnit = new ThirdPartyTextUnit();
                        thirdPartyTextUnit.setId(stringInfo.getHashcode());
                        thirdPartyTextUnit.setAssetPath(key.getAssetPath());
                        thirdPartyTextUnit.setName(key.getTextUnitName());
                        thirdPartyTextUnit.setNamePluralPrefix(isPluralFile(file.getFileUri()));
                        thirdPartyTextUnit.setUploadedFileUri(file.getFileUri());

                        return thirdPartyTextUnit;
                      });
                })
            .collect(Collectors.toList());

    return thirdPartyTextUnits;
  }

  @Override
  @Timed("SmartlingSync.createImageToTextUnitMappings")
  public void createImageToTextUnitMappings(
      String projectId, List<ThirdPartyImageToTextUnit> thirdPartyImageToTextUnits) {
    logger.debug("Upload image to text units mapping for project id: {}", projectId);
    Bindings bindings = new Bindings();

    List<Binding> bindingList =
        thirdPartyImageToTextUnits.stream()
            .map(
                thirdPartyImageToTextUnit -> {
                  Binding binding = new Binding();
                  binding.setStringHashcode(thirdPartyImageToTextUnit.getTextUnitId());
                  binding.setContextUid(thirdPartyImageToTextUnit.getImageId());
                  return binding;
                })
            .collect(Collectors.toList());

    bindings.setBindings(bindingList);
    Mono.fromRunnable(() -> smartlingClient.createBindings(bindings, projectId))
        .retryWhen(
            smartlingClient
                .getRetryConfiguration()
                .doBeforeRetry(
                    e ->
                        logger.info(
                            String.format(
                                "Retrying after failure in createBindings Smartling call for project: %s",
                                projectId),
                            e.failure())))
        .doOnError(
            e -> {
              String msg =
                  String.format(
                      "Error attempting to create bindings in Smartling for project: %s",
                      projectId);
              logger.error(msg, e);
              throw new SmartlingClientException(msg, e);
            })
        .block();
  }

  @Override
  public void push(
      Repository repository,
      String projectId,
      String pluralSeparator,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern,
      List<String> optionList) {

    try (var timer =
        Timer.resource(meterRegistry, "SmartlingSync.push")
            .tag("repository", repository.getName())) {
      SmartlingOptions options = SmartlingOptions.parseList(optionList);

      if (options.isJsonSync()) {
        thirdPartyTMSSmartlingWithJson.push(
            repository,
            projectId,
            pluralSeparator,
            skipTextUnitsWithPattern,
            skipAssetsWithPathPattern,
            options);
        return;
      }

      AndroidStringDocumentMapper mapper = new AndroidStringDocumentMapper(pluralSeparator, null);

      Stream<SmartlingFile> singularFiles =
          mapWithIndex(
              partitionSingulars(
                  repository.getId(),
                  LOCALE_EN,
                  skipTextUnitsWithPattern,
                  skipAssetsWithPathPattern),
              (batch, index) ->
                  processPushBatch(
                      batch, index, mapper, repository, projectId, options, Prefix.SINGULAR));

      Stream<SmartlingFile> pluralFiles =
          mapWithIndex(
              partitionPlurals(
                  repository.getId(),
                  LOCALE_EN,
                  skipTextUnitsWithPattern,
                  skipAssetsWithPathPattern),
              (batch, index) ->
                  processPushBatch(
                      batch, index, mapper, repository, projectId, options, Prefix.PLURAL));

      List<SmartlingFile> result =
          Stream.concat(singularFiles, pluralFiles).collect(Collectors.toList());
      resultProcessor.processPush(result, options);
    }
  }

  private SmartlingFile processPushBatch(
      List<TextUnitDTO> result,
      long batchNumber,
      AndroidStringDocumentMapper mapper,
      Repository repository,
      String projectId,
      SmartlingOptions options,
      Prefix filePrefix) {

    try (var timer =
        Timer.resource(meterRegistry, "SmartlingSync.processPushBatch")
            .tag("repository", repository.getName())) {
      return uploadTextUnitsToSmartling(
          result, batchNumber, mapper, repository, projectId, options, filePrefix);
    }
  }

  private SmartlingFile uploadTextUnitsToSmartling(
      List<TextUnitDTO> result,
      long batchNumber,
      AndroidStringDocumentMapper mapper,
      Repository repository,
      String projectId,
      SmartlingOptions options,
      Prefix filePrefix) {
    logger.debug("Convert text units to AndroidString for asset number: {}", batchNumber);
    SmartlingFile file = new SmartlingFile();
    file.setFileName(getOutputSourceFile(batchNumber, repository.getName(), filePrefix.getType()));

    try {

      logger.debug("Save source file to: {}", file.getFileName());
      AndroidStringDocumentWriter writer =
          new AndroidStringDocumentWriter(mapper.readFromSourceTextUnits(result));
      file.setFileContent(writer.toText());

    } catch (ParserConfigurationException | TransformerException e) {
      logger.error("An error ocurred when processing a push batch", e);
      throw new RuntimeException(e);
    }

    if (!options.isDryRun()) {
      Mono.fromCallable(
              () ->
                  smartlingClient.uploadFile(
                      projectId,
                      file.getFileName(),
                      "android",
                      file.getFileContent(),
                      options.getPlaceholderFormat(),
                      options.getCustomPlaceholderFormat(),
                      options.getStringFormat()))
          .retryWhen(
              smartlingClient
                  .getRetryConfiguration()
                  .doBeforeRetry(
                      e ->
                          logger.info(
                              String.format(
                                  "Retrying after failure to upload file %s", file.getFileName()),
                              e.failure())))
          .doOnError(
              e -> {
                String msg =
                    String.format(
                        "Error uploading file %s: %s", file.getFileName(), e.getMessage());
                logger.error(msg, e);
                throw new SmartlingClientException(msg, e);
              })
          .block();
    }
    return file;
  }

  @Override
  public PollableFuture<Void> pull(
      Repository repository,
      String projectId,
      String pluralSeparator,
      Map<String, String> localeMapping,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern,
      List<String> optionList,
      String schedulerName,
      PollableTask currentTask) {

    SmartlingOptions options = SmartlingOptions.parseList(optionList);

    try (var timer =
        Timer.resource(meterRegistry, "SmartlingSync.pull")
            .tag("repository", repository.getName())
            .tag("deltaPull", Boolean.toString(options.isDeltaPull()))) {

      if (options.isJsonSync()) {
        thirdPartyTMSSmartlingWithJson.pull(
            repository, projectId, localeMapping, options.isDeltaPull());
        return null;
      }

      if (options.isGlossarySync()) {
        thirdPartyTMSSmartlingGlossary.pull(repository, projectId, localeMapping);
        return null;
      }

      return schedulePullTranslationsJob(
          repository,
          projectId,
          pluralSeparator,
          localeMapping,
          skipTextUnitsWithPattern,
          skipAssetsWithPathPattern,
          schedulerName,
          currentTask,
          options);
    }
  }

  private PollableFuture<Void> schedulePullTranslationsJob(
      Repository repository,
      String projectId,
      String pluralSeparator,
      Map<String, String> localeMapping,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern,
      String schedulerName,
      PollableTask currentTask,
      SmartlingOptions options) {
    Long singulars =
        singularCount(
            repository.getId(), LOCALE_EN, skipTextUnitsWithPattern, skipAssetsWithPathPattern);

    Long plurals =
        pluralCount(
            repository.getId(), LOCALE_EN, skipTextUnitsWithPattern, skipAssetsWithPathPattern);

    SmartlingPullTranslationsJobInput input = new SmartlingPullTranslationsJobInput();
    input.setBatchSize(batchSize);
    input.setRepositoryName(repository.getName());
    input.setPluralSeparator(pluralSeparator);
    input.setDeltaPull(options.isDeltaPull());
    input.setDryRun(options.isDryRun());
    input.setProjectId(projectId);
    input.setPluralCount(plurals);
    input.setPluralFixForLocale(
        options.getPluralFixForLocales().stream().collect(Collectors.joining(",")));
    input.setLocaleMapping(
        localeMapping.keySet().stream()
            .map(key -> key + ":" + localeMapping.get(key))
            .collect(Collectors.joining(",")));
    input.setSchedulerName(schedulerName);
    input.setSingularCount(singulars);
    QuartzJobInfo<SmartlingPullTranslationsJobInput, Void> quartzJobInfo =
        QuartzJobInfo.newBuilder(SmartlingPullTranslationsJob.class)
            .withInput(input)
            .withScheduler(schedulerName)
            .withParentId(currentTask.getId())
            .withInlineInput(false)
            .build();

    return quartzPollableTaskScheduler.scheduleJob(quartzJobInfo);
  }

  @Override
  public void pushTranslations(
      Repository repository,
      String projectId,
      String pluralSeparator,
      Map<String, String> localeMapping,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern,
      String includeTextUnitsWithPattern,
      List<String> optionList) {

    try (var timer =
        Timer.resource(meterRegistry, "SmartlingSync.pushTranslations")
            .tag("repository", repository.getName())) {
      SmartlingOptions options = SmartlingOptions.parseList(optionList);
      if (options.isJsonSync()) {
        thirdPartyTMSSmartlingWithJson.pushTranslations(
            repository,
            projectId,
            pluralSeparator,
            localeMapping,
            skipTextUnitsWithPattern,
            skipAssetsWithPathPattern,
            includeTextUnitsWithPattern,
            options);
        return;
      }

      List<SmartlingFile> result;

      AndroidStringDocumentMapper mapper = new AndroidStringDocumentMapper(pluralSeparator, null);

      Set<Long> filterTmTextUnitIds = getFilterTmTextUnitIdsForPushTranslation(options);

      result =
          repository.getRepositoryLocales().stream()
              .map(l -> l.getLocale().getBcp47Tag())
              .filter(
                  localeTag ->
                      !localeTag.equalsIgnoreCase(repository.getSourceLocale().getBcp47Tag()))
              .flatMap(
                  localeTag ->
                      Stream.concat(
                          mapWithIndex(
                              partitionSingulars(
                                  repository.getId(),
                                  localeTag,
                                  skipTextUnitsWithPattern,
                                  skipAssetsWithPathPattern,
                                  includeTextUnitsWithPattern),
                              (list, batch) ->
                                  processTranslationBatch(
                                      list,
                                      batch,
                                      localeTag,
                                      mapper,
                                      repository,
                                      projectId,
                                      options,
                                      localeMapping,
                                      Prefix.SINGULAR,
                                      filterTmTextUnitIds,
                                      PUBLISHED)),
                          mapWithIndex(
                              partitionPlurals(
                                  repository.getId(),
                                  localeTag,
                                  skipTextUnitsWithPattern,
                                  skipAssetsWithPathPattern,
                                  options.getPluralFixForLocales(),
                                  includeTextUnitsWithPattern),
                              (list, batch) ->
                                  processTranslationBatch(
                                      list,
                                      batch,
                                      localeTag,
                                      mapper,
                                      repository,
                                      projectId,
                                      options,
                                      localeMapping,
                                      Prefix.PLURAL,
                                      filterTmTextUnitIds,
                                      PUBLISHED))))
              .collect(Collectors.toList());

      resultProcessor.processPushTranslations(result, options);
    }
  }

  private Set<Long> getFilterTmTextUnitIdsForPushTranslation(SmartlingOptions options) {
    Set<Long> filterTmTextUnitIds = null;
    if (options.getPushTranslationBranchName() != null) {
      filterTmTextUnitIds =
          assetTextUnitToTMTextUnitRepository
              .findByBranchName(options.getPushTranslationBranchName())
              .stream()
              .collect(Collectors.toSet());
    }
    return filterTmTextUnitIds;
  }

  @Override
  public void pullSource(
      Repository repository,
      String thirdPartyProjectId,
      List<String> optionList,
      Map<String, String> localeMapping) {
    SmartlingOptions options = SmartlingOptions.parseList(optionList);
    if (options.isGlossarySync()) {
      try (var timer =
          Timer.resource(meterRegistry, "SmartlingSync.pullSource")
              .tags("repository", repository.getName())) {

        thirdPartyTMSSmartlingGlossary.pullSourceTextUnits(
            repository, thirdPartyProjectId, localeMapping);
      }
    } else {
      throw new UnsupportedOperationException(
          "Pull source is only supported with glossary sync enabled.");
    }
  }

  @Override
  public void pushAITranslations(
      Repository repository,
      String projectId,
      String pluralSeparator,
      Map<String, String> localeMapping,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern,
      String includeTextUnitsWithPattern,
      List<String> optionList) {
    if (aiTranslationService == null) {
      throw new UnsupportedOperationException(
          "AI translation service is not supported as no AITranslationService bean is configured.");
    }
    try (var timer =
        Timer.resource(meterRegistry, "SmartlingSync.pushAITranslations")
            .tag("repository", repository.getName())) {
      SmartlingOptions options = SmartlingOptions.parseList(optionList);
      if (options.isJsonSync()) {
        thirdPartyTMSSmartlingWithJson.pushAiTranslations(
            repository,
            projectId,
            localeMapping,
            skipTextUnitsWithPattern,
            skipAssetsWithPathPattern,
            includeTextUnitsWithPattern);
      }

      AndroidStringDocumentMapper mapper = new AndroidStringDocumentMapper(pluralSeparator, null);

      Set<Long> filterTmTextUnitIds = getFilterTmTextUnitIdsForPushTranslation(options);

      List<SmartlingFile> result =
          repository.getRepositoryLocales().stream()
              .map(l -> l.getLocale().getBcp47Tag())
              .filter(
                  localeTag ->
                      !localeTag.equalsIgnoreCase(repository.getSourceLocale().getBcp47Tag()))
              .flatMap(
                  localeTag -> {
                    Map<String, List<TextUnitDTO>> singularsByUploadedFileUri =
                        partitionSingulars(
                                repository.getId(),
                                localeTag,
                                skipTextUnitsWithPattern,
                                skipAssetsWithPathPattern,
                                includeTextUnitsWithPattern,
                                StatusFilter.MT_TRANSLATED,
                                true)
                            .flatMap(List::stream)
                            .collect(Collectors.groupingBy(TextUnitDTO::getUploadedFileUri));

                    Map<String, List<TextUnitDTO>> pluralsByUploadedFileUri =
                        partitionPlurals(
                                repository.getId(),
                                localeTag,
                                skipTextUnitsWithPattern,
                                skipAssetsWithPathPattern,
                                options.getPluralFixForLocales(),
                                includeTextUnitsWithPattern,
                                StatusFilter.MT_TRANSLATED,
                                true)
                            .flatMap(List::stream)
                            .collect(Collectors.groupingBy(TextUnitDTO::getUploadedFileUri));

                    Stream<SmartlingFile> singularFiles =
                        singularsByUploadedFileUri.entrySet().stream()
                            .map(
                                entry ->
                                    uploadAiTranslationBatch(
                                        entry.getValue(),
                                        entry.getKey(),
                                        localeTag,
                                        mapper,
                                        repository,
                                        projectId,
                                        options,
                                        localeMapping,
                                        filterTmTextUnitIds));

                    Stream<SmartlingFile> pluralFiles =
                        pluralsByUploadedFileUri.entrySet().stream()
                            .map(
                                entry ->
                                    uploadAiTranslationBatch(
                                        entry.getValue(),
                                        entry.getKey(),
                                        localeTag,
                                        mapper,
                                        repository,
                                        projectId,
                                        options,
                                        localeMapping,
                                        filterTmTextUnitIds));

                    return Stream.concat(singularFiles, pluralFiles);
                  })
              .collect(Collectors.toList());

      resultProcessor.processPushAiTranslations(result, options);
    }
  }

  private SmartlingFile uploadAiTranslationBatch(
      List<TextUnitDTO> batch,
      String uploadedFileUri,
      String localeTag,
      AndroidStringDocumentMapper mapper,
      Repository repository,
      String projectId,
      SmartlingOptions options,
      Map<String, String> localeMapping,
      Set<Long> filterTmTextUnitIds) {

    SmartlingFile file =
        uploadAiTranslationFile(
            batch,
            localeTag,
            uploadedFileUri,
            mapper,
            repository,
            projectId,
            options,
            localeMapping,
            filterTmTextUnitIds);
    aiTranslationService.updateVariantStatusToMTReviewNeeded(
        batch.stream().map(TextUnitDTO::getTmTextUnitCurrentVariantId).toList());
    meterRegistry
        .counter(
            "SmartlingSync.uploadAiTranslationsBatch",
            "repository",
            repository.getName(),
            "jsonSync",
            "false")
        .increment(batch.size());
    return file;
  }

  private SmartlingFile processTranslationBatch(
      List<TextUnitDTO> batch,
      Long batchNumber,
      String localeTag,
      AndroidStringDocumentMapper mapper,
      Repository repository,
      String projectId,
      SmartlingOptions options,
      Map<String, String> localeMapping,
      Prefix filePrefix,
      Set<Long> filterTmTextUnitIds,
      String translationState) {

    try (var timer =
        Timer.resource(meterRegistry, "SmartlingSync.processTranslationBatch")
            .tag("repository", repository.getName())
            .tag("locale", localeTag)) {

      logger.debug("Process {} batch: {}", localeTag, batchNumber);
      logger.debug("Convert text units to AndroidString for asset number: {}", batchNumber);
      List<TextUnitDTO> fileBatch = batch;
      String sourceFilename =
          getOutputSourceFile(batchNumber, repository.getName(), filePrefix.getType());
      String targetFilename =
          getOutputTargetFile(batchNumber, repository.getName(), filePrefix.getType(), localeTag);
      SmartlingFile file = new SmartlingFile();
      file.setFileName(targetFilename);

      try {

        logger.debug("Save target file to: {}", file.getFileName());

        if (filterTmTextUnitIds != null) {
          fileBatch =
              fileBatch.stream()
                  .filter(
                      textUnitDTO -> filterTmTextUnitIds.contains(textUnitDTO.getTmTextUnitId()))
                  .collect(Collectors.toList());
        }

        AndroidStringDocumentWriter writer =
            new AndroidStringDocumentWriter(mapper.readFromTargetTextUnits(batch));
        file.setFileContent(writer.toText());

      } catch (ParserConfigurationException | TransformerException e) {
        logger.error("An error ocurred when processing a push_translations batch", e);
        throw new RuntimeException(e);
      }

      if (!options.isDryRun()) {
        logger.debug(
            "Push Android file to Smartling project: {} and locale: {}", projectId, localeTag);
        Mono.fromCallable(
                () ->
                    smartlingClient.uploadLocalizedFile(
                        projectId,
                        sourceFilename,
                        "android",
                        getSmartlingLocale(localeMapping, localeTag),
                        file.getFileContent(),
                        options.getPlaceholderFormat(),
                        options.getCustomPlaceholderFormat(),
                        translationState))
            .retryWhen(
                smartlingClient
                    .getRetryConfiguration()
                    .doBeforeRetry(
                        e ->
                            logger.info(
                                String.format(
                                    "Retrying after failure to upload localized file: %s, project id: %s",
                                    sourceFilename, projectId),
                                e.failure())))
            .doOnError(
                e -> {
                  String msg =
                      String.format(
                          "Error uploading localized file to Smartling for file %s in project %s",
                          sourceFilename, projectId);
                  logger.error(msg, e);
                  throw new SmartlingClientException(msg, e);
                })
            .block();
      }

      return file;
    }
  }

  private SmartlingFile uploadAiTranslationFile(
      List<TextUnitDTO> batch,
      String localeTag,
      String fileName,
      AndroidStringDocumentMapper mapper,
      Repository repository,
      String projectId,
      SmartlingOptions options,
      Map<String, String> localeMapping,
      Set<Long> filterTmTextUnitIds) {

    try (var timer =
        Timer.resource(meterRegistry, "SmartlingSync.processTranslationBatch")
            .tag("repository", repository.getName())
            .tag("locale", localeTag)) {

      logger.debug("Process translation batch for file: {}", fileName);
      List<TextUnitDTO> fileBatch = batch;
      SmartlingFile file = new SmartlingFile();
      file.setFileName(fileName + "_" + localeTag);

      try {
        logger.debug("Save target file to: {}", file.getFileName());

        if (filterTmTextUnitIds != null) {
          fileBatch =
              fileBatch.stream()
                  .filter(
                      textUnitDTO -> filterTmTextUnitIds.contains(textUnitDTO.getTmTextUnitId()))
                  .collect(Collectors.toList());
        }

        AndroidStringDocumentWriter writer =
            new AndroidStringDocumentWriter(mapper.readFromTargetTextUnits(fileBatch));
        file.setFileContent(writer.toText());

      } catch (ParserConfigurationException | TransformerException e) {
        logger.error("An error occurred when processing a translation batch", e);
        throw new RuntimeException(e);
      }

      if (!options.isDryRun()) {
        logger.debug(
            "Pushing Android file to Smartling project: {} and locale: {}", projectId, localeTag);
        Mono.fromCallable(
                () ->
                    smartlingClient.uploadLocalizedFile(
                        projectId,
                        fileName,
                        "android",
                        getSmartlingLocale(localeMapping, localeTag),
                        file.getFileContent(),
                        options.getPlaceholderFormat(),
                        options.getCustomPlaceholderFormat(),
                        POST_TRANSLATION))
            .retryWhen(
                smartlingClient
                    .getRetryConfiguration()
                    .doBeforeRetry(
                        e ->
                            logger.info(
                                String.format(
                                    "Retrying after failure to upload localized file: %s, project id: %s",
                                    fileName, projectId),
                                e.failure())))
            .doOnError(
                e -> {
                  String msg =
                      String.format(
                          "Error uploading localized file to Smartling for file %s in project %s",
                          fileName, projectId);
                  logger.error(msg, e);
                  throw new SmartlingClientException(msg, e);
                })
            .block();
      }

      return file;
    }
  }

  private Stream<List<TextUnitDTO>> partitionSingulars(
      Long repositoryId,
      String localeTag,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern) {
    TextUnitSearcherParameters parameters =
        this.baseParams()
            .repositoryId(repositoryId)
            .localeTags(ImmutableList.of(localeTag))
            .skipTextUnitWithPattern(skipTextUnitsWithPattern)
            .skipAssetPathWithPattern(skipAssetsWithPathPattern)
            .pluralFormsFiltered(true)
            .pluralFormsExcluded(true)
            .isOrderedByTextUnitID(true)
            .build();
    return partitionedStream(parameters, textUnitSearcher::search);
  }

  private Stream<List<TextUnitDTO>> partitionSingulars(
      Long repositoryId,
      String localeTag,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern,
      String includeTextUnitWithPattern) {
    TextUnitSearcherParameters parameters =
        this.baseParams()
            .repositoryId(repositoryId)
            .localeTags(ImmutableList.of(localeTag))
            .skipTextUnitWithPattern(skipTextUnitsWithPattern)
            .skipAssetPathWithPattern(skipAssetsWithPathPattern)
            .pluralFormsFiltered(true)
            .pluralFormsExcluded(true)
            .includeTextUnitsWithPattern(includeTextUnitWithPattern)
            .isOrderedByTextUnitID(true)
            .build();
    return partitionedStream(parameters, textUnitSearcher::search);
  }

  private Stream<List<TextUnitDTO>> partitionSingulars(
      Long repositoryId,
      String localeTag,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern,
      String includeTextUnitWithPattern,
      StatusFilter statusFilter,
      boolean isRetrieveFileUploadUri) {
    TextUnitSearcherParameters parameters =
        this.baseParams()
            .repositoryId(repositoryId)
            .localeTags(ImmutableList.of(localeTag))
            .skipTextUnitWithPattern(skipTextUnitsWithPattern)
            .skipAssetPathWithPattern(skipAssetsWithPathPattern)
            .pluralFormsFiltered(true)
            .pluralFormsExcluded(true)
            .includeTextUnitsWithPattern(includeTextUnitWithPattern)
            .statusFilter(statusFilter)
            .isOrderedByTextUnitID(true)
            .shouldRetrieveUploadedFileUri(isRetrieveFileUploadUri)
            .build();

    return partitionedStream(parameters, textUnitSearcher::search);
  }

  private Stream<List<TextUnitDTO>> partitionPlurals(
      Long repositoryId,
      String localeTag,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern) {
    TextUnitSearcherParameters parameters =
        this.baseParams()
            .repositoryId(repositoryId)
            .localeTags(ImmutableList.of(localeTag))
            .skipTextUnitWithPattern(skipTextUnitsWithPattern)
            .skipAssetPathWithPattern(skipAssetsWithPathPattern)
            .pluralFormsFiltered(false)
            .pluralFormsExcluded(false)
            .pluralFormOther("%")
            .isOrderedByTextUnitID(true)
            .build();
    return partitionedStream(parameters, textUnitSearcher::search);
  }

  private Stream<List<TextUnitDTO>> partitionPlurals(
      Long repositoryId,
      String localeTag,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern,
      Set<String> pluralFixForLocales,
      String includeTextUnitsWithPattern) {
    return partitionPlurals(
        repositoryId,
        localeTag,
        skipTextUnitsWithPattern,
        skipAssetsWithPathPattern,
        pluralFixForLocales,
        includeTextUnitsWithPattern,
        null,
        false);
  }

  private Stream<List<TextUnitDTO>> partitionPlurals(
      Long repositoryId,
      String localeTag,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern,
      Set<String> pluralFixForLocales,
      String includeTextUnitsWithPattern,
      StatusFilter statusFilter,
      boolean isRetrieveFileUploadUri) {
    Function<TextUnitSearcherParameters, List<TextUnitDTO>> searchFunction =
        textUnitSearcher::search;

    if (pluralFixForLocales != null && pluralFixForLocales.contains(localeTag)) {
      searchFunction =
          searchFunction.andThen(
              textUnits ->
                  textUnits.stream()
                      .filter(tu -> !MANY.toString().equalsIgnoreCase(tu.getPluralForm()))
                      .collect(Collectors.toList()));
    }

    TextUnitSearcherParameters parameters =
        this.baseParams()
            .repositoryId(repositoryId)
            .localeTags(ImmutableList.of(localeTag))
            .skipTextUnitWithPattern(skipTextUnitsWithPattern)
            .skipAssetPathWithPattern(skipAssetsWithPathPattern)
            .pluralFormsFiltered(false)
            .pluralFormsExcluded(false)
            .pluralFormOther("%")
            .includeTextUnitsWithPattern(includeTextUnitsWithPattern)
            .isOrderedByTextUnitID(true)
            .shouldRetrieveUploadedFileUri(isRetrieveFileUploadUri)
            .build();

    if (statusFilter != null) {
      parameters.setStatusFilter(statusFilter);
    }

    return partitionedStream(parameters, searchFunction);
  }

  private Stream<List<TextUnitDTO>> partitionedStream(
      TextUnitSearcherParameters params,
      Function<TextUnitSearcherParameters, List<TextUnitDTO>> function) {
    return StreamSupport.stream(
        Iterables.partition(function.apply(params), batchSize).spliterator(), false);
  }

  private Long singularCount(
      Long repositoryId,
      String localeTag,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern) {
    return textUnitSearcher
        .countTextUnitAndWordCount(
            this.baseParams()
                .repositoryId(repositoryId)
                .localeTags(ImmutableList.of(localeTag))
                .skipTextUnitWithPattern(skipTextUnitsWithPattern)
                .skipAssetPathWithPattern(skipAssetsWithPathPattern)
                .pluralFormsFiltered(true)
                .pluralFormsExcluded(true)
                .build())
        .getTextUnitCount();
  }

  private Long pluralCount(
      Long repositoryId,
      String localeTag,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern) {
    return textUnitSearcher
        .countTextUnitAndWordCount(
            this.baseParams()
                .repositoryId(repositoryId)
                .localeTags(ImmutableList.of(localeTag))
                .skipTextUnitWithPattern(skipTextUnitsWithPattern)
                .skipAssetPathWithPattern(skipAssetsWithPathPattern)
                .pluralFormsFiltered(false)
                .pluralFormsExcluded(false)
                .pluralFormOther("%")
                .build())
        .getTextUnitCount();
  }

  private TextUnitSearcherParameters.Builder baseParams() {
    return new TextUnitSearcherParameters.Builder()
        .rootLocaleExcluded(false)
        .doNotTranslateFilter(false)
        .searchType(SearchType.ILIKE)
        .statusFilter(StatusFilter.TRANSLATED)
        .usedFilter(UsedFilter.USED)
        .isExcludeUnexpiredPendingMT(aiTranslationConfiguration.isEnabled())
        .aiTranslationExpiryDuration(aiTranslationConfiguration.getExpiryDuration());
  }

  /**
   * Calculates the number of batches required to process totalUnits, considering the batchSize
   * configured at the instance level. E.g: If our batch size is 10, and we have 123 units, this
   * function returns 13, as we need 13 batches of 10 to process 123 units.
   *
   * @param totalUnits Total units to process
   * @return The amount of batches required to process totalUnits
   */
  long batchesFor(long totalUnits) {
    return totalUnits / batchSize + ((totalUnits % batchSize == 0) ? 0 : 1);
  }

  private enum Prefix {
    SINGULAR,
    PLURAL;

    public String getType() {
      return name().toLowerCase();
    }

    public boolean isPlural() {
      return this.equals(PLURAL);
    }
  }

  private boolean isImageExtensionSupported(String name) {
    String extension = Files.getFileExtension(name).toLowerCase();
    return supportedImageExtensions.contains(extension);
  }
}
