package com.box.l10n.mojito.service.thirdparty;

import static com.box.l10n.mojito.android.strings.AndroidPluralQuantity.MANY;
import static com.box.l10n.mojito.service.thirdparty.smartling.SmartlingFileUtils.*;
import static com.google.common.collect.Streams.mapWithIndex;

import com.box.l10n.mojito.android.strings.AndroidStringDocumentMapper;
import com.box.l10n.mojito.android.strings.AndroidStringDocumentReader;
import com.box.l10n.mojito.android.strings.AndroidStringDocumentWriter;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.service.assetExtraction.AssetTextUnitToTMTextUnitRepository;
import com.box.l10n.mojito.service.thirdparty.smartling.*;
import com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService;
import com.box.l10n.mojito.service.tm.search.*;
import com.box.l10n.mojito.smartling.AssetPathAndTextUnitNameKeys;
import com.box.l10n.mojito.smartling.SmartlingClient;
import com.box.l10n.mojito.smartling.SmartlingClientException;
import com.box.l10n.mojito.smartling.request.Binding;
import com.box.l10n.mojito.smartling.request.Bindings;
import com.box.l10n.mojito.smartling.response.File;
import com.box.l10n.mojito.smartling.response.StringInfo;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import io.micrometer.core.annotation.Timed;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;
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
  private final TextUnitSearcher textUnitSearcher;
  private final TextUnitBatchImporterService textUnitBatchImporterService;
  private final SmartlingResultProcessor resultProcessor;
  private final Integer batchSize;
  private final ThirdPartyTMSSmartlingWithJson thirdPartyTMSSmartlingWithJson;
  private final ThirdPartyTMSSmartlingGlossary thirdPartyTMSSmartlingGlossary;
  private final AssetTextUnitToTMTextUnitRepository assetTextUnitToTMTextUnitRepository;

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
      AssetTextUnitToTMTextUnitRepository assetTextUnitToTMTextUnitRepository) {
    this(
        smartlingClient,
        textUnitSearcher,
        assetPathAndTextUnitNameKeys,
        textUnitBatchImporterService,
        resultProcessor,
        thirdPartyTMSSmartlingWithJson,
        thirdPartyTMSSmartlingGlossary,
        assetTextUnitToTMTextUnitRepository,
        DEFAULT_BATCH_SIZE);
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
      int batchSize) {
    this.smartlingClient = smartlingClient;
    this.assetPathAndTextUnitNameKeys = assetPathAndTextUnitNameKeys;
    this.textUnitBatchImporterService = textUnitBatchImporterService;
    this.textUnitSearcher = textUnitSearcher;
    this.resultProcessor = resultProcessor;
    this.batchSize = batchSize < 1 ? DEFAULT_BATCH_SIZE : batchSize;
    this.thirdPartyTMSSmartlingWithJson = thirdPartyTMSSmartlingWithJson;
    this.thirdPartyTMSSmartlingGlossary = thirdPartyTMSSmartlingGlossary;
    this.assetTextUnitToTMTextUnitRepository = assetTextUnitToTMTextUnitRepository;
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
  @Timed("SmartlingSync.push")
  public void push(
      Repository repository,
      String projectId,
      String pluralSeparator,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern,
      List<String> optionList) {

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
                repository.getId(), LOCALE_EN, skipTextUnitsWithPattern, skipAssetsWithPathPattern),
            (batch, index) ->
                processPushBatch(
                    batch, index, mapper, repository, projectId, options, Prefix.SINGULAR));

    Stream<SmartlingFile> pluralFiles =
        mapWithIndex(
            partitionPlurals(
                repository.getId(), LOCALE_EN, skipTextUnitsWithPattern, skipAssetsWithPathPattern),
            (batch, index) ->
                processPushBatch(
                    batch, index, mapper, repository, projectId, options, Prefix.PLURAL));

    List<SmartlingFile> result =
        Stream.concat(singularFiles, pluralFiles).collect(Collectors.toList());
    resultProcessor.processPush(result, options);
  }

  private SmartlingFile processPushBatch(
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
  @Timed("SmartlingSync.pull")
  public void pull(
      Repository repository,
      String projectId,
      String pluralSeparator,
      Map<String, String> localeMapping,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern,
      List<String> optionList) {

    SmartlingOptions options = SmartlingOptions.parseList(optionList);

    if (options.isJsonSync()) {
      thirdPartyTMSSmartlingWithJson.pull(repository, projectId, localeMapping);
      return;
    }

    if (options.isGlossarySync()) {
      thirdPartyTMSSmartlingGlossary.pull(repository, projectId, localeMapping);
      return;
    }

    Long singulars =
        singularCount(
            repository.getId(), LOCALE_EN, skipTextUnitsWithPattern, skipAssetsWithPathPattern);
    LongStream.range(0, batchesFor(singulars))
        .forEach(
            num ->
                processPullBatch(
                    num,
                    pluralSeparator,
                    repository,
                    projectId,
                    options,
                    localeMapping,
                    Prefix.SINGULAR));

    Long plurals =
        pluralCount(
            repository.getId(), LOCALE_EN, skipTextUnitsWithPattern, skipAssetsWithPathPattern);
    LongStream.range(0, batchesFor(plurals))
        .forEach(
            num ->
                processPullBatch(
                    num,
                    pluralSeparator,
                    repository,
                    projectId,
                    options,
                    localeMapping,
                    Prefix.PLURAL));
  }

  private void processPullBatch(
      Long batchNumber,
      String pluralSeparator,
      Repository repository,
      String projectId,
      SmartlingOptions options,
      Map<String, String> localeMapping,
      Prefix filePrefix) {

    AndroidStringDocumentMapper mapper;

    for (RepositoryLocale locale : repository.getRepositoryLocales()) {

      if (locale.getParentLocale() == null) {
        continue;
      }

      String localeTag = locale.getLocale().getBcp47Tag();
      String smartlingLocale = getSmartlingLocale(localeMapping, localeTag);
      String fileName =
          getOutputSourceFile(batchNumber, repository.getName(), filePrefix.getType());
      mapper =
          new AndroidStringDocumentMapper(pluralSeparator, null, localeTag, repository.getName());

      logger.debug(
          "Download localized file from Smartling for file: {}, Mojito locale: {} and Smartling locale: {}",
          fileName,
          localeTag,
          smartlingLocale);

      String fileContent =
          Mono.fromCallable(
                  () ->
                      smartlingClient.downloadPublishedFile(
                          projectId, smartlingLocale, fileName, false))
              .retryWhen(
                  smartlingClient
                      .getRetryConfiguration()
                      .doBeforeRetry(
                          e ->
                              logger.info(
                                  String.format(
                                      "Retrying after failure to download file %s", fileName),
                                  e.failure())))
              .doOnError(
                  e -> {
                    String msg =
                        String.format("Error downloading file %s: %s", fileName, e.getMessage());
                    logger.error(msg, e);
                    throw new SmartlingClientException(msg, e);
                  })
              .blockOptional()
              .orElseThrow(
                  () ->
                      new SmartlingClientException(
                          "Error with download from Smartling, file content string is not present."));

      List<TextUnitDTO> textUnits;

      try {
        textUnits = mapper.mapToTextUnits(AndroidStringDocumentReader.fromText(fileContent));
      } catch (ParserConfigurationException | IOException | SAXException e) {
        String msg = "An error ocurred when processing a pull batch";
        logger.error(msg, e);
        throw new RuntimeException(msg, e);
      }

      if (!textUnits.isEmpty()
          && filePrefix.isPlural()
          && options.getPluralFixForLocales().contains(localeTag)) {
        textUnits = SmartlingPluralFix.fixTextUnits(textUnits);
      }

      if (!options.isDryRun()) {
        logger.debug("Importing text units for locale: {}", smartlingLocale);
        textUnitBatchImporterService.importTextUnits(textUnits, false, true);
      }
    }
  }

  @Override
  @Timed("SmartlingSync.pushTranslations")
  public void pushTranslations(
      Repository repository,
      String projectId,
      String pluralSeparator,
      Map<String, String> localeMapping,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern,
      String includeTextUnitsWithPattern,
      List<String> optionList) {

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
                                    filterTmTextUnitIds)),
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
                                    filterTmTextUnitIds))))
            .collect(Collectors.toList());

    resultProcessor.processPushTranslations(result, options);
  }

  private Set<Long> getFilterTmTextUnitIdsForPushTranslation(SmartlingOptions options) {
    Set<Long> filterTmTextUnitIds = null;
    if (options.getPushTranslationBranchName() != null) {
      filterTmTextUnitIds =
          assetTextUnitToTMTextUnitRepository
              .findByBranchName(options.getPushTranslationBranchName()).stream()
              .collect(Collectors.toSet());
    }
    return filterTmTextUnitIds;
  }

  @Override
  @Timed("SmartlingSync.pullSource")
  public void pullSource(
      Repository repository,
      String thirdPartyProjectId,
      List<String> optionList,
      Map<String, String> localeMapping) {
    SmartlingOptions options = SmartlingOptions.parseList(optionList);
    if (options.isGlossarySync()) {
      thirdPartyTMSSmartlingGlossary.pullSourceTextUnits(
          repository, thirdPartyProjectId, localeMapping);
    } else {
      throw new UnsupportedOperationException(
          "Pull source is only supported with glossary sync enabled.");
    }
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
      Set<Long> filterTmTextUnitIds) {

    logger.debug("Process {} batch: {}", localeTag, batchNumber);
    logger.debug("Convert text units to AndroidString for asset number: {}", batchNumber);
    String sourceFilename =
        getOutputSourceFile(batchNumber, repository.getName(), filePrefix.getType());
    String targetFilename =
        getOutputTargetFile(batchNumber, repository.getName(), filePrefix.getType(), localeTag);
    SmartlingFile file = new SmartlingFile();
    file.setFileName(targetFilename);

    try {

      logger.debug("Save target file to: {}", file.getFileName());

      if (filterTmTextUnitIds != null) {
        batch =
            batch.stream()
                .filter(textUnitDTO -> filterTmTextUnitIds.contains(textUnitDTO.getTmTextUnitId()))
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
                      options.getCustomPlaceholderFormat()))
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

  private Stream<List<TextUnitDTO>> partitionSingulars(
      Long repositoryId,
      String localeTag,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern) {
    return partitionedStream(
        baseParams(
            repositoryId,
            localeTag,
            skipTextUnitsWithPattern,
            skipAssetsWithPathPattern,
            true,
            true,
            null),
        textUnitSearcher::search);
  }

  private Stream<List<TextUnitDTO>> partitionSingulars(
      Long repositoryId,
      String localeTag,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern,
      String includeTextUnitWithPattern) {
    return partitionedStream(
        baseParams(
            repositoryId,
            localeTag,
            skipTextUnitsWithPattern,
            skipAssetsWithPathPattern,
            true,
            true,
            null,
            includeTextUnitWithPattern),
        textUnitSearcher::search);
  }

  private Stream<List<TextUnitDTO>> partitionPlurals(
      Long repositoryId,
      String localeTag,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern) {
    return partitionedStream(
        baseParams(
            repositoryId,
            localeTag,
            skipTextUnitsWithPattern,
            skipAssetsWithPathPattern,
            false,
            false,
            "%"),
        textUnitSearcher::search);
  }

  private Stream<List<TextUnitDTO>> partitionPlurals(
      Long repositoryId,
      String localeTag,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern,
      Set<String> pluralFixForLocales,
      String includeTextUnitsWithPattern) {

    Function<TextUnitSearcherParameters, List<TextUnitDTO>> searchFunction =
        textUnitSearcher::search;

    if (pluralFixForLocales.contains(localeTag)) {
      searchFunction =
          searchFunction.andThen(
              textUnits ->
                  textUnits.stream()
                      .filter(tu -> !MANY.toString().equalsIgnoreCase(tu.getPluralForm()))
                      .collect(Collectors.toList()));
    }

    return partitionedStream(
        baseParams(
            repositoryId,
            localeTag,
            skipTextUnitsWithPattern,
            skipAssetsWithPathPattern,
            false,
            false,
            "%",
            includeTextUnitsWithPattern),
        searchFunction);
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
            baseParams(
                repositoryId,
                localeTag,
                skipTextUnitsWithPattern,
                skipAssetsWithPathPattern,
                true,
                true,
                null))
        .getTextUnitCount();
  }

  private Long pluralCount(
      Long repositoryId,
      String localeTag,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern) {
    return textUnitSearcher
        .countTextUnitAndWordCount(
            baseParams(
                repositoryId,
                localeTag,
                skipTextUnitsWithPattern,
                skipAssetsWithPathPattern,
                false,
                false,
                "%"))
        .getTextUnitCount();
  }

  private TextUnitSearcherParameters baseParams(
      Long repositoryId,
      String localeTag,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern,
      boolean pluralFormsFiltered,
      boolean pluralFormsExcluded,
      String pluralFormOther) {
    TextUnitSearcherParameters result = new TextUnitSearcherParameters();
    result.setRepositoryIds(repositoryId);
    result.setLocaleTags(ImmutableList.of(localeTag));
    result.setRootLocaleExcluded(false);
    result.setDoNotTranslateFilter(false);
    result.setSearchType(SearchType.ILIKE);
    result.setStatusFilter(StatusFilter.TRANSLATED);
    result.setUsedFilter(UsedFilter.USED);
    result.setPluralFormsFiltered(pluralFormsFiltered);
    result.setPluralFormsExcluded(pluralFormsExcluded);
    result.setSkipTextUnitWithPattern(skipTextUnitsWithPattern);
    result.setSkipAssetPathWithPattern(skipAssetsWithPathPattern);

    if (!Strings.isNullOrEmpty(pluralFormOther)) {
      result.setPluralFormOther(pluralFormOther);
    }

    return result;
  }

  private TextUnitSearcherParameters baseParams(
      Long repositoryId,
      String localeTag,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern,
      boolean pluralFormsFiltered,
      boolean pluralFormsExcluded,
      String pluralFormOther,
      String includeTextUnitsWithPattern) {
    TextUnitSearcherParameters result =
        baseParams(
            repositoryId,
            localeTag,
            skipTextUnitsWithPattern,
            skipAssetsWithPathPattern,
            pluralFormsFiltered,
            pluralFormsExcluded,
            pluralFormOther);
    result.setIncludeTextUnitsWithPattern(includeTextUnitsWithPattern);
    return result;
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
