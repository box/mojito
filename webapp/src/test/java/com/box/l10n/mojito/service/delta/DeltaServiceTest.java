package com.box.l10n.mojito.service.delta;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.entity.AssetTextUnitToTMTextUnit;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.PullRun;
import com.box.l10n.mojito.entity.PullRunAsset;
import com.box.l10n.mojito.entity.PushRun;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.TM;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionRepository;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import com.box.l10n.mojito.service.assetExtraction.AssetTextUnitToTMTextUnitRepository;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.delta.dtos.DeltaLocaleDataDTO;
import com.box.l10n.mojito.service.delta.dtos.DeltaResponseDTO;
import com.box.l10n.mojito.service.delta.dtos.DeltaTranslationDTO;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pullrun.PullRunAssetService;
import com.box.l10n.mojito.service.pullrun.PullRunRepository;
import com.box.l10n.mojito.service.pullrun.PullRunService;
import com.box.l10n.mojito.service.pushrun.PushRunService;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMRepository;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTestData;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import com.box.l10n.mojito.service.tm.TextUnitVariantDeltaDTO;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

public class DeltaServiceTest extends ServiceTestBase {

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Autowired PullRunAssetService pullRunAssetService;

  @Autowired DeltaService deltaService;

  @Autowired PushRunService pushRunService;

  @Autowired PullRunRepository pullRunRepository;
  @Autowired PullRunService pullRunService;

  @Autowired TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;

  @Autowired TMTextUnitVariantRepository tmTextUnitVariantRepository;

  @Autowired TMTextUnitRepository tmTextUnitRepository;

  @Autowired LocaleService localeService;

  @Autowired TMRepository tmRepository;

  @Autowired RepositoryService repositoryService;

  @Autowired AssetService assetService;

  @Autowired TMService tmService;

  @Autowired AssetExtractionRepository assetExtractionRepository;

  @Autowired AssetExtractionService assetExtractionService;

  @Autowired AssetTextUnitToTMTextUnitRepository assetTextUnitToTMTextUnitRepository;

  @Test
  public void testGetDeltasFromDateWorks() {
    TMTestData tmTestData = new TMTestData(testIdWatcher);
    Assert.assertNotNull(tmTestData.repository);

    List<TextUnitVariantDeltaDTO> deltasFromBeginning =
        deltaService
            .getDeltasForDates(
                tmTestData.repository,
                Arrays.asList(tmTestData.frFR, tmTestData.frCA, tmTestData.koKR, tmTestData.jaJP),
                ZonedDateTime.now().plusSeconds(1),
                null,
                Pageable.unpaged())
            .getContent();
    Assert.assertEquals(0, deltasFromBeginning.size());

    List<TextUnitVariantDeltaDTO> deltasFromCreation =
        deltaService
            .getDeltasForDates(
                tmTestData.repository,
                Arrays.asList(tmTestData.frFR, tmTestData.frCA, tmTestData.koKR, tmTestData.jaJP),
                JSR310Migration.newDateTimeCtorAtEpoch(),
                ZonedDateTime.now().plusHours(1),
                PageRequest.of(0, 1, Sort.Direction.ASC, "id"))
            .getContent();
    Assert.assertNotEquals(0, deltasFromCreation.size());
  }

  @Test
  public void testGetDeltasToDateWorks() {
    TMTestData tmTestData = new TMTestData(testIdWatcher);
    Assert.assertNotNull(tmTestData.repository);

    List<TextUnitVariantDeltaDTO> deltasFromBeginning =
        deltaService
            .getDeltasForDates(
                tmTestData.repository,
                Arrays.asList(tmTestData.frFR, tmTestData.frCA, tmTestData.koKR, tmTestData.jaJP),
                ZonedDateTime.now().plusSeconds(1),
                null,
                Pageable.unpaged())
            .getContent();
    Assert.assertEquals(0, deltasFromBeginning.size());

    List<TextUnitVariantDeltaDTO> deltasFromCreation =
        deltaService
            .getDeltasForDates(
                tmTestData.repository,
                Arrays.asList(tmTestData.frFR, tmTestData.frCA, tmTestData.koKR, tmTestData.jaJP),
                JSR310Migration.newDateTimeCtorAtEpoch(),
                JSR310Migration.newDateTimeCtorAtEpoch(),
                PageRequest.of(0, 1, Sort.Direction.ASC, "id"))
            .getContent();

    Assert.assertEquals(0, deltasFromCreation.size());
  }

  @Test
  public void testGetDeltasOnlyUsedTextUnits() throws Exception {
    Locale koKR = localeService.findByBcp47Tag("ko-KR");
    Locale frFR = localeService.findByBcp47Tag("fr-FR");
    Repository repository = repositoryService.createRepository("testGetDeltasOnlyUsedTextUnits");
    RepositoryLocale repoLocaleKoKR =
        repositoryService.addRepositoryLocale(repository, koKR.getBcp47Tag());
    RepositoryLocale repoLocaleFrFR =
        repositoryService.addRepositoryLocale(repository, frFR.getBcp47Tag());
    TM tm = repository.getTm();
    Asset asset =
        assetService.createAssetWithContent(repository.getId(), "fake_for_test", "fake for test");
    Long assetId = asset.getId();

    TMTextUnit usedTextUnit =
        tmService.addTMTextUnit(tm.getId(), assetId, "used_text_unit", "active string", "Comment1");
    TMTextUnit unusedTextUnit =
        tmService.addTMTextUnit(tm.getId(), assetId, "unused", "inactive string", "Comment2");

    AssetExtraction assetExtraction = new AssetExtraction();
    assetExtraction.setAsset(asset);
    assetExtraction = assetExtractionRepository.save(assetExtraction);

    AssetTextUnit usedAssetTextUnit =
        assetExtractionService.createAssetTextUnit(
            assetExtraction,
            usedTextUnit.getName(),
            usedTextUnit.getContent(),
            usedTextUnit.getComment());

    // Only mark one TextUnit as used
    AssetTextUnitToTMTextUnit assetTextUnitToTMTextUnit = new AssetTextUnitToTMTextUnit();
    assetTextUnitToTMTextUnit.setAssetExtraction(assetExtraction);
    assetTextUnitToTMTextUnit.setAssetTextUnit(usedAssetTextUnit);
    assetTextUnitToTMTextUnit.setTmTextUnit(usedTextUnit);
    assetTextUnitToTMTextUnitRepository.save(assetTextUnitToTMTextUnit);

    assetExtractionService.markAssetExtractionAsLastSuccessful(asset, assetExtraction);

    tmService.addCurrentTMTextUnitVariant(usedTextUnit.getId(), koKR.getId(), "활성 문자열");
    tmService.addCurrentTMTextUnitVariant(usedTextUnit.getId(), frFR.getId(), "chaîne active");
    tmService.addCurrentTMTextUnitVariant(unusedTextUnit.getId(), frFR.getId(), "inutilisé");

    List<TextUnitVariantDeltaDTO> deltasFromBeggining =
        deltaService
            .getDeltasForDates(
                repository,
                null,
                JSR310Migration.newDateTimeCtorAtEpoch(),
                ZonedDateTime.now().plusHours(1),
                Pageable.unpaged())
            .getContent();

    Assert.assertEquals(2, deltasFromBeggining.size());
  }

  @Test
  public void testGetDeltasNoRejectedTextUnits() throws Exception {
    Locale koKR = localeService.findByBcp47Tag("ko-KR");
    Locale frFR = localeService.findByBcp47Tag("fr-FR");
    Repository repository = repositoryService.createRepository("testGetDeltasNoRejectedTextUnits");
    RepositoryLocale repoLocaleKoKR =
        repositoryService.addRepositoryLocale(repository, koKR.getBcp47Tag());
    RepositoryLocale repoLocaleFrFR =
        repositoryService.addRepositoryLocale(repository, frFR.getBcp47Tag());
    TM tm = repository.getTm();
    Asset asset =
        assetService.createAssetWithContent(repository.getId(), "fake_for_test", "fake for test");
    Long assetId = asset.getId();

    TMTextUnit usedTextUnit =
        tmService.addTMTextUnit(tm.getId(), assetId, "used_text_unit", "active string", "Comment1");

    AssetExtraction assetExtraction = new AssetExtraction();
    assetExtraction.setAsset(asset);
    assetExtraction = assetExtractionRepository.save(assetExtraction);

    AssetTextUnit usedAssetTextUnit =
        assetExtractionService.createAssetTextUnit(
            assetExtraction,
            usedTextUnit.getName(),
            usedTextUnit.getContent(),
            usedTextUnit.getComment());

    // Only mark one TextUnit as used
    AssetTextUnitToTMTextUnit assetTextUnitToTMTextUnit = new AssetTextUnitToTMTextUnit();
    assetTextUnitToTMTextUnit.setAssetExtraction(assetExtraction);
    assetTextUnitToTMTextUnit.setAssetTextUnit(usedAssetTextUnit);
    assetTextUnitToTMTextUnit.setTmTextUnit(usedTextUnit);
    assetTextUnitToTMTextUnitRepository.save(assetTextUnitToTMTextUnit);

    assetExtractionService.markAssetExtractionAsLastSuccessful(asset, assetExtraction);

    TMTextUnitVariant rejectedVariant =
        tmService.addCurrentTMTextUnitVariant(usedTextUnit.getId(), koKR.getId(), "활성 문자열");
    rejectedVariant.setIncludedInLocalizedFile(false);
    tmTextUnitVariantRepository.save(rejectedVariant);

    tmService.addCurrentTMTextUnitVariant(usedTextUnit.getId(), frFR.getId(), "chaîne active");

    List<TextUnitVariantDeltaDTO> deltasFromBeggining =
        deltaService
            .getDeltasForDates(
                repository,
                null,
                JSR310Migration.newDateTimeCtorAtEpoch(),
                ZonedDateTime.now().plusHours(1),
                Pageable.unpaged())
            .getContent();

    Assert.assertEquals(1, deltasFromBeggining.size());
  }

  @Test
  public void testGetDeltasSpecificLocales() throws Exception {
    Locale koKR = localeService.findByBcp47Tag("ko-KR");
    Locale frFR = localeService.findByBcp47Tag("fr-FR");
    Repository repository = repositoryService.createRepository("testGetDeltasSpecificLocales");
    repositoryService.addRepositoryLocale(repository, koKR.getBcp47Tag());
    repositoryService.addRepositoryLocale(repository, frFR.getBcp47Tag());
    TM tm = repository.getTm();
    Asset asset =
        assetService.createAssetWithContent(repository.getId(), "fake_for_test", "fake for test");
    Long assetId = asset.getId();

    TMTextUnit usedTextUnit1 =
        tmService.addTMTextUnit(tm.getId(), assetId, "used_text_unit", "active string", "Comment1");
    TMTextUnit usedTextUnit2 =
        tmService.addTMTextUnit(
            tm.getId(), assetId, "used_text_unit 2", "active string", "Comment1");

    AssetExtraction assetExtraction = new AssetExtraction();
    assetExtraction.setAsset(asset);
    assetExtraction = assetExtractionRepository.save(assetExtraction);

    AssetTextUnit usedAssetTextUnit1 =
        assetExtractionService.createAssetTextUnit(
            assetExtraction,
            usedTextUnit1.getName(),
            usedTextUnit1.getContent(),
            usedTextUnit1.getComment());

    AssetTextUnit usedAssetTextUnit2 =
        assetExtractionService.createAssetTextUnit(
            assetExtraction,
            usedTextUnit2.getName(),
            usedTextUnit2.getContent(),
            usedTextUnit2.getComment());

    AssetTextUnitToTMTextUnit assetTextUnitToTMTextUnit1 = new AssetTextUnitToTMTextUnit();
    assetTextUnitToTMTextUnit1.setAssetExtraction(assetExtraction);
    assetTextUnitToTMTextUnit1.setAssetTextUnit(usedAssetTextUnit1);
    assetTextUnitToTMTextUnit1.setTmTextUnit(usedTextUnit1);

    AssetTextUnitToTMTextUnit assetTextUnitToTMTextUnit2 = new AssetTextUnitToTMTextUnit();
    assetTextUnitToTMTextUnit2.setAssetExtraction(assetExtraction);
    assetTextUnitToTMTextUnit2.setAssetTextUnit(usedAssetTextUnit2);
    assetTextUnitToTMTextUnit2.setTmTextUnit(usedTextUnit2);
    assetTextUnitToTMTextUnitRepository.saveAll(
        Arrays.asList(assetTextUnitToTMTextUnit1, assetTextUnitToTMTextUnit2));

    assetExtractionService.markAssetExtractionAsLastSuccessful(asset, assetExtraction);

    tmService.addCurrentTMTextUnitVariant(usedTextUnit1.getId(), koKR.getId(), "활성 문자열");
    tmService.addCurrentTMTextUnitVariant(usedTextUnit1.getId(), frFR.getId(), "chaîne active");
    tmService.addCurrentTMTextUnitVariant(usedTextUnit2.getId(), frFR.getId(), "inutilisé");

    List<TextUnitVariantDeltaDTO> frDeltasFromStart =
        deltaService
            .getDeltasForDates(
                repository,
                Collections.singletonList(frFR),
                JSR310Migration.newDateTimeCtorAtEpoch(),
                ZonedDateTime.now().plusHours(1),
                Pageable.unpaged())
            .getContent();
    Assert.assertEquals(2, frDeltasFromStart.size());

    List<TextUnitVariantDeltaDTO> koKrDeltasFromStart =
        deltaService
            .getDeltasForDates(
                repository,
                Collections.singletonList(koKR),
                JSR310Migration.newDateTimeCtorAtEpoch(),
                ZonedDateTime.now().plusHours(1),
                Pageable.unpaged())
            .getContent();
    Assert.assertEquals(1, koKrDeltasFromStart.size());

    List<TextUnitVariantDeltaDTO> noSpecificLocalesDeltasFromStart =
        deltaService
            .getDeltasForDates(
                repository,
                null,
                JSR310Migration.newDateTimeCtorAtEpoch(),
                ZonedDateTime.now().plusHours(1),
                Pageable.unpaged())
            .getContent();
    Assert.assertEquals(3, noSpecificLocalesDeltasFromStart.size());

    List<TextUnitVariantDeltaDTO> allLocalesDeltasFromStart =
        deltaService
            .getDeltasForDates(
                repository,
                Arrays.asList(koKR, frFR),
                JSR310Migration.newDateTimeCtorAtEpoch(),
                ZonedDateTime.now().plusHours(1),
                Pageable.unpaged())
            .getContent();
    Assert.assertEquals(3, allLocalesDeltasFromStart.size());
  }

  @Transactional
  @Test
  public void testGetDeltasForRunsWorks() throws Exception {
    Locale koKR = localeService.findByBcp47Tag("ko-KR");
    Locale frFR = localeService.findByBcp47Tag("fr-FR");
    Locale roRO = localeService.findByBcp47Tag("ro-RO");
    Repository repository = repositoryService.createRepository("getDeltasForRunsWorks");
    RepositoryLocale repoLocaleKoKR =
        repositoryService.addRepositoryLocale(repository, koKR.getBcp47Tag());
    RepositoryLocale repoLocaleFrFR =
        repositoryService.addRepositoryLocale(repository, frFR.getBcp47Tag());
    RepositoryLocale repoLocaleRoRO =
        repositoryService.addRepositoryLocale(repository, roRO.getBcp47Tag());
    TM tm = repository.getTm();
    Asset asset =
        assetService.createAssetWithContent(repository.getId(), "fake_for_test", "fake for test");
    Long assetId = asset.getId();

    TMTextUnit usedTextUnit =
        tmService.addTMTextUnit(tm.getId(), assetId, "used_text_unit", "active string", "Comment1");
    TMTextUnit unusedTextUnit =
        tmService.addTMTextUnit(tm.getId(), assetId, "unused", "inactive string", "Comment2");

    AssetExtraction assetExtraction = new AssetExtraction();
    assetExtraction.setAsset(asset);
    assetExtraction = assetExtractionRepository.save(assetExtraction);

    AssetTextUnit usedAssetTextUnit =
        assetExtractionService.createAssetTextUnit(
            assetExtraction,
            usedTextUnit.getName(),
            usedTextUnit.getContent(),
            usedTextUnit.getComment());

    AssetTextUnitToTMTextUnit assetTextUnitToTMTextUnit = new AssetTextUnitToTMTextUnit();
    assetTextUnitToTMTextUnit.setAssetExtraction(assetExtraction);
    assetTextUnitToTMTextUnit.setAssetTextUnit(usedAssetTextUnit);
    assetTextUnitToTMTextUnit.setTmTextUnit(usedTextUnit);
    assetTextUnitToTMTextUnitRepository.save(assetTextUnitToTMTextUnit);

    assetExtractionService.markAssetExtractionAsLastSuccessful(asset, assetExtraction);

    PushRun firstPushRun = pushRunService.createPushRun(repository);
    pushRunService.associatePushRunToTextUnitIds(
        firstPushRun, asset, Arrays.asList(usedTextUnit.getId(), unusedTextUnit.getId()));

    PushRun emptyPushRun = pushRunService.createPushRun(repository);
    PullRun emptyPullRun = createPullRunForTest(repository);

    TMTextUnitVariant koKrUsedTuv =
        tmService.addCurrentTMTextUnitVariant(usedTextUnit.getId(), koKR.getId(), "활성 문자열");
    TMTextUnitVariant frFrUsedTuv =
        tmService.addCurrentTMTextUnitVariant(usedTextUnit.getId(), frFR.getId(), "chaîne active");
    TMTextUnitVariant frFrUnusedTuv =
        tmService.addCurrentTMTextUnitVariant(unusedTextUnit.getId(), frFR.getId(), "inutilisé");
    TMTextUnitVariant rejectedTuv =
        tmService.addCurrentTMTextUnitVariant(usedTextUnit.getId(), roRO.getId(), "Rejected");
    rejectedTuv.setIncludedInLocalizedFile(false);
    tmTextUnitVariantRepository.save(rejectedTuv);

    DeltaResponseDTO deltas =
        deltaService.getDeltasForRuns(
            repository,
            Arrays.asList(frFR, koKR, roRO),
            Collections.singletonList(emptyPushRun),
            null);
    Assert.assertEquals(0, deltas.getTranslationsPerLocale().size());

    deltas =
        deltaService.getDeltasForRuns(
            repository,
            Arrays.asList(frFR, koKR, roRO),
            Collections.singletonList(firstPushRun),
            Collections.singletonList(emptyPullRun));
    Assert.assertEquals(2, deltas.getTranslationsPerLocale().size());
    Assert.assertEquals(3, getDeltaTranslationsFromDeltaFile(deltas).size());

    PullRun pullRunWithInitialTranslations = createPullRunForTest(repository);
    associatePullRunToTextUnitIds(
        pullRunWithInitialTranslations,
        asset,
        Arrays.asList(frFrUsedTuv, koKrUsedTuv, frFrUnusedTuv, rejectedTuv));

    deltas =
        deltaService.getDeltasForRuns(
            repository,
            Arrays.asList(frFR, koKR, roRO),
            Collections.singletonList(firstPushRun),
            Collections.singletonList(pullRunWithInitialTranslations));
    Assert.assertEquals(0, deltas.getTranslationsPerLocale().size());

    tmService.addCurrentTMTextUnitVariant(
        usedTextUnit.getId(), frFR.getId(), "chaîne active - soon to be reverted");
    TMTextUnitVariant frFrUsedTuvOriginalValue =
        tmService.addCurrentTMTextUnitVariant(usedTextUnit.getId(), frFR.getId(), "chaîne active");

    PullRun pullRunWithRevertedTranslations = createPullRunForTest(repository);
    associatePullRunToTextUnitIds(
        pullRunWithRevertedTranslations,
        asset,
        Arrays.asList(frFrUsedTuvOriginalValue, koKrUsedTuv, frFrUnusedTuv, rejectedTuv));

    deltas =
        deltaService.getDeltasForRuns(
            repository,
            Arrays.asList(frFR, koKR, roRO),
            Collections.singletonList(firstPushRun),
            Collections.singletonList(pullRunWithRevertedTranslations));
    Assert.assertEquals(0, deltas.getTranslationsPerLocale().size());
  }

  @Transactional
  @Test
  public void testGetDeltasForRunsIgnoresOldTranslations() throws Exception {
    Locale frFR = localeService.findByBcp47Tag("fr-FR");
    Repository repository =
        repositoryService.createRepository("testGetDeltasForRunsIgnoresOldTranslations");
    RepositoryLocale repoLocaleFrFR =
        repositoryService.addRepositoryLocale(repository, frFR.getBcp47Tag());
    TM tm = repository.getTm();
    Asset asset =
        assetService.createAssetWithContent(repository.getId(), "fake_for_test", "fake for test");
    Long assetId = asset.getId();

    TMTextUnit usedTextUnit =
        tmService.addTMTextUnit(tm.getId(), assetId, "used_text_unit", "active string", "Comment1");

    AssetExtraction assetExtraction = new AssetExtraction();
    assetExtraction.setAsset(asset);
    assetExtraction = assetExtractionRepository.save(assetExtraction);

    AssetTextUnit usedAssetTextUnit =
        assetExtractionService.createAssetTextUnit(
            assetExtraction,
            usedTextUnit.getName(),
            usedTextUnit.getContent(),
            usedTextUnit.getComment());

    AssetTextUnitToTMTextUnit assetTextUnitToTMTextUnit = new AssetTextUnitToTMTextUnit();
    assetTextUnitToTMTextUnit.setAssetExtraction(assetExtraction);
    assetTextUnitToTMTextUnit.setAssetTextUnit(usedAssetTextUnit);
    assetTextUnitToTMTextUnit.setTmTextUnit(usedTextUnit);
    assetTextUnitToTMTextUnitRepository.save(assetTextUnitToTMTextUnit);

    assetExtractionService.markAssetExtractionAsLastSuccessful(asset, assetExtraction);

    PushRun firstPushRun = pushRunService.createPushRun(repository);
    pushRunService.associatePushRunToTextUnitIds(
        firstPushRun, asset, Collections.singletonList(usedTextUnit.getId()));

    TMTextUnitVariant frFrUsedTuv =
        tmService.addCurrentTMTextUnitVariant(usedTextUnit.getId(), frFR.getId(), "chaîne active");

    PushRun emptyPushRun = pushRunService.createPushRun(repository);
    PullRun emptyPullRun =
        pullRunService.getOrCreate(
            "testGetDeltasForRunsIgnoresOldTranslations-emptyPullRun", repository);

    // Advance the date of the PullRun to make sure the translation is older than the PullRun
    // creation date
    emptyPullRun.setCreatedDate(emptyPullRun.getCreatedDate().plusDays(1));
    pullRunRepository.save(emptyPullRun);

    DeltaResponseDTO deltas =
        deltaService.getDeltasForRuns(
            repository,
            Collections.singletonList(frFR),
            Collections.singletonList(emptyPushRun),
            null);
    Assert.assertEquals(0, deltas.getTranslationsPerLocale().size());

    deltas =
        deltaService.getDeltasForRuns(
            repository,
            Collections.singletonList(frFR),
            Collections.singletonList(firstPushRun),
            Collections.singletonList(emptyPullRun));
    Assert.assertEquals(0, deltas.getTranslationsPerLocale().size());
  }

  @Transactional
  @Test
  public void testGetDeltasForRunsCorrectDeltaTypes() throws Exception {
    Locale koKR = localeService.findByBcp47Tag("ko-KR");
    Locale frFR = localeService.findByBcp47Tag("fr-FR");
    Repository repository =
        repositoryService.createRepository("testGetDeltasForRunsCorrectDeltaTypes");
    RepositoryLocale repoLocaleKoKR =
        repositoryService.addRepositoryLocale(repository, koKR.getBcp47Tag());
    RepositoryLocale repoLocaleFrFR =
        repositoryService.addRepositoryLocale(repository, frFR.getBcp47Tag());
    TM tm = repository.getTm();
    Asset asset =
        assetService.createAssetWithContent(repository.getId(), "fake_for_test", "fake for test");
    Long assetId = asset.getId();

    TMTextUnit usedTextUnit =
        tmService.addTMTextUnit(tm.getId(), assetId, "used_text_unit", "active string", "Comment1");

    AssetExtraction assetExtraction = new AssetExtraction();
    assetExtraction.setAsset(asset);
    assetExtraction = assetExtractionRepository.save(assetExtraction);

    AssetTextUnit usedAssetTextUnit =
        assetExtractionService.createAssetTextUnit(
            assetExtraction,
            usedTextUnit.getName(),
            usedTextUnit.getContent(),
            usedTextUnit.getComment());

    AssetTextUnitToTMTextUnit assetTextUnitToTMTextUnit = new AssetTextUnitToTMTextUnit();
    assetTextUnitToTMTextUnit.setAssetExtraction(assetExtraction);
    assetTextUnitToTMTextUnit.setAssetTextUnit(usedAssetTextUnit);
    assetTextUnitToTMTextUnit.setTmTextUnit(usedTextUnit);
    assetTextUnitToTMTextUnitRepository.save(assetTextUnitToTMTextUnit);

    assetExtractionService.markAssetExtractionAsLastSuccessful(asset, assetExtraction);

    PushRun firstPushRun = pushRunService.createPushRun(repository);
    pushRunService.associatePushRunToTextUnitIds(
        firstPushRun, asset, Collections.singletonList(usedTextUnit.getId()));

    TMTextUnitVariant frFrUsedTuv =
        tmService.addCurrentTMTextUnitVariant(usedTextUnit.getId(), frFR.getId(), "chaîne active");

    PullRun pullRunWithInitialTranslations = createPullRunForTest(repository);
    associatePullRunToTextUnitIds(
        pullRunWithInitialTranslations, asset, Collections.singletonList(frFrUsedTuv));

    TMTextUnitVariant koKrUsedTuv =
        tmService.addCurrentTMTextUnitVariant(usedTextUnit.getId(), koKR.getId(), "활성 문자열");
    TMTextUnitVariant frFrUpdatedTuv =
        tmService.addCurrentTMTextUnitVariant(
            usedTextUnit.getId(), frFR.getId(), "chaîne active 2");

    DeltaResponseDTO deltas =
        deltaService.getDeltasForRuns(
            repository,
            Arrays.asList(frFR, koKR),
            Collections.singletonList(firstPushRun),
            Collections.singletonList(pullRunWithInitialTranslations));
    Assert.assertEquals(2, deltas.getTranslationsPerLocale().size());
    Assert.assertEquals(
        DeltaType.UPDATED_TRANSLATION,
        deltas
            .getTranslationsPerLocale()
            .get(frFR.getBcp47Tag())
            .getTranslationsByTextUnitName()
            .get(usedTextUnit.getName())
            .getDeltaType());
    Assert.assertEquals(
        DeltaType.NEW_TRANSLATION,
        deltas
            .getTranslationsPerLocale()
            .get(koKR.getBcp47Tag())
            .getTranslationsByTextUnitName()
            .get(usedTextUnit.getName())
            .getDeltaType());
  }

  private DeltaLocaleDataDTO getFirstLocaleFromDeltaFile(DeltaResponseDTO deltaResponseDTO) {
    Map.Entry<String, DeltaLocaleDataDTO> stringDeltaLocaleDataDTOEntry =
        deltaResponseDTO.getTranslationsPerLocale().entrySet().stream().findFirst().orElse(null);

    return stringDeltaLocaleDataDTOEntry != null ? stringDeltaLocaleDataDTOEntry.getValue() : null;
  }

  private List<DeltaTranslationDTO> getDeltaTranslationsFromDeltaFile(
      DeltaResponseDTO deltaResponseDTO) {
    return deltaResponseDTO.getTranslationsPerLocale().values().stream()
        .map(DeltaLocaleDataDTO::getTranslationsByTextUnitName)
        .flatMap(stringDeltaTranslationDTOMap -> stringDeltaTranslationDTOMap.values().stream())
        .collect(Collectors.toList());
  }

  private PullRun createPullRunForTest(Repository repository) {
    return pullRunService.getOrCreate(UUID.randomUUID().toString(), repository);
  }

  private void associatePullRunToTextUnitIds(
      PullRun pullRun, Asset asset, List<TMTextUnitVariant> tmTextUnitVariants) {
    PullRunAsset pullRunAsset = pullRunAssetService.getOrCreate(pullRun, asset);
    tmTextUnitVariants.stream()
        .collect(
            Collectors.groupingBy(
                tmTextUnitVariant -> tmTextUnitVariant.getLocale().getId(),
                LinkedHashMap::new,
                Collectors.mapping(TMTextUnitVariant::getId, Collectors.toList())))
        .forEach(
            (localeId, perLocale) ->
                pullRunAssetService.replaceTextUnitVariants(
                    pullRunAsset, localeId, perLocale, "test"));
  }
}
