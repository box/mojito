package com.box.l10n.mojito.service.thirdparty;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.android.strings.AndroidStringDocument;
import com.box.l10n.mojito.android.strings.AndroidStringDocumentMapper;
import com.box.l10n.mojito.android.strings.AndroidStringDocumentReader;
import com.box.l10n.mojito.android.strings.AndroidStringDocumentWriter;
import com.box.l10n.mojito.android.strings.AndroidStringDocumentWriter.EscapeType;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.thirdparty.phrase.PhraseClient;
import com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService;
import com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService.IntegrityChecksType;
import com.box.l10n.mojito.service.tm.search.SearchType;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import com.box.l10n.mojito.utils.OptionsParser;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.phrase.client.model.Tag;
import com.phrase.client.model.TranslationKey;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(value = "l10n.ThirdPartyTMS.impl", havingValue = "ThirdPartyTMSPhrase")
@Component
public class ThirdPartyTMSPhrase implements ThirdPartyTMS {

  static final String TAG_PREFIX = "push_";
  static final String TAG_PREFIX_WITH_REPOSITORY = "push_%s";
  static final String TAG_DATE_FORMAT = "yyyy_MM_dd_HH_mm_ss_SSS";
  static final boolean NATIVE_CLIENT_DEFAULT_VALUE = false;

  static Logger logger = LoggerFactory.getLogger(ThirdPartyTMSPhrase.class);

  TextUnitSearcher textUnitSearcher = new TextUnitSearcher();

  TextUnitBatchImporterService textUnitBatchImporterService;

  PhraseClient phraseClient;

  RepositoryService repositoryService;

  MeterRegistry meterRegistry;

  public ThirdPartyTMSPhrase(
      TextUnitSearcher textUnitSearcher,
      TextUnitBatchImporterService textUnitBatchImporterService,
      PhraseClient phraseClient,
      RepositoryService repositoryService,
      MeterRegistry meterRegistry) {

    this.textUnitSearcher = textUnitSearcher;
    this.textUnitBatchImporterService = textUnitBatchImporterService;
    this.phraseClient = phraseClient;
    this.repositoryService = repositoryService;
    this.meterRegistry = meterRegistry;
  }

  @Override
  public void removeImage(String projectId, String imageId) {
    throw new UnsupportedOperationException("Remove image is not supported");
  }

  @Override
  public ThirdPartyTMSImage uploadImage(String projectId, String name, byte[] content) {
    throw new UnsupportedOperationException("Upload image is not supported");
  }

  @Override
  public List<ThirdPartyTextUnit> getThirdPartyTextUnits(
      Repository repository, String projectId, List<String> optionList) {

    List<ThirdPartyTextUnit> thirdPartyTextUnits = new ArrayList<>();

    String currentTagsForRepository = getCurrentTagsForRepository(repository, projectId);

    List<TranslationKey> phraseTranslationKeys =
        phraseClient.getKeys(projectId, currentTagsForRepository);

    for (TranslationKey translationKey : phraseTranslationKeys) {

      String[] nameParts = translationKey.getName().split("#@#", 3);

      if (nameParts.length != 3) {
        logger.error(
            "Skipping entry. Name: {} should have 3 parts. Missing part could happen in old project",
            translationKey.getName());
        continue;
      }

      String idSection = nameParts[0];
      if (!idSection.contains(",")) {
        ThirdPartyTextUnit thirdPartyTextUnit = new ThirdPartyTextUnit();
        thirdPartyTextUnit.setAssetPath(nameParts[1]);
        thirdPartyTextUnit.setName(nameParts[2]);
        thirdPartyTextUnit.setId(translationKey.getId());
        thirdPartyTextUnit.setTmTextUnitId(Long.valueOf(idSection));
        thirdPartyTextUnits.add(thirdPartyTextUnit);
      } else {
        List<Long> ids = Arrays.stream(idSection.split(",")).map(Long::valueOf).toList();
        for (Long id : ids) {
          ThirdPartyTextUnit thirdPartyTextUnit = new ThirdPartyTextUnit();
          thirdPartyTextUnit.setAssetPath(nameParts[1]);
          thirdPartyTextUnit.setName(nameParts[2]);
          thirdPartyTextUnit.setId(translationKey.getId());
          thirdPartyTextUnit.setTmTextUnitId(id);
          thirdPartyTextUnits.add(thirdPartyTextUnit);
        }
      }
    }

    return thirdPartyTextUnits;
  }

  @Override
  public void createImageToTextUnitMappings(
      String projectId, List<ThirdPartyImageToTextUnit> thirdPartyImageToTextUnits) {
    throw new UnsupportedOperationException("Create image to text units is not supported");
  }

  @Override
  public void push(
      Repository repository,
      String projectId,
      String pluralSeparator,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern,
      List<String> options) {

    OptionsParser optionsParser = new OptionsParser(options);
    Boolean nativeClient = optionsParser.getBoolean("nativeClient", NATIVE_CLIENT_DEFAULT_VALUE);
    Map<String, String> formatOptions = getFormatOptions(optionsParser);
    final AtomicReference<EscapeType> escapeType =
        getEscapeTypeAtomicReference(nativeClient, optionsParser);

    Stopwatch stopwatchGetTextUnitDTO = Stopwatch.createStarted();
    List<TextUnitDTO> search =
        getSourceTextUnitDTOs(repository, skipTextUnitsWithPattern, skipAssetsWithPathPattern);
    logger.info("Get Source TextUnitDTO for push took: {}", stopwatchGetTextUnitDTO.elapsed());
    String text = getFileContent(pluralSeparator, search, true, null, escapeType.get());

    String tagForUpload = getTagForUpload(repository.getName());

    if (nativeClient) {
      logger.debug("Pushing with native and options: {}", formatOptions);
      Stopwatch stopWatchPhaseNativePush = Stopwatch.createStarted();
      phraseClient.nativeUploadAndWait(
          projectId,
          repository.getSourceLocale().getBcp47Tag(),
          "xml",
          repository.getName() + "-strings.xml",
          text,
          ImmutableList.of(tagForUpload),
          formatOptions.isEmpty() ? null : formatOptions);
      logger.info(
          "Pushing with native and options: {}, took: {}",
          formatOptions,
          stopWatchPhaseNativePush.elapsed());
    } else {
      phraseClient.uploadAndWait(
          projectId,
          repository.getSourceLocale().getBcp47Tag(),
          "xml",
          repository.getName() + "-strings.xml",
          text,
          ImmutableList.of(tagForUpload),
          null);
    }

    boolean skipRemoveUnusedKeysAndTags =
        optionsParser.getBoolean("skipRemoveUnusedKeysAndTags", false);

    if (!skipRemoveUnusedKeysAndTags) {
      logger.info("Skipping removing unused keys and tags");
      removeUnusedKeysAndTags(projectId, repository.getName(), tagForUpload);
    }
  }

  /**
   * Remove unused keys and tags
   *
   * <ul>
   *   <li><b>Remove Unused Keys:</b>
   *       <ul>
   *         <li>Unused keys in a Phrase project are defined as keys that are not tagged with the
   *             latest push tag from any Mojito repository.
   *         <li>To identify unused keys, fetch the current push tags of all repositories, excluding
   *             the push tag related to the Mojito repository being processed, and include the
   *             current upload tag.
   *       </ul>
   *   <li><b>Manage Tags:</b>
   *       <ul>
   *         <li>Ensure that there is only one active "push" tag per repository.
   *         <li>Remove old tags that are prefixed with "push" but not active
   *       </ul>
   * </ul>
   *
   * <p><b>Explanation:</b>
   *
   * <p>The N:1 relationship between Mojito and Phrase is maintained through this logic, allowing
   * for efficient management of keys and tags.
   */
  public void removeUnusedKeysAndTags(
      String projectId, String repositoryName, String tagForUpload) {

    Stopwatch stopwatchRemoveUnusedKeysAndTags = Stopwatch.createStarted();

    List<String> tagsForOtherRepositories =
        phraseClient.listTags(projectId).stream()
            .map(Tag::getName)
            .filter(Objects::nonNull)
            .filter(tagName -> tagName.startsWith(TAG_PREFIX))
            .filter(tagName -> !tagName.startsWith(getTagNamePrefixForRepository(repositoryName)))
            .toList();

    List<String> allActiveTags = new ArrayList<>(tagsForOtherRepositories);
    allActiveTags.add(tagForUpload);

    logger.debug("All active tags: {}", allActiveTags);
    phraseClient.removeKeysNotTaggedWith(projectId, allActiveTags);

    List<String> pushTagsToDelete =
        phraseClient.listTags(projectId).stream()
            .map(Tag::getName)
            .filter(Objects::nonNull)
            .filter(tagName -> tagName.startsWith(TAG_PREFIX))
            .filter(tagName -> !allActiveTags.contains(tagName))
            // That's to handle concurrent sync and make sure a tag that was just pushed is not
            // deleted before the  pull from the same sync is finished.
            // We don't prevent syncs to run concurrently
            .filter(tagName -> !areTagsWithin5Minutes(tagForUpload, tagName))
            .toList();

    logger.info("Tags to delete: {}", pushTagsToDelete);
    phraseClient.deleteTags(projectId, pushTagsToDelete);
    logger.info("RemoveUnusedKeysAndTags took: {}", stopwatchRemoveUnusedKeysAndTags);
  }

  static boolean areTagsWithin5Minutes(String tagName1, String tagName2) {
    return Duration.between(uploadTagToLocalDateTime(tagName2), uploadTagToLocalDateTime(tagName1))
            .abs()
            .getSeconds()
        < (60 * 5);
  }

  private List<TextUnitDTO> getSourceTextUnitDTOs(
      Repository repository, String skipTextUnitsWithPattern, String skipAssetsWithPathPattern) {
    TextUnitSearcherParameters parameters = new TextUnitSearcherParameters();

    parameters.setRepositoryIds(repository.getId());
    parameters.setForRootLocale(true);
    parameters.setDoNotTranslateFilter(false);
    parameters.setUsedFilter(UsedFilter.USED);
    parameters.setSkipTextUnitWithPattern(skipTextUnitsWithPattern);
    parameters.setSkipAssetPathWithPattern(skipAssetsWithPathPattern);
    parameters.setPluralFormsFiltered(false);
    parameters.setOrderByTextUnitID(true);

    return textUnitSearcher.search(parameters);
  }

  private List<TextUnitDTO> getSourceTextUnitDTOsPluralOnly(
      Repository repository, String skipTextUnitsWithPattern, String skipAssetsWithPathPattern) {

    TextUnitSearcherParameters parameters = new TextUnitSearcherParameters();

    parameters.setRepositoryIds(repository.getId());
    parameters.setForRootLocale(true);
    parameters.setDoNotTranslateFilter(false);
    parameters.setUsedFilter(UsedFilter.USED);
    parameters.setSkipTextUnitWithPattern(skipTextUnitsWithPattern);
    parameters.setSkipAssetPathWithPattern(skipAssetsWithPathPattern);
    parameters.setSearchType(SearchType.ILIKE);
    parameters.setPluralFormsFiltered(false);
    parameters.setPluralFormOther("%");

    return textUnitSearcher.search(parameters);
  }

  public static String getTagForUpload(String repositoryName) {
    ZonedDateTime zonedDateTime = JSR310Migration.dateTimeNowInUTC();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TAG_DATE_FORMAT);
    return normalizeTagName(
        "%s%s_%s_%s"
            .formatted(
                TAG_PREFIX,
                repositoryName,
                formatter.format(zonedDateTime),
                Math.abs(UUID.randomUUID().getLeastSignificantBits() % 1000)));
  }

  public static LocalDateTime uploadTagToLocalDateTime(String tag) {

    if (tag == null || !tag.contains("_")) {
      throw new IllegalArgumentException("Invalid tag format: " + tag);
    }

    int dateEndIndex = tag.lastIndexOf('_'); // last part is a random number
    int dateStartIndex = dateEndIndex;

    for (int i = 0; i < 7; i++) {
      dateStartIndex = tag.lastIndexOf('_', dateStartIndex - 1);
      if (dateStartIndex == -1) {
        throw new IllegalArgumentException("Invalid tag format: " + tag);
      }
    }

    String dateTimePart = tag.substring(dateStartIndex + 1, dateEndIndex);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TAG_DATE_FORMAT);
    return LocalDateTime.parse(dateTimePart, formatter);
  }

  private static String getTagNamePrefixForRepository(String repositoryName) {
    return normalizeTagName(TAG_PREFIX_WITH_REPOSITORY.formatted(repositoryName));
  }

  /**
   * At least "/" are getting converted by phrase into "_", apply that logic on our side to have
   * consistent filtering
   */
  private static String normalizeTagName(String repositoryName) {
    return repositoryName.replace("/", "_");
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

    Set<RepositoryLocale> repositoryLocalesWithoutRootLocale =
        repositoryService.getRepositoryLocalesWithoutRootLocale(repository);

    String currentTags = getCurrentTagsForRepository(repository, projectId);

    OptionsParser optionsParser = new OptionsParser(optionList);
    Boolean integrityCheckKeepStatusIfFailedAndSameTarget =
        optionsParser.getBoolean("integrityCheckKeepStatusIfFailedAndSameTarget", true);

    AtomicReference<IntegrityChecksType> integrityChecksType =
        new AtomicReference<>(IntegrityChecksType.KEEP_STATUS_IF_SAME_TARGET);
    optionsParser.getString(
        "integrityChecksType", s -> integrityChecksType.set(IntegrityChecksType.valueOf(s)));

    // may already hit rate limit, according it is 4 qps ... there is a retry in the locale client
    // though.
    // but 4 qps is very low to download 64 locales.
    repositoryLocalesWithoutRootLocale.parallelStream()
        .forEach(
            locale ->
                pullLocaleTimed(
                    repository,
                    projectId,
                    locale,
                    pluralSeparator,
                    currentTags,
                    integrityChecksType.get(),
                    optionList));

    return null;
  }

  private void pullLocaleTimed(
      Repository repository,
      String projectId,
      RepositoryLocale repositoryLocale,
      String pluralSeparator,
      String currentTags,
      IntegrityChecksType integrityChecksType,
      List<String> optionList) {
    try (var timer =
        Timer.resource(meterRegistry, "ThirdPartyTMSPhrase.pullLocale")
            .tag("repository", repository.getName())) {

      pullLocale(
          repository,
          projectId,
          repositoryLocale,
          pluralSeparator,
          currentTags,
          integrityChecksType,
          optionList);
    }
  }

  private void pullLocale(
      Repository repository,
      String projectId,
      RepositoryLocale repositoryLocale,
      String pluralSeparator,
      String currentTags,
      IntegrityChecksType integrityChecksType,
      List<String> optionList) {

    String localeTag = repositoryLocale.getLocale().getBcp47Tag();
    logger.info("Downloading locale: {} from Phrase with tags: {}", localeTag, currentTags);

    OptionsParser optionsParser = new OptionsParser(optionList);
    Boolean nativeClient = optionsParser.getBoolean("nativeClient", NATIVE_CLIENT_DEFAULT_VALUE);
    Boolean escapeTags = optionsParser.getBoolean("escapeTags", true);
    Boolean escapeLineBreaks = optionsParser.getBoolean("escapeLineBreaks", false);
    Map<String, String> formatOptions = new HashMap<>();
    if (escapeTags) {
      formatOptions.put("escape_tags", "true");
    }
    if (escapeLineBreaks) {
      formatOptions.put("escape_linebreaks", "true");
    }
    final AtomicReference<EscapeType> escapeTypeAtomicReference =
        getEscapeTypeAtomicReference(nativeClient, optionsParser);

    Stopwatch localeDownloadStopWatch = Stopwatch.createStarted();
    String fileContent;

    if (nativeClient) {
      logger.info("Pulling locale with native and options: {}", formatOptions);

      fileContent =
          phraseClient.nativeLocaleDownload(
              projectId,
              localeTag,
              "xml",
              currentTags,
              formatOptions,
              () -> getCurrentTagsForRepository(repository, projectId));
    } else {
      fileContent =
          phraseClient.localeDownload(
              projectId,
              localeTag,
              "xml",
              currentTags,
              () -> getCurrentTagsForRepository(repository, projectId));
    }

    logger.info("Phrase locale download took: {}", localeDownloadStopWatch.elapsed());

    logger.debug("file content from pull: {}", fileContent);

    AndroidStringDocumentMapper mapper =
        new AndroidStringDocumentMapper(
            pluralSeparator, null, localeTag, repository.getName(), true, null);

    List<TextUnitDTO> textUnitDTOS =
        mapper.mapToTextUnits(AndroidStringDocumentReader.fromText(fileContent));

    // TODO(ja) that should not be necessary since the TextUnitBatchImporterService was fixed?
    // make sure that the source comment does not get replicated to all translations. Eventually
    // we may want to
    // communicate some comments, but anyway it should not be the source comment
    textUnitDTOS.forEach(t -> t.setComment(null));

    Stopwatch importStopWatch = Stopwatch.createStarted();

    textUnitBatchImporterService.importTextUnits(textUnitDTOS, integrityChecksType);
    logger.info("Time importing text units: {}", importStopWatch.elapsed());
  }

  /**
   * It should typically return a single tag, but if concurrent syncs, it could return more. That
   * should not be a problem when downloading or keys for mapping or pulling strings
   */
  private String getCurrentTagsForRepository(Repository repository, String projectId) {
    String tagNamePrefixForRepository = getTagNamePrefixForRepository(repository.getName());
    String tags =
        phraseClient.listTags(projectId).stream()
            .map(Tag::getName)
            .filter(Objects::nonNull)
            .filter(tagName -> tagName.startsWith(tagNamePrefixForRepository))
            .collect(Collectors.joining(","));

    if (tags.isBlank()) {
      throw new RuntimeException(
          "There are no current tags for the repository, make sure push was run first or that the repository name does not contain special character (which will need to get normalized)");
    }

    return tags;
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

    OptionsParser optionsParser = new OptionsParser(optionList);
    Boolean nativeClient = optionsParser.getBoolean("nativeClient", NATIVE_CLIENT_DEFAULT_VALUE);
    Map<String, String> formatOptions = getFormatOptions(optionsParser);

    final AtomicReference<EscapeType> escapeType =
        getEscapeTypeAtomicReference(nativeClient, optionsParser);

    List<TextUnitDTO> search =
        getSourceTextUnitDTOs(repository, skipTextUnitsWithPattern, skipAssetsWithPathPattern);

    List<TextUnitDTO> pluralTextUnitDTOs =
        getSourceTextUnitDTOsPluralOnly(
            repository, skipTextUnitsWithPattern, skipAssetsWithPathPattern);

    Map<String, List<TextUnitDTO>> pluralFormOtherToTextUnitDTO =
        pluralTextUnitDTOs.stream()
            .collect(
                Collectors.groupingBy(
                    AndroidStringDocumentMapper::getKeyToGroupByPluralOtherAndComment));

    pluralFormOtherToTextUnitDTO.forEach(
        (key, value) -> {
          if (value.size() != 6) {
            throw new RuntimeException("there must be only 6 text units per PluralFormOther value");
          }
        });

    Map<String, String> pluralFormToCommaId =
        pluralFormOtherToTextUnitDTO.entrySet().stream()
            .map(
                e ->
                    new SimpleEntry<>(
                        e.getKey(),
                        e.getValue().stream()
                            .sorted(new ByPluralFormComparator())
                            .map(TextUnitDTO::getTmTextUnitId)
                            .map(String::valueOf)
                            .collect(Collectors.joining(","))))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    Set<RepositoryLocale> repositoryLocalesWithoutRootLocale =
        repositoryService.getRepositoryLocalesWithoutRootLocale(repository);

    for (RepositoryLocale repositoryLocale : repositoryLocalesWithoutRootLocale) {
      List<TextUnitDTO> textUnitDTOS =
          getTextUnitDTOSForLocale(
              repository,
              skipTextUnitsWithPattern,
              skipAssetsWithPathPattern,
              includeTextUnitsWithPattern,
              repositoryLocale);

      if (textUnitDTOS.isEmpty()) {
        logger.info("Not translation, skip upload");
      } else {
        String fileContent =
            getFileContent(
                pluralSeparator, textUnitDTOS, false, pluralFormToCommaId, escapeType.get());
        logger.info("Push translation to phrase:\n{}", fileContent);

        if (nativeClient) {
          logger.info("Pushing translations with native and options: {}", formatOptions);
          phraseClient.nativeUploadAndWait(
              projectId,
              repositoryLocale.getLocale().getBcp47Tag(),
              "xml",
              repository.getName() + "-strings.xml",
              fileContent,
              null,
              formatOptions.isEmpty() ? null : formatOptions);
        } else {
          phraseClient.uploadAndWait(
              projectId,
              repositoryLocale.getLocale().getBcp47Tag(),
              "xml",
              repository.getName() + "-strings.xml",
              fileContent,
              null,
              null);
        }
      }
    }
  }

  private List<TextUnitDTO> getTextUnitDTOSForLocale(
      Repository repository,
      String skipTextUnitsWithPattern,
      String skipAssetsWithPathPattern,
      String includeTextUnitsWithPattern,
      RepositoryLocale repositoryLocale) {
    TextUnitSearcherParameters parameters = new TextUnitSearcherParameters();
    parameters.setRepositoryIds(repository.getId());
    parameters.setLocaleId(repositoryLocale.getLocale().getId());
    parameters.setDoNotTranslateFilter(false);
    parameters.setStatusFilter(StatusFilter.TRANSLATED);
    parameters.setUsedFilter(UsedFilter.USED);
    parameters.setSkipTextUnitWithPattern(skipTextUnitsWithPattern);
    parameters.setSkipAssetPathWithPattern(skipAssetsWithPathPattern);
    parameters.setIncludeTextUnitsWithPattern(includeTextUnitsWithPattern);
    parameters.setPluralFormsFiltered(true);
    return textUnitSearcher.search(parameters);
  }

  private static String getFileContent(
      String pluralSeparator,
      List<TextUnitDTO> textUnitDTOS,
      boolean useSource,
      Map<String, String> pluralFormToCommaId,
      EscapeType escapeType) {

    AndroidStringDocumentMapper androidStringDocumentMapper =
        new AndroidStringDocumentMapper(
            pluralSeparator, null, null, null, true, pluralFormToCommaId);

    AndroidStringDocument androidStringDocument =
        androidStringDocumentMapper.readFromTextUnits(textUnitDTOS, useSource);

    return new AndroidStringDocumentWriter(androidStringDocument, escapeType).toText();
  }

  @Override
  public void pullSource(
      Repository repository,
      String projectId,
      List<String> optionList,
      Map<String, String> localeMapping) {
    throw new UnsupportedOperationException("Pull source is not supported");
  }

  private static Map<String, String> getFormatOptions(OptionsParser optionsParser) {
    Boolean unescapeTags = optionsParser.getBoolean("unescapeTags", true);
    Boolean unescapeLineBreaks = optionsParser.getBoolean("unescapeLineBreaks", false);
    Map<String, String> formatOptions = new HashMap<>();
    if (unescapeTags) {
      formatOptions.put("unescape_tags", "true");
    }

    if (unescapeLineBreaks) {
      formatOptions.put("unescape_linebreaks", "true");
    }
    return formatOptions;
  }

  private static AtomicReference<EscapeType> getEscapeTypeAtomicReference(
      Boolean nativeClient, OptionsParser optionsParser) {
    final AtomicReference<EscapeType> escapeType =
        new AtomicReference<>(EscapeType.QUOTE_AND_NEW_LINE);
    if (nativeClient) {
      escapeType.set(EscapeType.NEW_LINE);
    }
    optionsParser.getString("escapeType", s -> escapeType.set(EscapeType.valueOf(s)));
    return escapeType;
  }

  static class ByPluralFormComparator implements Comparator<TextUnitDTO> {

    private final Map<String, Integer> orderMap;

    public ByPluralFormComparator() {
      this.orderMap = new HashMap<>();
      int i = 0;
      for (String v : Arrays.asList("zero", "one", "two", "few", "many", "other")) {
        this.orderMap.put(v, i++);
      }
    }

    @Override
    public int compare(TextUnitDTO o1, TextUnitDTO o2) {
      return Integer.compare(
          orderMap.getOrDefault(o1.getPluralForm(), -1),
          orderMap.getOrDefault(o2.getPluralForm(), -1));
    }
  }
}
