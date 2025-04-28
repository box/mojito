package com.box.l10n.mojito.service.thirdparty.smartling.glossary;

import com.box.l10n.mojito.service.WordCountService;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "l10n.glossary.cache.enabled", havingValue = "true")
public class GlossaryCacheBuilder {

  static Logger logger = LoggerFactory.getLogger(GlossaryCacheBuilder.class);
  private final GlossaryCacheConfiguration glossaryCacheConfiguration;

  private final GlossaryCacheBlobStorage blobStorage;

  private final TextUnitSearcher textUnitSearcher;

  private final WordCountService wordCountService;

  private final StemmerService stemmer;

  private static final Map<SmartlingGlossaryConfigParameter, Pattern> CONFIG_PATTERNS =
      new HashMap<>();

  public GlossaryCacheBuilder(
      GlossaryCacheConfiguration glossaryCacheConfiguration,
      GlossaryCacheBlobStorage blobStorage,
      TextUnitSearcher textUnitSearcher,
      WordCountService wordCountService,
      StemmerService stemmer) {
    this.glossaryCacheConfiguration = glossaryCacheConfiguration;
    this.blobStorage = blobStorage;
    this.textUnitSearcher = textUnitSearcher;
    this.wordCountService = wordCountService;
    this.stemmer = stemmer;
    for (SmartlingGlossaryConfigParameter param : SmartlingGlossaryConfigParameter.values()) {
      CONFIG_PATTERNS.put(param, Pattern.compile("--- " + param + ":\\s*([^---]*)"));
    }
  }

  public void buildCache() {
    if (glossaryCacheConfiguration.getEnabled()) {
      logger.info("Building Glossary Cache");
      if (glossaryCacheConfiguration.getRepositories() == null
          || glossaryCacheConfiguration.getRepositories().isEmpty()) {
        logger.warn("No repositories configured for Glossary cache builder, skipping cache build");
        return;
      }
      GlossaryCache glossaryCache = buildGlossaryCache();
      logger.info("Glossary Cache build completed with {} terms", glossaryCache.getCache().size());
      blobStorage.putGlossaryCache(glossaryCache);
    }
  }

  GlossaryCache buildGlossaryCache() {
    GlossaryCache glossaryCache = new GlossaryCache();

    List<GlossaryTerm> glossaryTerms = retrieveTranslationsForGlossaryTerms(getSourceTerms());

    for (GlossaryTerm glossaryTerm : glossaryTerms) {
      processTerm(glossaryTerm, glossaryCache);
    }
    return glossaryCache;
  }

  protected void processTerm(GlossaryTerm glossaryTerm, GlossaryCache glossaryCache) {
    int termWordCount = wordCountService.getEnglishWordCount(glossaryTerm.getTerm());
    if (termWordCount > glossaryCache.getMaxNGramSize()) {
      logger.debug("Setting glossary cache max ngram size to {}", termWordCount);
      glossaryCache.setMaxNGramSize(termWordCount);
    }
    logger.debug("Adding term to glossary cache: {}", glossaryTerm.getTerm());
    glossaryCache.add(stemmer.stem(glossaryTerm.getTerm()), glossaryTerm);
  }

  List<GlossaryTerm> retrieveTranslationsForGlossaryTerms(
      Map<Long, List<GlossaryTerm>> glossaryTermMap) {
    TextUnitSearcherParameters textUnitSearcherParameters;
    List<TextUnitDTO> textUnitDTOs;

    textUnitSearcherParameters = new TextUnitSearcherParameters();
    textUnitSearcherParameters.setRepositoryNames(glossaryCacheConfiguration.getRepositories());
    textUnitSearcherParameters.setUsedFilter(UsedFilter.USED);
    textUnitSearcherParameters.setRootLocaleExcluded(true);
    textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);

    for (TextUnitDTO textUnitDTO : textUnitDTOs) {
      List<GlossaryTerm> glossaryTerms = glossaryTermMap.get(textUnitDTO.getTmTextUnitId());
      if (glossaryTerms != null) {
        for (GlossaryTerm glossaryTerm : glossaryTerms) {
          logger.debug(
              "Adding translation for glossary term (tmTextUnitId: {})",
              glossaryTerm.getTmTextUnitId());
          glossaryTerm.addLocaleTranslation(textUnitDTO.getTargetLocale(), textUnitDTO.getTarget());
        }
      }
    }

    return glossaryTermMap.values().stream().flatMap(List::stream).collect(Collectors.toList());
  }

  protected Map<Long, List<GlossaryTerm>> getSourceTerms() {
    TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
    textUnitSearcherParameters.setRepositoryNames(glossaryCacheConfiguration.getRepositories());
    textUnitSearcherParameters.setUsedFilter(UsedFilter.USED);
    textUnitSearcherParameters.setForRootLocale(true);

    List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);
    return textUnitDTOs.stream()
        .map(this::mapTextUnitDTOToGlossaryTerms)
        .flatMap(List::stream)
        .collect(Collectors.groupingBy(GlossaryTerm::getTmTextUnitId));
  }

  /**
   * Maps a TextUnitDTO to a list of GlossaryTerm objects. A glossary source term can have
   * variations, which are handled as individual @link GlossaryTerm objects.
   *
   * @param textUnitDTO
   * @return list of GlossaryTerm objects
   */
  protected List<GlossaryTerm> mapTextUnitDTOToGlossaryTerms(TextUnitDTO textUnitDTO) {
    List<GlossaryTerm> glossaryTerms = new ArrayList<>();
    String comment = textUnitDTO.getComment();
    boolean caseSensitive = getSmartlingTermCaseSensitive(comment);
    boolean doNotTranslate = getSmartlingTermDoNotTranslate(comment);
    boolean exactMatch = getSmartlingTermExactMatch(comment);
    List<String> variations = getSmartlingTermVariations(comment);

    GlossaryTerm glossaryTerm =
        new GlossaryTerm(
            textUnitDTO.getSource(),
            exactMatch,
            caseSensitive,
            doNotTranslate,
            textUnitDTO.getTmTextUnitId());
    glossaryTerms.add(glossaryTerm);

    for (String variation : variations) {
      GlossaryTerm variationTerm =
          new GlossaryTerm(
              variation, exactMatch, caseSensitive, doNotTranslate, textUnitDTO.getTmTextUnitId());
      glossaryTerms.add(variationTerm);
    }

    return glossaryTerms;
  }

  private Boolean getSmartlingTermCaseSensitive(String comment) {
    return Boolean.parseBoolean(
        extractProperty(
            comment, CONFIG_PATTERNS.get(SmartlingGlossaryConfigParameter.CASE_SENSITIVE)));
  }

  private Boolean getSmartlingTermDoNotTranslate(String comment) {
    return Boolean.parseBoolean(
        extractProperty(
            comment, CONFIG_PATTERNS.get(SmartlingGlossaryConfigParameter.DO_NOT_TRANSLATE)));
  }

  private Boolean getSmartlingTermExactMatch(String comment) {
    return Boolean.parseBoolean(
        extractProperty(
            comment, CONFIG_PATTERNS.get(SmartlingGlossaryConfigParameter.EXACT_MATCH)));
  }

  private List<String> getSmartlingTermVariations(String comment) {
    String variations =
        extractProperty(comment, CONFIG_PATTERNS.get(SmartlingGlossaryConfigParameter.VARIATIONS));
    if (variations != null) {
      return Arrays.asList(variations.split("\\s*,\\s*"));
    }
    return Collections.emptyList();
  }

  private String extractProperty(String input, Pattern pattern) {
    Matcher matcher = pattern.matcher(input);
    if (matcher.find()) {
      return matcher.group(1).trim();
    }
    return null;
  }
}
