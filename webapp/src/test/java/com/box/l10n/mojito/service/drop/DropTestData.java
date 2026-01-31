package com.box.l10n.mojito.service.drop;

import com.box.l10n.mojito.entity.*;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionRepository;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import com.box.l10n.mojito.service.assetExtraction.AssetMappingService;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.repository.RepositoryLocaleRepository;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.search.*;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.util.*;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author jaurambault, wadimw
 */
@Configurable
public class DropTestData {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(DropTestData.class);

  @Autowired TMService tmService;

  @Autowired LocaleService localeService;

  @Autowired AssetExtractionRepository assetExtractionRepository;

  @Autowired AssetMappingService assetMappingService;

  @Autowired AssetExtractionService assetExtractionService;

  @Autowired AssetService assetService;

  @Autowired RepositoryService repositoryService;

  @Autowired RepositoryLocaleRepository repositoryLocaleRepository;

  @Autowired TextUnitSearcher textUnitSearcher;

  public Repository repository;
  public TM tm;
  public Asset asset;
  public AssetExtraction assetExtraction;
  public List<Locale> locales;
  public Map<String, TMTextUnit> tmTextUnits;
  public Map<Locale, Map<String, TMTextUnitVariant>> addCurrentTMTextUnitVariants;

  TestIdWatcher testIdWatcher;

  public DropTestData(TestIdWatcher testIdWatcher) {
    this.testIdWatcher = testIdWatcher;
  }

  @Transactional
  public static DropTestData createWithDefaultData(TestIdWatcher testIdWatcher) throws Exception {
    var data = new DropTestData(testIdWatcher);
    data.createRepository();
    for (var localeTag : List.of("ko-KR", "fr-FR", "fr-CA", "ja-JP")) {
      data.addLocale(localeTag);
    }
    data.setLocaleMapping("fr-FR", "fr-CA", false);

    data.createUnits(
        List.of(
            new TextUnitDefinition(
                "zuora_error_message_verify_state_province",
                "Please enter a valid state, region or province",
                "Comment1"),
            new TextUnitDefinition("TEST2", "Content2", "Comment2")));
    data.createTranslations(
        "ko-KR",
        List.of(
            new TranslationDefinition(
                "zuora_error_message_verify_state_province", "올바른 국가, 지역 또는 시/도를 입력하십시오.")));
    data.createTranslations(
        "fr-FR",
        List.of(
            new TranslationDefinition(
                "zuora_error_message_verify_state_province",
                "Veuillez indiquer un état, une région ou une province valide.")));
    data.createTranslations("fr-CA", List.of(new TranslationDefinition("TEST2", "Content2 fr-CA")));
    return data;
  }

  @Transactional
  public static DropTestData createWithGeneratedUnits(
      TestIdWatcher testIdWatcher, List<String> locales, int unitCount) throws Exception {
    var data = new DropTestData(testIdWatcher);
    data.createRepository();
    for (var localeTag : locales) {
      data.addLocale(localeTag);
    }

    data.createUnits(
        IntStream.range(0, unitCount)
            .mapToObj(
                i ->
                    new TextUnitDefinition(
                        String.format("generated_unit_id_%,d", i),
                        String.format("Content of generated unit %,d", i),
                        String.format("Comment for generated unit %,d", i)))
            .toList());

    return data;
  }

  public Locale findLocaleForTag(String bcp47Tag) {
    return localeService.findByBcp47Tag(bcp47Tag);
  }

  public List<String> getLocaleTags() {
    return locales.stream().map(Locale::getBcp47Tag).toList();
  }

  @Transactional
  public void createRepository() throws Exception {
    repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
    tm = repository.getTm();
    locales = new ArrayList<>();
  }

  @Transactional
  public void addLocale(String localeTag) throws Exception {
    var locale = findLocaleForTag(localeTag);
    repositoryService.addRepositoryLocale(repository, locale.getBcp47Tag());
    locales.add(locale);
  }

  @Transactional
  public void setLocaleMapping(
      String parentLocaleTag, String childLocaleTag, boolean toBeFullyTranslated) {
    RepositoryLocale parentLocale =
        repositoryLocaleRepository.findByRepositoryAndLocale(
            repository, findLocaleForTag(parentLocaleTag));
    RepositoryLocale childLocale =
        repositoryLocaleRepository.findByRepositoryAndLocale(
            repository, findLocaleForTag(childLocaleTag));
    childLocale.setParentLocale(parentLocale);
    childLocale.setToBeFullyTranslated(toBeFullyTranslated);
    repositoryLocaleRepository.save(childLocale);
  }

  @Transactional
  public void createUnits(Collection<TextUnitDefinition> textUnits) {
    asset =
        assetService.createAssetWithContent(repository.getId(), "fake_for_test", "fake for test");
    assetExtraction = new AssetExtraction();
    assetExtraction.setAsset(asset);
    assetExtraction = assetExtractionRepository.save(assetExtraction);

    tmTextUnits = new HashMap<>();
    for (TextUnitDefinition textUnit : textUnits) {
      var tmTextUnit =
          tmService.addTMTextUnit(
              tm.getId(), asset.getId(), textUnit.name, textUnit.content, textUnit.comment);
      tmTextUnits.put(textUnit.name, tmTextUnit);

      var assetTextUnit =
          assetExtractionService.createAssetTextUnit(
              assetExtraction, textUnit.name, textUnit.content, textUnit.comment);
    }

    assetMappingService.mapAssetTextUnitAndCreateTMTextUnit(
        assetExtraction.getId(), tm.getId(), asset.getId(), null, PollableTask.INJECT_CURRENT_TASK);
    assetExtractionService.markAssetExtractionAsLastSuccessful(asset, assetExtraction);
  }

  @Transactional
  public void createTranslations(String localeTag, Collection<TranslationDefinition> translations) {
    var locale = findLocaleForTag(localeTag);
    if (Objects.isNull(addCurrentTMTextUnitVariants)) {
      addCurrentTMTextUnitVariants = new HashMap<>();
    }

    addCurrentTMTextUnitVariants.putIfAbsent(locale, new HashMap<>());
    var variantsForLocale = addCurrentTMTextUnitVariants.get(locale);

    for (TranslationDefinition translation : translations) {
      var tmTextUnit = tmTextUnits.get(translation.name);
      variantsForLocale.put(
          translation.name,
          tmService.addCurrentTMTextUnitVariant(
              tmTextUnit.getId(), locale.getId(), translation.content));
    }
  }

  public List<TextUnitDTO> getTextUnitsForStatus(
      StatusFilter statusFilter, List<String> bcp47Tags) {
    TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
    textUnitSearcherParameters.setRepositoryIds(repository.getId());
    textUnitSearcherParameters.setStatusFilter(statusFilter);
    textUnitSearcherParameters.setUsedFilter(UsedFilter.USED);
    textUnitSearcherParameters.setLocaleTags(bcp47Tags);

    return textUnitSearcher.search(textUnitSearcherParameters);
  }

  public ExportDropConfig getExportDropConfig(List<String> bcp47Tags) {
    ExportDropConfig exportDropConfig = new ExportDropConfig();
    exportDropConfig.setRepositoryId(repository.getId());
    exportDropConfig.setBcp47Tags(bcp47Tags);
    return exportDropConfig;
  }

  public static class TextUnitDefinition {
    public String name;
    public String content;
    public String comment;

    public TextUnitDefinition(String name, String content, String comment) {
      this.name = name;
      this.content = content;
      this.comment = comment;
    }
  }

  public static class TranslationDefinition {
    public String name;
    public String content;

    public TranslationDefinition(String name, String content) {
      this.name = name;
      this.content = content;
    }
  }
}
