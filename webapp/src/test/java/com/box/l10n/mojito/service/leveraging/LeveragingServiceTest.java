package com.box.l10n.mojito.service.leveraging;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TM;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariantComment;
import com.box.l10n.mojito.rest.asset.AssetWithIdNotFoundException;
import com.box.l10n.mojito.rest.leveraging.CopyTmConfig;
import com.box.l10n.mojito.rest.repository.RepositoryWithIdNotFoundException;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionRepository;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import com.box.l10n.mojito.service.assetExtraction.AssetMappingService;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.repository.RepositoryLocaleCreationException;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTestData;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantCommentRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author jaurambault
 */
public class LeveragingServiceTest extends ServiceTestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(LeveragingServiceTest.class);

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Autowired LeveragingService leveragingService;

  @Autowired RepositoryService repositoryService;

  @Autowired TMTextUnitVariantRepository tmTextUnitVariantRepository;

  @Autowired TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;

  @Autowired AssetService assetService;

  @Autowired TMService tmService;

  @Autowired LocaleService localeService;

  @Autowired AssetExtractionRepository assetExtractionRepository;

  @Autowired AssetExtractionService assetExtractionService;

  @Autowired AssetMappingService assetMappingService;

  @Autowired TMTextUnitRepository tmTextUnitRepository;

  @Autowired TMTextUnitVariantCommentRepository tmTextUnitVariantCommentRepository;

  private static String[] tu(String name, String content, String comment) {
    return new String[] {name, content, comment};
  }

  private Repository createRepository(String suffix, String... localeTags)
      throws RepositoryNameAlreadyUsedException, RepositoryLocaleCreationException {
    Repository repo = repositoryService.createRepository(testIdWatcher.getEntityName(suffix));
    for (String tag : localeTags) {
      repositoryService.addRepositoryLocale(repo, tag);
    }
    return repo;
  }

  private Asset createAsset(Repository repo, String path) {
    return assetService.createAssetWithContent(repo.getId(), path, "fake for test");
  }

  private List<TMTextUnit> push(Repository repo, Asset asset, String[]... textUnits) {
    AssetExtraction extraction = new AssetExtraction();
    extraction.setAsset(asset);
    extraction = assetExtractionRepository.save(extraction);
    for (String[] tu : textUnits) {
      assetExtractionService.createAssetTextUnit(extraction, tu[0], tu[1], tu[2]);
    }
    assetMappingService.mapAssetTextUnitAndCreateTMTextUnit(
        extraction.getId(),
        repo.getTm().getId(),
        asset.getId(),
        null,
        PollableTask.INJECT_CURRENT_TASK);
    assetExtractionService.markAssetExtractionAsLastSuccessful(asset, extraction);
    return tmTextUnitRepository.findByAsset(asset);
  }

  private void translate(TMTextUnit tu, String localeTag, String translation) {
    translate(tu, localeTag, translation, TMTextUnitVariant.Status.APPROVED);
  }

  private void translate(
      TMTextUnit tu, String localeTag, String translation, TMTextUnitVariant.Status status) {
    Locale locale = localeService.findByBcp47Tag(localeTag);
    tmService.addCurrentTMTextUnitVariant(tu.getId(), locale.getId(), translation, status, true);
  }

  private void runCopyTm(CopyTmConfig config)
      throws InterruptedException,
          ExecutionException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {
    leveragingService.copyTm(config).get();
  }

  private List<TMTextUnitVariant> getTranslations(Repository repo) {
    return tmTextUnitVariantRepository
        .findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(repo, "en");
  }

  private TMTextUnitVariant getTranslation(TMTextUnit tu, String localeTag) {
    Locale locale = localeService.findByBcp47Tag(localeTag);
    TMTextUnitCurrentVariant cv =
        tmTextUnitCurrentVariantRepository.findByLocale_IdAndTmTextUnit_Id(
            locale.getId(), tu.getId());
    return cv == null ? null : cv.getTmTextUnitVariant();
  }

  private Map<String, TMTextUnitVariant> getTranslationsByLocale(Repository repo) {
    Map<String, TMTextUnitVariant> byLocale = new HashMap<>();
    for (TMTextUnitVariant v : getTranslations(repo)) {
      byLocale.put(v.getLocale().getBcp47Tag(), v);
    }
    return byLocale;
  }

  private Map<String, TMTextUnitVariant> getTranslationsByName(Repository repo, Asset asset) {
    Map<String, TMTextUnitVariant> byName = new HashMap<>();
    for (TMTextUnitVariant v : getTranslations(repo)) {
      if (v.getTmTextUnit().getAsset().getId().equals(asset.getId())) {
        byName.put(v.getTmTextUnit().getName(), v);
      }
    }
    return byName;
  }

  @Test
  public void copyAllTranslationsWithMD5MatchBetweenRepositories()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    TMTestData tmTestDataSource = new TMTestData(testIdWatcher);

    Repository sourceRepository = tmTestDataSource.repository;

    logger.debug("Create the target repository");
    Repository targetRepository =
        repositoryService.createRepository(testIdWatcher.getEntityName("targetRepository"));

    TM tm = targetRepository.getTm();

    Asset asset =
        assetService.createAssetWithContent(
            targetRepository.getId(), "fake_for_test", "fake for test");
    Long assetId = asset.getId();

    tmService.addTMTextUnit(
        tm.getId(),
        assetId,
        "zuora_error_message_verify_state_province",
        "Please enter a valid state, region or province",
        "Comment1");
    tmService.addTMTextUnit(tm.getId(), assetId, "TEST2", "Content2", "Comment2");
    tmService.addTMTextUnit(tm.getId(), assetId, "TEST3", "Content3", "Comment3");

    CopyTmConfig copyTmConfig = new CopyTmConfig();
    copyTmConfig.setSourceRepositoryId(sourceRepository.getId());
    copyTmConfig.setTargetRepositoryId(targetRepository.getId());

    leveragingService.copyTm(copyTmConfig).get();

    List<TMTextUnitVariant> sourceTranslations =
        tmTextUnitVariantRepository
            .findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(
                sourceRepository, "en");
    List<TMTextUnitVariant> targetTranslations =
        tmTextUnitVariantRepository
            .findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(
                targetRepository, "en");

    Iterator<TMTextUnitVariant> itSource = sourceTranslations.iterator();
    Iterator<TMTextUnitVariant> itTarget = targetTranslations.iterator();

    while (itTarget.hasNext()) {
      TMTextUnitVariant next = itTarget.next();
      Assert.assertEquals(
          "translation in source and target must be the same",
          itSource.next().getContent(),
          next.getContent());
    }

    Assert.assertFalse(itSource.hasNext());
  }

  @Test
  public void copyAllTranslationsWithMD5MatchBetweenRepositoriesNameRegex()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    TMTestData tmTestDataSource = new TMTestData(testIdWatcher);

    Repository sourceRepository = tmTestDataSource.repository;

    logger.debug("Create the target repository");
    Repository targetRepository =
        repositoryService.createRepository(testIdWatcher.getEntityName("targetRepository"));

    TM tm = targetRepository.getTm();

    Asset asset =
        assetService.createAssetWithContent(
            targetRepository.getId(), "fake_for_test", "fake for test");
    Long assetId = asset.getId();

    tmService.addTMTextUnit(
        tm.getId(),
        assetId,
        "zuora_error_message_verify_state_province",
        "Please enter a valid state, region or province",
        "Comment1");
    tmService.addTMTextUnit(tm.getId(), assetId, "TEST2", "Content2", "Comment2");
    tmService.addTMTextUnit(tm.getId(), assetId, "TEST3", "Content3", "Comment3");

    CopyTmConfig copyTmConfig = new CopyTmConfig();
    copyTmConfig.setSourceRepositoryId(sourceRepository.getId());
    copyTmConfig.setTargetRepositoryId(targetRepository.getId());
    copyTmConfig.setNameRegex("TEST.*");

    leveragingService.copyTm(copyTmConfig).get();

    List<TMTextUnitVariant> sourceTranslations =
        tmTextUnitVariantRepository
            .findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(
                sourceRepository, "en");
    List<TMTextUnitVariant> targetTranslations =
        tmTextUnitVariantRepository
            .findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(
                targetRepository, "en");

    Predicate<TMTextUnitVariant> filterZuora =
        new Predicate<TMTextUnitVariant>() {
          @Override
          public boolean apply(TMTextUnitVariant tmtuv) {
            return !"zuora_error_message_verify_state_province"
                .equals(tmtuv.getTmTextUnit().getName());
          }
        };

    Iterator<TMTextUnitVariant> itSource =
        Iterables.filter(sourceTranslations, filterZuora).iterator();
    Iterator<TMTextUnitVariant> itTarget = targetTranslations.iterator();

    while (itTarget.hasNext()) {
      TMTextUnitVariant next = itTarget.next();
      Assert.assertEquals(
          "translation in source and target must be the same",
          itSource.next().getContent(),
          next.getContent());
    }

    Assert.assertFalse(itSource.hasNext());
  }

  @Test
  public void copyTranslationForTmTextUnitMapping()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    TMTestData tmTestDataSource = new TMTestData(testIdWatcher);

    Repository sourceRepository = tmTestDataSource.repository;

    logger.debug("Create the target repository");
    Repository targetRepository =
        repositoryService.createRepository(testIdWatcher.getEntityName("targetRepository"));

    TM tm = targetRepository.getTm();

    Asset asset =
        assetService.createAssetWithContent(
            targetRepository.getId(), "fake_for_test", "fake for test");
    Long assetId = asset.getId();

    TMTextUnit targetTmTextUnit1 =
        tmService.addTMTextUnit(
            tm.getId(),
            assetId,
            "zuora_error_message_verify_state_province other",
            "Please enter a valid state, region or province",
            "Comment1");
    TMTextUnit targetTmTextUnit2 =
        tmService.addTMTextUnit(tm.getId(), assetId, "TEST2 other", "Content2", "Comment2");
    TMTextUnit targetTmTextUnit3 =
        tmService.addTMTextUnit(tm.getId(), assetId, "TEST3 other", "Content3", "Comment3");

    Map<Long, Long> sourceTotTargetTmTextUnitIds = new HashMap<>();
    sourceTotTargetTmTextUnitIds.put(
        tmTestDataSource.addTMTextUnit1.getId(), targetTmTextUnit1.getId());
    sourceTotTargetTmTextUnitIds.put(
        tmTestDataSource.addTMTextUnit2.getId(), targetTmTextUnit2.getId());

    CopyTmConfig copyTmConfig = new CopyTmConfig();
    copyTmConfig.setSourceRepositoryId(sourceRepository.getId());
    copyTmConfig.setTargetRepositoryId(targetRepository.getId());
    copyTmConfig.setMode(CopyTmConfig.Mode.TUIDS);
    copyTmConfig.setSourceToTargetTmTextUnitIds(sourceTotTargetTmTextUnitIds);

    leveragingService.copyTm(copyTmConfig).get();

    List<TMTextUnitVariant> sourceTranslations =
        tmTextUnitVariantRepository
            .findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(
                sourceRepository, "en");
    List<TMTextUnitVariant> targetTranslations =
        tmTextUnitVariantRepository
            .findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(
                targetRepository, "en");

    Assert.assertEquals(5, sourceTranslations.size());
    Assert.assertEquals(3, targetTranslations.size());

    Assert.assertEquals("Content2 fr-CA", targetTranslations.get(0).getContent());
    Assert.assertEquals(
        targetTmTextUnit2.getId(), targetTranslations.get(0).getTmTextUnit().getId());

    Assert.assertEquals(
        "Veuillez indiquer un état, une région ou une province valide.",
        targetTranslations.get(1).getContent());
    Assert.assertEquals(
        targetTmTextUnit1.getId(), targetTranslations.get(1).getTmTextUnit().getId());

    Assert.assertEquals("올바른 국가, 지역 또는 시/도를 입력하십시오.", targetTranslations.get(2).getContent());
    Assert.assertEquals(
        targetTmTextUnit1.getId(), targetTranslations.get(2).getTmTextUnit().getId());
  }

  @Test
  public void copyAllTranslationsWithNameMatchBetweenRepositories()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    TMTestData tmTestDataSource = new TMTestData(testIdWatcher);

    Repository sourceRepository = tmTestDataSource.repository;

    logger.debug("Create the target repository");
    Repository targetRepository =
        repositoryService.createRepository(testIdWatcher.getEntityName("targetRepository"));

    TM tm = targetRepository.getTm();

    Asset asset =
        assetService.createAssetWithContent(
            targetRepository.getId(), "fake_for_test", "fake for test");
    Long assetId = asset.getId();

    tmService.addTMTextUnit(
        tm.getId(),
        assetId,
        "zuora_error_message_verify_state_province",
        "Different source content",
        "DifferentComment");
    tmService.addTMTextUnit(
        tm.getId(), assetId, "TEST2", "Different Content2", "DifferentComment2");
    tmService.addTMTextUnit(tm.getId(), assetId, "TEST3", "Content3", "Comment3");

    CopyTmConfig copyTmConfig = new CopyTmConfig();
    copyTmConfig.setSourceRepositoryId(sourceRepository.getId());
    copyTmConfig.setTargetRepositoryId(targetRepository.getId());
    copyTmConfig.setMode(CopyTmConfig.Mode.NAME);

    leveragingService.copyTm(copyTmConfig).get();

    List<TMTextUnitVariant> sourceTranslations =
        tmTextUnitVariantRepository
            .findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(
                sourceRepository, "en");
    List<TMTextUnitVariant> targetTranslations =
        tmTextUnitVariantRepository
            .findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(
                targetRepository, "en");

    Iterator<TMTextUnitVariant> itSource = sourceTranslations.iterator();
    Iterator<TMTextUnitVariant> itTarget = targetTranslations.iterator();

    while (itTarget.hasNext()) {
      TMTextUnitVariant next = itTarget.next();
      Assert.assertEquals(
          "translation in source and target must be the same",
          itSource.next().getContent(),
          next.getContent());
    }

    Assert.assertFalse(itSource.hasNext());
  }

  @Test
  public void copyWithNameMatchPreservesStatusForAnyMatch()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    TMTestData tmTestDataSource = new TMTestData(testIdWatcher);

    Repository sourceRepository = tmTestDataSource.repository;

    logger.debug("Create the target repository");
    Repository targetRepository =
        repositoryService.createRepository(testIdWatcher.getEntityName("targetRepository"));

    TM tm = targetRepository.getTm();

    Asset asset =
        assetService.createAssetWithContent(
            targetRepository.getId(), "fake_for_test", "fake for test");
    Long assetId = asset.getId();

    tmService.addTMTextUnit(
        tm.getId(),
        assetId,
        "zuora_error_message_verify_state_province",
        "Different source content",
        "DifferentComment");

    CopyTmConfig copyTmConfig = new CopyTmConfig();
    copyTmConfig.setSourceRepositoryId(sourceRepository.getId());
    copyTmConfig.setTargetRepositoryId(targetRepository.getId());
    copyTmConfig.setMode(CopyTmConfig.Mode.NAME);
    copyTmConfig.setPreserveStatusMode(CopyTmConfig.PreserveStatusMode.ALL);

    leveragingService.copyTm(copyTmConfig).get();

    List<TMTextUnitVariant> targetTranslations =
        tmTextUnitVariantRepository
            .findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(
                targetRepository, "en");

    Assert.assertFalse("Should have copied translations", targetTranslations.isEmpty());
    for (TMTextUnitVariant variant : targetTranslations) {
      Assert.assertEquals(
          "Status should be preserved as APPROVED with ALL",
          TMTextUnitVariant.Status.APPROVED,
          variant.getStatus());
    }
  }

  @Test
  public void copyWithNameMatchPreservesStatusForUniqueMatchOnly()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    TMTestData tmTestDataSource = new TMTestData(testIdWatcher);

    Repository sourceRepository = tmTestDataSource.repository;

    logger.debug("Create the target repository");
    Repository targetRepository =
        repositoryService.createRepository(testIdWatcher.getEntityName("targetRepository"));

    TM tm = targetRepository.getTm();

    Asset asset =
        assetService.createAssetWithContent(
            targetRepository.getId(), "fake_for_test", "fake for test");
    Long assetId = asset.getId();

    tmService.addTMTextUnit(
        tm.getId(),
        assetId,
        "zuora_error_message_verify_state_province",
        "Different source content",
        "DifferentComment");

    CopyTmConfig copyTmConfig = new CopyTmConfig();
    copyTmConfig.setSourceRepositoryId(sourceRepository.getId());
    copyTmConfig.setTargetRepositoryId(targetRepository.getId());
    copyTmConfig.setMode(CopyTmConfig.Mode.NAME);
    copyTmConfig.setPreserveStatusMode(CopyTmConfig.PreserveStatusMode.UNIQUE);

    leveragingService.copyTm(copyTmConfig).get();

    List<TMTextUnitVariant> targetTranslations =
        tmTextUnitVariantRepository
            .findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(
                targetRepository, "en");

    Assert.assertFalse("Should have copied translations", targetTranslations.isEmpty());
    for (TMTextUnitVariant variant : targetTranslations) {
      Assert.assertEquals(
          "Status should be preserved as APPROVED for unambiguous match with UNIQUE mode",
          TMTextUnitVariant.Status.APPROVED,
          variant.getStatus());
    }
  }

  @Test
  public void copyWithNameMatchSetsTranslationNeededWithoutPreserveStatus()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    TMTestData tmTestDataSource = new TMTestData(testIdWatcher);

    Repository sourceRepository = tmTestDataSource.repository;

    logger.debug("Create the target repository");
    Repository targetRepository =
        repositoryService.createRepository(testIdWatcher.getEntityName("targetRepository"));

    TM tm = targetRepository.getTm();

    Asset asset =
        assetService.createAssetWithContent(
            targetRepository.getId(), "fake_for_test", "fake for test");
    Long assetId = asset.getId();

    tmService.addTMTextUnit(
        tm.getId(),
        assetId,
        "zuora_error_message_verify_state_province",
        "Different source content",
        "DifferentComment");

    CopyTmConfig copyTmConfig = new CopyTmConfig();
    copyTmConfig.setSourceRepositoryId(sourceRepository.getId());
    copyTmConfig.setTargetRepositoryId(targetRepository.getId());
    copyTmConfig.setMode(CopyTmConfig.Mode.NAME);

    leveragingService.copyTm(copyTmConfig).get();

    List<TMTextUnitVariant> targetTranslations =
        tmTextUnitVariantRepository
            .findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(
                targetRepository, "en");

    Assert.assertFalse("Should have copied translations", targetTranslations.isEmpty());
    for (TMTextUnitVariant variant : targetTranslations) {
      Assert.assertEquals(
          "Status should be TRANSLATION_NEEDED with PRECISION mode for NAME leveraging",
          TMTextUnitVariant.Status.TRANSLATION_NEEDED,
          variant.getStatus());
    }
  }

  @Test
  public void copyTranslationsWithNameMatchDoesNotMatchDifferentNames()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    TMTestData tmTestDataSource = new TMTestData(testIdWatcher);

    Repository sourceRepository = tmTestDataSource.repository;

    logger.debug("Create the target repository");
    Repository targetRepository =
        repositoryService.createRepository(testIdWatcher.getEntityName("targetRepository"));

    TM tm = targetRepository.getTm();

    Asset asset =
        assetService.createAssetWithContent(
            targetRepository.getId(), "fake_for_test", "fake for test");
    Long assetId = asset.getId();

    tmService.addTMTextUnit(
        tm.getId(),
        assetId,
        "completely_different_name",
        "Please enter a valid state, region or province",
        "Comment1");
    tmService.addTMTextUnit(tm.getId(), assetId, "another_different_name", "Content2", "Comment2");

    CopyTmConfig copyTmConfig = new CopyTmConfig();
    copyTmConfig.setSourceRepositoryId(sourceRepository.getId());
    copyTmConfig.setTargetRepositoryId(targetRepository.getId());
    copyTmConfig.setMode(CopyTmConfig.Mode.NAME);

    leveragingService.copyTm(copyTmConfig).get();

    List<TMTextUnitVariant> targetTranslations =
        tmTextUnitVariantRepository
            .findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(
                targetRepository, "en");

    Assert.assertEquals(
        "No translations should be copied when names don't match", 0, targetTranslations.size());
  }

  @Test
  public void copyAllTranslationsWithExactMatchBetweenRepositories()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    TMTestData tmTestDataSource = new TMTestData(testIdWatcher);

    Repository sourceRepository = tmTestDataSource.repository;

    logger.debug("Create the target repository");
    Repository targetRepository =
        repositoryService.createRepository(testIdWatcher.getEntityName("targetRepository"));

    TM tm = targetRepository.getTm();

    Asset asset =
        assetService.createAssetWithContent(
            targetRepository.getId(), "fake_for_test", "fake for test");
    Long assetId = asset.getId();

    tmService.addTMTextUnit(
        tm.getId(),
        assetId,
        "zuora_error_message_verify_state_province_update",
        "Please enter a valid state, region or province",
        "Comment1");
    tmService.addTMTextUnit(tm.getId(), assetId, "TEST2", "Content2", "Comment2");
    tmService.addTMTextUnit(tm.getId(), assetId, "TEST3", "Content3", "Comment3");

    CopyTmConfig copyTmConfig = new CopyTmConfig();
    copyTmConfig.setSourceRepositoryId(sourceRepository.getId());
    copyTmConfig.setTargetRepositoryId(targetRepository.getId());
    copyTmConfig.setMode(CopyTmConfig.Mode.EXACT);

    leveragingService.copyTm(copyTmConfig).get();

    List<TMTextUnitVariant> sourceTranslations =
        tmTextUnitVariantRepository
            .findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(
                sourceRepository, "en");
    List<TMTextUnitVariant> targetTranslations =
        tmTextUnitVariantRepository
            .findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(
                targetRepository, "en");

    Iterator<TMTextUnitVariant> itSource = sourceTranslations.iterator();
    Iterator<TMTextUnitVariant> itTarget = targetTranslations.iterator();

    while (itTarget.hasNext()) {
      TMTextUnitVariant next = itTarget.next();
      Assert.assertEquals(
          "translation in source and target must be the same",
          itSource.next().getContent(),
          next.getContent());
    }

    Assert.assertFalse(itSource.hasNext());
  }

  @Test
  public void checkCommentsAreNotCopiedIfTmTextUnitCurrentVariantNotChanged()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    TMTestData tmTestDataSource = new TMTestData(testIdWatcher);

    Repository sourceRepository = tmTestDataSource.repository;

    logger.debug("Create the target repository");
    Repository targetRepository =
        repositoryService.createRepository(testIdWatcher.getEntityName("targetRepository"));

    TM tm = targetRepository.getTm();

    Asset asset =
        assetService.createAssetWithContent(
            targetRepository.getId(), "fake_for_test", "fake for test");
    Long assetId = asset.getId();

    tmService.addTMTextUnit(
        tm.getId(),
        assetId,
        "zuora_error_message_verify_state_province_update",
        "Please enter a valid state, region or province",
        "Comment1");
    tmService.addTMTextUnit(tm.getId(), assetId, "TEST2", "Content2", "Comment2");
    tmService.addTMTextUnit(tm.getId(), assetId, "TEST3", "Content3", "Comment3");

    CopyTmConfig copyTmConfig = new CopyTmConfig();
    copyTmConfig.setSourceRepositoryId(sourceRepository.getId());
    copyTmConfig.setTargetRepositoryId(targetRepository.getId());

    leveragingService.copyTm(copyTmConfig).get();
    leveragingService.copyTm(copyTmConfig).get();

    List<TMTextUnitVariant> targetTranslations =
        tmTextUnitVariantRepository
            .findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(
                targetRepository, "en");
    for (TMTextUnitVariant targetTranslation : targetTranslations) {
      Assert.assertEquals(1, targetTranslation.getTmTextUnitVariantComments().size());
    }
  }

  @Test
  public void copyBetweenAssets()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    Locale frFR = localeService.findByBcp47Tag("fr-FR");

    logger.debug("Create the source repository");
    Repository sourceRepository =
        repositoryService.createRepository(testIdWatcher.getEntityName("sourceRepository"));
    repositoryService.addRepositoryLocale(sourceRepository, frFR.getBcp47Tag());

    Asset sourceAsset =
        assetService.createAssetWithContent(
            sourceRepository.getId(), "fake_for_test_1", "fake for test");
    Long sourceAssetId = sourceAsset.getId();

    TMTextUnit addTMTextUnit =
        tmService.addTMTextUnit(
            sourceRepository.getTm().getId(), sourceAssetId, "TEST3", "Content3", "Comment3");
    tmService.addCurrentTMTextUnitVariant(
        addTMTextUnit.getId(), frFR.getId(), "Content3 fr-FR from source");

    Asset sourceAsset2 =
        assetService.createAssetWithContent(
            sourceRepository.getId(), "fake_for_test2", "fake for test");
    Long sourceAssetId2 = sourceAsset2.getId();

    TMTextUnit addTMTextUnit2 =
        tmService.addTMTextUnit(
            sourceRepository.getTm().getId(), sourceAssetId2, "TEST3", "Content3", "Comment3");
    tmService.addCurrentTMTextUnitVariant(
        addTMTextUnit2.getId(), frFR.getId(), "Content3 fr-FR from source2");

    logger.debug("Create the target repository");
    Repository targetRepository =
        repositoryService.createRepository(testIdWatcher.getEntityName("targetRepository"));

    Asset targetAsset =
        assetService.createAssetWithContent(
            targetRepository.getId(), "fake_for_test", "fake for test");
    Long targetAssetId = targetAsset.getId();

    tmService.addTMTextUnit(
        targetRepository.getTm().getId(), targetAssetId, "TEST3", "Content3", "Comment3");

    Asset targetAsset2 =
        assetService.createAssetWithContent(
            targetRepository.getId(), "fake_for_test2", "fake for test");
    Long targetAssetId2 = targetAsset2.getId();

    tmService.addTMTextUnit(
        targetRepository.getTm().getId(), targetAssetId2, "TEST3", "Content3", "Comment3");

    CopyTmConfig copyTmConfig = new CopyTmConfig();
    copyTmConfig.setSourceAssetId(sourceAssetId);
    copyTmConfig.setTargetAssetId(targetAssetId);
    copyTmConfig.setMode(CopyTmConfig.Mode.MD5);

    leveragingService.copyTm(copyTmConfig).get();

    List<TMTextUnitVariant> targetTranslations =
        tmTextUnitVariantRepository
            .findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(
                targetRepository, "en");
    Assert.assertEquals("Content3 fr-FR from source", targetTranslations.get(0).getContent());
    Assert.assertEquals(
        targetAsset.getId(), targetTranslations.get(0).getTmTextUnit().getAsset().getId());

    Assert.assertEquals(1, targetTranslations.size());

    CopyTmConfig copyTmConfig2 = new CopyTmConfig();
    copyTmConfig2.setSourceAssetId(sourceAssetId2);
    copyTmConfig2.setTargetAssetId(targetAssetId2);
    copyTmConfig2.setMode(CopyTmConfig.Mode.MD5);

    leveragingService.copyTm(copyTmConfig2).get();

    List<TMTextUnitVariant> targetTranslations2 =
        tmTextUnitVariantRepository
            .findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(
                targetRepository, "en");
    Assert.assertEquals("Content3 fr-FR from source", targetTranslations2.get(0).getContent());
    Assert.assertEquals(
        targetAsset.getId(), targetTranslations2.get(0).getTmTextUnit().getAsset().getId());

    Assert.assertEquals("Content3 fr-FR from source2", targetTranslations2.get(1).getContent());
    Assert.assertEquals(
        targetAsset2.getId(), targetTranslations2.get(1).getTmTextUnit().getAsset().getId());

    Assert.assertEquals(2, targetTranslations2.size());
  }

  @Test
  public void noneModeSkipsApprovedLocaleAndLeveragesOthers()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    Repository sourceRepo = createRepository("source", "ko-KR", "fr-FR");
    Asset sourceAsset = createAsset(sourceRepo, "fake_for_test");
    TMTextUnit sourceTu =
        push(sourceRepo, sourceAsset, tu("greeting", "Hello", "A greeting")).get(0);
    translate(sourceTu, "ko-KR", "안녕하세요");
    translate(sourceTu, "fr-FR", "Bonjour");

    Repository targetRepo = createRepository("target", "ko-KR", "fr-FR");
    Asset targetAsset = createAsset(targetRepo, "fake_for_test");
    TMTextUnit targetTu =
        push(targetRepo, targetAsset, tu("greeting", "Hello", "A greeting")).get(0);
    translate(targetTu, "ko-KR", "existing ko-KR approved");

    CopyTmConfig config = new CopyTmConfig();
    config.setSourceRepositoryId(sourceRepo.getId());
    config.setTargetRepositoryId(targetRepo.getId());
    config.setMode(CopyTmConfig.Mode.MD5);
    config.setOverwriteMode(CopyTmConfig.OverwriteMode.NONE);
    runCopyTm(config);

    Map<String, TMTextUnitVariant> byLocale = getTranslationsByLocale(targetRepo);
    Assert.assertEquals(2, byLocale.size());

    Assert.assertEquals(
        "ko-KR should keep existing approved translation",
        "existing ko-KR approved",
        byLocale.get("ko-KR").getContent());
    Assert.assertEquals(TMTextUnitVariant.Status.APPROVED, byLocale.get("ko-KR").getStatus());

    Assert.assertEquals(
        "fr-FR should be leveraged since it had no translation",
        "Bonjour",
        byLocale.get("fr-FR").getContent());
  }

  @Test
  public void noneModeSkipsTranslationNeededLocale()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    Repository sourceRepo = createRepository("source", "fr-FR");
    Asset sourceAsset = createAsset(sourceRepo, "fake_for_test");
    TMTextUnit sourceTu =
        push(sourceRepo, sourceAsset, tu("greeting", "Hello", "A greeting")).get(0);
    translate(sourceTu, "fr-FR", "Bonjour");

    Repository targetRepo = createRepository("target", "fr-FR");
    Asset targetAsset = createAsset(targetRepo, "fake_for_test");
    TMTextUnit targetTu =
        push(targetRepo, targetAsset, tu("greeting", "Hello", "A greeting")).get(0);
    translate(
        targetTu,
        "fr-FR",
        "existing translation needed",
        TMTextUnitVariant.Status.TRANSLATION_NEEDED);

    CopyTmConfig config = new CopyTmConfig();
    config.setSourceRepositoryId(sourceRepo.getId());
    config.setTargetRepositoryId(targetRepo.getId());
    config.setMode(CopyTmConfig.Mode.MD5);
    config.setOverwriteMode(CopyTmConfig.OverwriteMode.NONE);
    runCopyTm(config);

    List<TMTextUnitVariant> translations = getTranslations(targetRepo);
    Assert.assertEquals(1, translations.size());
    Assert.assertEquals(
        "Should keep existing TRANSLATION_NEEDED translation",
        "existing translation needed",
        translations.get(0).getContent());
    Assert.assertEquals(
        TMTextUnitVariant.Status.TRANSLATION_NEEDED, translations.get(0).getStatus());
  }

  @Test
  public void forTranslationModeOverwritesTranslationNeededAndLeveragesUntranslated()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    Repository sourceRepo = createRepository("source", "fr-FR", "ko-KR", "ja-JP");
    Asset sourceAsset = createAsset(sourceRepo, "fake_for_test");
    TMTextUnit sourceTu =
        push(sourceRepo, sourceAsset, tu("greeting", "Hello", "A greeting")).get(0);
    translate(sourceTu, "fr-FR", "Bonjour");
    translate(sourceTu, "ko-KR", "안녕하세요");
    translate(sourceTu, "ja-JP", "こんにちは");

    Repository targetRepo = createRepository("target", "fr-FR", "ko-KR", "ja-JP");
    Asset targetAsset = createAsset(targetRepo, "fake_for_test");
    TMTextUnit targetTu =
        push(targetRepo, targetAsset, tu("greeting", "Hello", "A greeting")).get(0);
    translate(
        targetTu,
        "fr-FR",
        "existing translation needed",
        TMTextUnitVariant.Status.TRANSLATION_NEEDED);
    translate(targetTu, "ko-KR", "existing approved ko");

    CopyTmConfig config = new CopyTmConfig();
    config.setSourceRepositoryId(sourceRepo.getId());
    config.setTargetRepositoryId(targetRepo.getId());
    config.setMode(CopyTmConfig.Mode.MD5);
    config.setPreserveStatusMode(CopyTmConfig.PreserveStatusMode.ALL);
    config.setOverwriteMode(CopyTmConfig.OverwriteMode.FOR_TRANSLATION);
    runCopyTm(config);

    TMTextUnitVariant frTranslation = getTranslation(targetTu, "fr-FR");
    TMTextUnitVariant koTranslation = getTranslation(targetTu, "ko-KR");
    TMTextUnitVariant jaTranslation = getTranslation(targetTu, "ja-JP");

    Assert.assertEquals(
        "fr-FR should be overwritten because current status is TRANSLATION_NEEDED",
        "Bonjour",
        frTranslation.getContent());
    Assert.assertEquals(
        "ko-KR should NOT be overwritten because current status is APPROVED",
        "existing approved ko",
        koTranslation.getContent());
    Assert.assertNotNull("ja-JP should be leveraged since it had no translation", jaTranslation);
    Assert.assertEquals("こんにちは", jaTranslation.getContent());
  }

  @Test
  public void higherStatusModeOverwritesLowerStatus()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    Repository sourceRepo = createRepository("source", "fr-FR", "ko-KR");
    Asset sourceAsset = createAsset(sourceRepo, "fake_for_test");
    TMTextUnit sourceTu =
        push(sourceRepo, sourceAsset, tu("greeting", "Hello", "A greeting")).get(0);
    translate(sourceTu, "fr-FR", "Bonjour approved");
    translate(sourceTu, "ko-KR", "안녕 approved");

    Repository targetRepo = createRepository("target", "fr-FR", "ko-KR");
    Asset targetAsset = createAsset(targetRepo, "fake_for_test");
    TMTextUnit targetTu =
        push(targetRepo, targetAsset, tu("greeting", "Hello", "A greeting")).get(0);
    translate(
        targetTu,
        "fr-FR",
        "existing translation needed",
        TMTextUnitVariant.Status.TRANSLATION_NEEDED);
    translate(targetTu, "ko-KR", "existing approved ko");

    CopyTmConfig config = new CopyTmConfig();
    config.setSourceRepositoryId(sourceRepo.getId());
    config.setTargetRepositoryId(targetRepo.getId());
    config.setMode(CopyTmConfig.Mode.MD5);
    config.setPreserveStatusMode(CopyTmConfig.PreserveStatusMode.ALL);
    config.setOverwriteMode(CopyTmConfig.OverwriteMode.HIGHER_STATUS);
    runCopyTm(config);

    TMTextUnitVariant frTranslation = getTranslation(targetTu, "fr-FR");
    TMTextUnitVariant koTranslation = getTranslation(targetTu, "ko-KR");

    Assert.assertEquals(
        "fr-FR should be overwritten because APPROVED > TRANSLATION_NEEDED",
        "Bonjour approved",
        frTranslation.getContent());
    Assert.assertEquals(
        "ko-KR should NOT be overwritten because APPROVED is not > APPROVED",
        "existing approved ko",
        koTranslation.getContent());
  }

  @Test
  public void higherStatusModeSkipsWhenCandidateStatusIsLower()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    Repository sourceRepo = createRepository("source", "fr-FR");
    Asset sourceAsset = createAsset(sourceRepo, "fake_for_test");
    TMTextUnit sourceTu =
        push(sourceRepo, sourceAsset, tu("greeting", "Hello", "A greeting")).get(0);
    translate(sourceTu, "fr-FR", "Bonjour review", TMTextUnitVariant.Status.REVIEW_NEEDED);

    Repository targetRepo = createRepository("target", "fr-FR");
    Asset targetAsset = createAsset(targetRepo, "fake_for_test");
    TMTextUnit targetTu =
        push(targetRepo, targetAsset, tu("greeting", "Hello", "A greeting")).get(0);
    translate(targetTu, "fr-FR", "existing approved");

    CopyTmConfig config = new CopyTmConfig();
    config.setSourceRepositoryId(sourceRepo.getId());
    config.setTargetRepositoryId(targetRepo.getId());
    config.setMode(CopyTmConfig.Mode.MD5);
    config.setPreserveStatusMode(CopyTmConfig.PreserveStatusMode.ALL);
    config.setOverwriteMode(CopyTmConfig.OverwriteMode.HIGHER_STATUS);
    runCopyTm(config);

    List<TMTextUnitVariant> translations = getTranslations(targetRepo);
    Assert.assertEquals(1, translations.size());
    Assert.assertEquals(
        "Should keep existing APPROVED because REVIEW_NEEDED is not higher",
        "existing approved",
        translations.get(0).getContent());
    Assert.assertEquals(TMTextUnitVariant.Status.APPROVED, translations.get(0).getStatus());
  }

  @Test
  public void higherOrEqualStatusModeOverwritesEqualStatus()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    Repository sourceRepo = createRepository("source", "fr-FR", "ko-KR");
    Asset sourceAsset = createAsset(sourceRepo, "fake_for_test");
    TMTextUnit sourceTu =
        push(sourceRepo, sourceAsset, tu("greeting", "Hello", "A greeting")).get(0);
    translate(sourceTu, "fr-FR", "Bonjour review", TMTextUnitVariant.Status.REVIEW_NEEDED);
    translate(sourceTu, "ko-KR", "안녕 review", TMTextUnitVariant.Status.REVIEW_NEEDED);

    Repository targetRepo = createRepository("target", "fr-FR", "ko-KR");
    Asset targetAsset = createAsset(targetRepo, "fake_for_test");
    TMTextUnit targetTu =
        push(targetRepo, targetAsset, tu("greeting", "Hello", "A greeting")).get(0);
    translate(targetTu, "fr-FR", "existing review fr", TMTextUnitVariant.Status.REVIEW_NEEDED);
    translate(targetTu, "ko-KR", "existing approved ko");

    CopyTmConfig config = new CopyTmConfig();
    config.setSourceRepositoryId(sourceRepo.getId());
    config.setTargetRepositoryId(targetRepo.getId());
    config.setMode(CopyTmConfig.Mode.MD5);
    config.setPreserveStatusMode(CopyTmConfig.PreserveStatusMode.ALL);
    config.setOverwriteMode(CopyTmConfig.OverwriteMode.HIGHER_OR_EQUAL_STATUS);
    runCopyTm(config);

    TMTextUnitVariant frTranslation = getTranslation(targetTu, "fr-FR");
    TMTextUnitVariant koTranslation = getTranslation(targetTu, "ko-KR");

    Assert.assertEquals(
        "fr-FR should be overwritten because REVIEW_NEEDED == REVIEW_NEEDED",
        "Bonjour review",
        frTranslation.getContent());
    Assert.assertEquals(
        "ko-KR should NOT be overwritten because REVIEW_NEEDED < APPROVED",
        "existing approved ko",
        koTranslation.getContent());
  }

  @Test
  public void allModeOverwritesEverything()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    Repository sourceRepo = createRepository("source", "fr-FR");
    Asset sourceAsset = createAsset(sourceRepo, "fake_for_test");
    TMTextUnit sourceTu =
        push(sourceRepo, sourceAsset, tu("greeting", "Hello", "A greeting")).get(0);
    translate(
        sourceTu,
        "fr-FR",
        "Bonjour translation needed",
        TMTextUnitVariant.Status.TRANSLATION_NEEDED);

    Repository targetRepo = createRepository("target", "fr-FR");
    Asset targetAsset = createAsset(targetRepo, "fake_for_test");
    TMTextUnit targetTu =
        push(targetRepo, targetAsset, tu("greeting", "Hello", "A greeting")).get(0);
    translate(targetTu, "fr-FR", "existing approved");

    CopyTmConfig config = new CopyTmConfig();
    config.setSourceRepositoryId(sourceRepo.getId());
    config.setTargetRepositoryId(targetRepo.getId());
    config.setMode(CopyTmConfig.Mode.MD5);
    config.setPreserveStatusMode(CopyTmConfig.PreserveStatusMode.ALL);
    config.setOverwriteMode(CopyTmConfig.OverwriteMode.ALL);
    runCopyTm(config);

    TMTextUnitVariant frTranslation = getTranslation(targetTu, "fr-FR");

    Assert.assertNotNull("fr-FR should have a translation", frTranslation);
    Assert.assertEquals(
        "Should overwrite even though TRANSLATION_NEEDED < APPROVED",
        "Bonjour translation needed",
        frTranslation.getContent());
  }

  @Test
  public void higherStatusModeUsesCandidateStatusNotEffectiveStatus()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    // Source repo has greeting="Hello" with APPROVED translation "Bonjour approved"
    Repository sourceRepo = createRepository("source", "fr-FR");
    Asset sourceAsset = createAsset(sourceRepo, "fake_for_test");
    TMTextUnit sourceTu =
        push(sourceRepo, sourceAsset, tu("greeting", "Hello", "A greeting")).get(0);
    translate(sourceTu, "fr-FR", "Bonjour approved");

    // Target repo has the same name "greeting" but different content and comment,
    // so it's a name-only match (not MD5). The existing translation is REVIEW_NEEDED.
    Repository targetRepo = createRepository("target", "fr-FR");
    Asset targetAsset = createAsset(targetRepo, "fake_for_test");
    TMTextUnit targetTu =
        push(targetRepo, targetAsset, tu("greeting", "Hello updated", "A greeting updated")).get(0);
    translate(targetTu, "fr-FR", "existing review needed", TMTextUnitVariant.Status.REVIEW_NEEDED);

    // Leverage by name with PRECISION + HIGHER_STATUS. The candidate's original status
    // (APPROVED) is higher than the target's existing status (REVIEW_NEEDED), so the
    // overwrite should happen. But since it's a name-only match, PRECISION mode downgrades
    // the effective status to TRANSLATION_NEEDED.
    CopyTmConfig config = new CopyTmConfig();
    config.setSourceRepositoryId(sourceRepo.getId());
    config.setTargetRepositoryId(targetRepo.getId());
    config.setMode(CopyTmConfig.Mode.NAME);
    config.setPreserveStatusMode(CopyTmConfig.PreserveStatusMode.PRECISION);
    config.setOverwriteMode(CopyTmConfig.OverwriteMode.HIGHER_STATUS);
    runCopyTm(config);

    List<TMTextUnitVariant> translations = getTranslations(targetRepo);
    Assert.assertEquals(2, translations.size());
    TMTextUnitVariant leveraged =
        translations.stream()
            .filter(v -> v.getContent().equals("Bonjour approved"))
            .findFirst()
            .orElse(null);
    Assert.assertNotNull(
        "Should overwrite: candidate's original status is APPROVED which is higher than "
            + "existing REVIEW_NEEDED (effective status downgrade does not affect the decision)",
        leveraged);
    Assert.assertEquals(
        "Name-only match with PRECISION mode downgrades to TRANSLATION_NEEDED",
        TMTextUnitVariant.Status.TRANSLATION_NEEDED,
        leveraged.getStatus());
  }

  @Test
  public void higherStatusModeAllowsLeveragingIntoUntranslatedLocale()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    Repository sourceRepo = createRepository("source", "fr-FR");
    Asset sourceAsset = createAsset(sourceRepo, "fake_for_test");
    TMTextUnit sourceTu =
        push(sourceRepo, sourceAsset, tu("greeting", "Hello", "A greeting")).get(0);
    translate(sourceTu, "fr-FR", "Bonjour");

    Repository targetRepo = createRepository("target", "fr-FR");
    Asset targetAsset = createAsset(targetRepo, "fake_for_test");
    push(targetRepo, targetAsset, tu("greeting", "Hello", "A greeting"));

    CopyTmConfig config = new CopyTmConfig();
    config.setSourceRepositoryId(sourceRepo.getId());
    config.setTargetRepositoryId(targetRepo.getId());
    config.setMode(CopyTmConfig.Mode.MD5);
    config.setPreserveStatusMode(CopyTmConfig.PreserveStatusMode.ALL);
    config.setOverwriteMode(CopyTmConfig.OverwriteMode.HIGHER_STATUS);
    runCopyTm(config);

    List<TMTextUnitVariant> translations = getTranslations(targetRepo);
    Assert.assertEquals(1, translations.size());
    Assert.assertEquals("Bonjour", translations.get(0).getContent());
    Assert.assertEquals(TMTextUnitVariant.Status.APPROVED, translations.get(0).getStatus());
  }

  @Test
  public void copyByNamePrefersHigherPrecisionEvenWhenAllUnused()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    Repository repo = createRepository("repo", "fr-FR");
    Asset sourceAsset = createAsset(repo, "en-US.properties");

    // Push #1: en-US.properties contains 3 strings:
    //   some.id.one=SOURCE ONE          #comment one
    //   some.id.two=SOURCE TWO          #comment two
    //   some.id.three=SOURCE THREE      #comment three
    push(
        repo,
        sourceAsset,
        tu("some.id.one", "SOURCE ONE", "comment one"),
        tu("some.id.two", "SOURCE TWO", "comment two"),
        tu("some.id.three", "SOURCE THREE", "comment three"));

    // Translate all 3 strings: TRANSLATION ONE, TRANSLATION TWO, TRANSLATION THREE
    Map<String, String> v1Translations = new HashMap<>();
    v1Translations.put("some.id.one", "TRANSLATION ONE");
    v1Translations.put("some.id.two", "TRANSLATION TWO");
    v1Translations.put("some.id.three", "TRANSLATION THREE");
    for (TMTextUnit tu : tmTextUnitRepository.findByAsset(sourceAsset)) {
      translate(tu, "fr-FR", v1Translations.get(tu.getName()));
    }

    // Push #2: content changed for some.id.two and some.id.three:
    //   some.id.one=SOURCE ONE          #comment one          (unchanged)
    //   some.id.two=NEW SOURCE TWO      #comment two          (content changed → old TU becomes
    // UNUSED)
    //   some.id.three=NEW SOURCE THREE  #comment three        (content changed → old TU becomes
    // UNUSED)
    push(
        repo,
        sourceAsset,
        tu("some.id.one", "SOURCE ONE", "comment one"),
        tu("some.id.two", "NEW SOURCE TWO", "comment two"),
        tu("some.id.three", "NEW SOURCE THREE", "comment three"));

    // Translate the new TUs created by push #2: NEW TRANSLATION TWO, NEW TRANSLATION THREE.
    // Source leveraging during push already copied v1 translations onto the new TUs,
    // so we overwrite with the correct v2 translations by matching on content.
    Map<String, String> v2Translations = new HashMap<>();
    v2Translations.put("NEW SOURCE TWO", "NEW TRANSLATION TWO");
    v2Translations.put("NEW SOURCE THREE", "NEW TRANSLATION THREE");
    for (TMTextUnit tu : tmTextUnitRepository.findByAsset(sourceAsset)) {
      String v2 = v2Translations.get(tu.getContent());
      if (v2 != null) {
        translate(tu, "fr-FR", v2);
      }
    }

    // Push #3: asset type migration. Empty push on en-US.properties makes ALL its TUs UNUSED.
    push(repo, sourceAsset);

    // New asset (project.xliff) pushed in the SAME repo:
    //   some.id.one=SOURCE ONE            #comment one       → exactly as in latest
    // en-US.properties
    //   some.id.two=NEW SOURCE TWO        #comment two       → exactly as in latest
    // en-US.properties
    //   some.id.three=XLIFF SOURCE THREE  #comment three     → content differs from latest
    // en-US.properties
    Asset targetAsset = createAsset(repo, "project.xliff");
    push(
        repo,
        targetAsset,
        tu("some.id.one", "SOURCE ONE", "comment one"),
        tu("some.id.two", "NEW SOURCE TWO", "comment two"),
        tu("some.id.three", "XLIFF SOURCE THREE", "comment three"));

    // Leverage by name within the same repo (source asset → target asset)
    CopyTmConfig config = new CopyTmConfig();
    config.setSourceRepositoryId(repo.getId());
    config.setTargetRepositoryId(repo.getId());
    config.setTargetAssetId(targetAsset.getId());
    config.setMode(CopyTmConfig.Mode.NAME);
    runCopyTm(config);

    Map<String, TMTextUnitVariant> byName = getTranslationsByName(repo, targetAsset);
    Assert.assertEquals("All 3 target strings should be leveraged", 3, byName.size());

    // some.id.one: unchanged across pushes → should pick TRANSLATION ONE with high confidence
    Assert.assertEquals("TRANSLATION ONE", byName.get("some.id.one").getContent());
    Assert.assertEquals(TMTextUnitVariant.Status.APPROVED, byName.get("some.id.one").getStatus());

    // some.id.two: push #2 updated the content, push #3 kept name+content+comment identical
    // → MD5 match with the v2 TU, should pick NEW TRANSLATION TWO (over older TRANSLATION TWO)
    Assert.assertEquals("NEW TRANSLATION TWO", byName.get("some.id.two").getContent());
    Assert.assertEquals(TMTextUnitVariant.Status.APPROVED, byName.get("some.id.two").getStatus());

    // some.id.three: push #2 updated the content, push #3 changed it too
    // → should still pick NEW TRANSLATION THREE (more recently translated) over TRANSLATION THREE,
    // but downgrade its status due to low confidence (multiple matches on name-only)
    Assert.assertEquals("NEW TRANSLATION THREE", byName.get("some.id.three").getContent());
    Assert.assertEquals(
        TMTextUnitVariant.Status.TRANSLATION_NEEDED, byName.get("some.id.three").getStatus());
  }

  @Test
  public void copyByNamePrefersUsedOverUnused()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    Repository sourceRepo = createRepository("source", "fr-FR");

    // Asset 1: push with greeting=Hello, then translate. This text unit is USED.
    Asset sourceAsset1 = createAsset(sourceRepo, "asset1.properties");
    TMTextUnit usedTu =
        push(sourceRepo, sourceAsset1, tu("greeting", "Hello", "A greeting")).get(0);
    translate(usedTu, "fr-FR", "Bonjour (used)");

    // Asset 2: same repo, push then empty-push to make it UNUSED.
    Asset sourceAsset2 = createAsset(sourceRepo, "asset2.properties");
    TMTextUnit unusedTu =
        push(sourceRepo, sourceAsset2, tu("greeting", "Hello", "A greeting")).get(0);
    translate(unusedTu, "fr-FR", "Bonjour (unused)");
    push(sourceRepo, sourceAsset2);

    // Target repo
    Repository targetRepo = createRepository("target", "fr-FR");
    Asset targetAsset = createAsset(targetRepo, "target.properties");
    push(targetRepo, targetAsset, tu("greeting", "Hello", "A greeting"));

    CopyTmConfig config = new CopyTmConfig();
    config.setSourceRepositoryId(sourceRepo.getId());
    config.setTargetRepositoryId(targetRepo.getId());
    config.setMode(CopyTmConfig.Mode.NAME);
    runCopyTm(config);

    List<TMTextUnitVariant> translations = getTranslations(targetRepo);
    Assert.assertEquals(1, translations.size());
    Assert.assertEquals(
        "Should pick translation from used text unit",
        "Bonjour (used)",
        translations.get(0).getContent());
  }

  @Test
  public void copyByNamePrefersMostRecentTranslationAligned()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    // When TU creation order and translation order are aligned (the typical case),
    // the more recently created TU also has the more recent translation.

    Repository sourceRepo = createRepository("source", "fr-FR");
    Asset sourceAsset = createAsset(sourceRepo, "source.properties");

    // First push: creates TU A (lower ID)
    push(sourceRepo, sourceAsset, tu("greeting", "Hello", "comment A"));
    TMTextUnit tuA =
        tmTextUnitRepository.findByAsset(sourceAsset).stream()
            .filter(t -> t.getComment().equals("comment A"))
            .findFirst()
            .orElseThrow();

    // Second push: creates TU B (higher ID), TU A becomes UNUSED
    push(sourceRepo, sourceAsset, tu("greeting", "Hello", "comment B"));
    TMTextUnit tuB =
        tmTextUnitRepository.findByAsset(sourceAsset).stream()
            .filter(t -> t.getComment().equals("comment B"))
            .findFirst()
            .orElseThrow();

    // Translate in creation order: A first, then B.
    // Both TU ID and translation timestamp point to B as "newer".
    translate(tuA, "fr-FR", "translation A (older)");
    translate(tuB, "fr-FR", "translation B (newer)");

    push(sourceRepo, sourceAsset);

    Repository targetRepo = createRepository("target", "fr-FR");
    Asset targetAsset = createAsset(targetRepo, "target.properties");
    push(targetRepo, targetAsset, tu("greeting", "Hello", "target comment"));

    CopyTmConfig config = new CopyTmConfig();
    config.setSourceRepositoryId(sourceRepo.getId());
    config.setTargetRepositoryId(targetRepo.getId());
    config.setMode(CopyTmConfig.Mode.NAME);
    runCopyTm(config);

    List<TMTextUnitVariant> translations = getTranslations(targetRepo);
    Assert.assertEquals(1, translations.size());
    Assert.assertEquals(
        "Should pick the more recently translated variant (B)",
        "translation B (newer)",
        translations.get(0).getContent());
  }

  @Test
  public void copyByNamePrefersMostRecentTranslationDiverged()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    // When TU creation order and translation order diverge, the tiebreaker should use
    // translation recency, not TU creation order. Here TU A is older but has the newer
    // translation (e.g. a translator re-translated an old string after the new one was
    // already translated).

    Repository sourceRepo = createRepository("source", "fr-FR");
    Asset sourceAsset = createAsset(sourceRepo, "source.properties");

    // First push: creates TU A (lower ID)
    push(sourceRepo, sourceAsset, tu("greeting", "Hello", "comment A"));
    TMTextUnit tuA =
        tmTextUnitRepository.findByAsset(sourceAsset).stream()
            .filter(t -> t.getComment().equals("comment A"))
            .findFirst()
            .orElseThrow();

    // Second push: creates TU B (higher ID), TU A becomes UNUSED
    push(sourceRepo, sourceAsset, tu("greeting", "Hello", "comment B"));
    TMTextUnit tuB =
        tmTextUnitRepository.findByAsset(sourceAsset).stream()
            .filter(t -> t.getComment().equals("comment B"))
            .findFirst()
            .orElseThrow();

    // Translate in REVERSE order: B first, then A.
    // TU A (older TU) gets the newer translation, TU B (newer TU) has the older translation.
    translate(tuB, "fr-FR", "translation B (older)");
    translate(tuA, "fr-FR", "translation A (newer)");

    push(sourceRepo, sourceAsset);

    Repository targetRepo = createRepository("target", "fr-FR");
    Asset targetAsset = createAsset(targetRepo, "target.properties");
    push(targetRepo, targetAsset, tu("greeting", "Hello", "target comment"));

    CopyTmConfig config = new CopyTmConfig();
    config.setSourceRepositoryId(sourceRepo.getId());
    config.setTargetRepositoryId(targetRepo.getId());
    config.setMode(CopyTmConfig.Mode.NAME);
    runCopyTm(config);

    List<TMTextUnitVariant> translations = getTranslations(targetRepo);
    Assert.assertEquals(1, translations.size());
    Assert.assertEquals(
        "Should pick TU A's translation (more recently translated), not TU B's "
            + "(proves translation recency is used, not TU creation order)",
        "translation A (newer)",
        translations.get(0).getContent());
  }

  @Test
  public void copyByNameMatchesMd5WithNoRetranslation()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    // Source repo has greeting=Hello with comment "A greeting", translated to "Bonjour"
    Repository sourceRepo = createRepository("source", "fr-FR");
    Asset sourceAsset = createAsset(sourceRepo, "source.properties");
    TMTextUnit sourceTu =
        push(sourceRepo, sourceAsset, tu("greeting", "Hello", "A greeting")).get(0);
    translate(sourceTu, "fr-FR", "Bonjour");

    // Target repo has identical text unit (same name, content, comment → MD5 match)
    Repository targetRepo = createRepository("target", "fr-FR");
    Asset targetAsset = createAsset(targetRepo, "target.properties");
    push(targetRepo, targetAsset, tu("greeting", "Hello", "A greeting"));

    // Even though mode is NAME, an MD5 match should be detected first and the translation
    // should be leveraged with APPROVED status (no re-translation flag).
    CopyTmConfig config = new CopyTmConfig();
    config.setSourceRepositoryId(sourceRepo.getId());
    config.setTargetRepositoryId(targetRepo.getId());
    config.setMode(CopyTmConfig.Mode.NAME);
    runCopyTm(config);

    List<TMTextUnitVariant> translations = getTranslations(targetRepo);
    Assert.assertEquals(1, translations.size());
    Assert.assertEquals("Bonjour", translations.get(0).getContent());
    Assert.assertEquals(
        "MD5 match should preserve APPROVED status (no re-translation needed)",
        TMTextUnitVariant.Status.APPROVED,
        translations.get(0).getStatus());

    TMTextUnitVariantComment comment =
        tmTextUnitVariantCommentRepository
            .findAllByTmTextUnitVariant_id(translations.get(0).getId())
            .get(0);
    Assert.assertTrue(
        "Comment should indicate MD5 match level", comment.getContent().contains("MD5"));
  }

  @Test
  public void copyByNameFallsBackToNameOnlyWithRetranslation()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    // Source repo has greeting="Hello world"
    Repository sourceRepo = createRepository("source", "fr-FR");
    Asset sourceAsset = createAsset(sourceRepo, "source.properties");
    TMTextUnit sourceTu =
        push(sourceRepo, sourceAsset, tu("greeting", "Hello world", "comment")).get(0);
    translate(sourceTu, "fr-FR", "Bonjour le monde");

    // Target repo has same name "greeting" but different content "Hello everyone".
    // Only the name matches — no MD5 or content match.
    Repository targetRepo = createRepository("target", "fr-FR");
    Asset targetAsset = createAsset(targetRepo, "target.properties");
    push(targetRepo, targetAsset, tu("greeting", "Hello everyone", "different comment"));

    // Should leverage the translation but flag as TRANSLATION_NEEDED since only name matched.
    CopyTmConfig config = new CopyTmConfig();
    config.setSourceRepositoryId(sourceRepo.getId());
    config.setTargetRepositoryId(targetRepo.getId());
    config.setMode(CopyTmConfig.Mode.NAME);
    runCopyTm(config);

    List<TMTextUnitVariant> translations = getTranslations(targetRepo);
    Assert.assertEquals(1, translations.size());
    Assert.assertEquals("Bonjour le monde", translations.get(0).getContent());
    Assert.assertEquals(
        "Name-only match should mark as TRANSLATION_NEEDED",
        TMTextUnitVariant.Status.TRANSLATION_NEEDED,
        translations.get(0).getStatus());
  }

  @Test
  public void copyByExactMatchesContentOnly()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    // Source repo has name "old.key" and content "Hello world"
    Repository sourceRepo = createRepository("source", "fr-FR");
    Asset sourceAsset = createAsset(sourceRepo, "source.properties");
    TMTextUnit sourceTu =
        push(sourceRepo, sourceAsset, tu("old.key", "Hello world", "comment")).get(0);
    translate(sourceTu, "fr-FR", "Bonjour le monde");

    // Target repo has different name "new.key" but same content "Hello world".
    // In EXACT mode, a content-only match should still leverage.
    Repository targetRepo = createRepository("target", "fr-FR");
    Asset targetAsset = createAsset(targetRepo, "target.properties");
    push(targetRepo, targetAsset, tu("new.key", "Hello world", "different comment"));

    CopyTmConfig config = new CopyTmConfig();
    config.setSourceRepositoryId(sourceRepo.getId());
    config.setTargetRepositoryId(targetRepo.getId());
    config.setMode(CopyTmConfig.Mode.EXACT);
    runCopyTm(config);

    List<TMTextUnitVariant> translations = getTranslations(targetRepo);
    Assert.assertEquals(1, translations.size());
    Assert.assertEquals("Bonjour le monde", translations.get(0).getContent());
    Assert.assertEquals(
        "Content-only match should mark as TRANSLATION_NEEDED",
        TMTextUnitVariant.Status.TRANSLATION_NEEDED,
        translations.get(0).getStatus());
  }

  @Test
  public void copyByExactPrefersHigherPrecisionEvenWhenAllUnused()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    // Source has 3 groups of TUs (all UNUSED after empty push), each matching one target
    // string at a different precision level:
    //
    // For target's "key.one":
    //   key.one=Hello/comment one        → MD5 match (name+content+comment all identical)
    //   key.one=Hello/comment one alt    → NAME_AND_CONTENT match (same name+content, different
    // comment)
    //   other.one=Hello/comment one      → CONTENT_ONLY match (same content, different name)
    //
    // For target's "key.two":
    //   key.two=World/comment two alt    → NAME_AND_CONTENT match (same name+content, different
    // comment)
    //   other.two=World/comment two      → CONTENT_ONLY match (same content, different name)
    //
    // For target's "key.three":
    //   other.three=Goodbye/comment three → CONTENT_ONLY match (same content, different name)
    Repository sourceRepo = createRepository("source", "fr-FR");
    Asset sourceAsset = createAsset(sourceRepo, "source.properties");
    push(
        sourceRepo,
        sourceAsset,
        tu("key.one", "Hello", "comment one"),
        tu("key.one", "Hello", "comment one alt"),
        tu("other.one", "Hello", "comment one"),
        tu("key.two", "World", "comment two alt"),
        tu("other.two", "World", "comment two"),
        tu("other.three", "Goodbye", "comment three"));
    for (TMTextUnit tu : tmTextUnitRepository.findByAsset(sourceAsset)) {
      translate(tu, "fr-FR", tu.getName() + "/" + tu.getComment());
    }
    push(sourceRepo, sourceAsset);

    // Target has 3 strings, each exercising a different best match level
    Repository targetRepo = createRepository("target", "fr-FR");
    Asset targetAsset = createAsset(targetRepo, "target.properties");
    push(
        targetRepo,
        targetAsset,
        tu("key.one", "Hello", "comment one"),
        tu("key.two", "World", "comment two"),
        tu("key.three", "Goodbye", "comment three"));

    CopyTmConfig config = new CopyTmConfig();
    config.setSourceRepositoryId(sourceRepo.getId());
    config.setTargetRepositoryId(targetRepo.getId());
    config.setMode(CopyTmConfig.Mode.EXACT);
    runCopyTm(config);

    Map<String, TMTextUnitVariant> byName = getTranslationsByName(targetRepo, targetAsset);
    Assert.assertEquals("All 3 target strings should be leveraged", 3, byName.size());

    // key.one: MD5 match → APPROVED (no re-translation needed)
    Assert.assertEquals("key.one/comment one", byName.get("key.one").getContent());
    Assert.assertEquals(TMTextUnitVariant.Status.APPROVED, byName.get("key.one").getStatus());

    // key.two: name+content match (best available) → APPROVED (name+content is high confidence)
    Assert.assertEquals("key.two/comment two alt", byName.get("key.two").getContent());
    Assert.assertEquals(TMTextUnitVariant.Status.APPROVED, byName.get("key.two").getStatus());

    // key.three: content-only match (best available) → TRANSLATION_NEEDED
    Assert.assertEquals("other.three/comment three", byName.get("key.three").getContent());
    Assert.assertEquals(
        TMTextUnitVariant.Status.TRANSLATION_NEEDED, byName.get("key.three").getStatus());
  }

  @Test
  public void copyByNameMatchLevelBeatsUsedStatus()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    Repository sourceRepo = createRepository("source", "fr-FR");

    // Asset 1: name-only match with target (same name, different content). USED.
    Asset sourceAsset1 = createAsset(sourceRepo, "asset1.properties");
    TMTextUnit usedNameOnly =
        push(sourceRepo, sourceAsset1, tu("greeting", "Hello world", "comment")).get(0);
    translate(usedNameOnly, "fr-FR", "name-only used");

    // Asset 2: name+content match with target. Push then empty-push → UNUSED.
    Asset sourceAsset2 = createAsset(sourceRepo, "asset2.properties");
    TMTextUnit unusedNameContent =
        push(sourceRepo, sourceAsset2, tu("greeting", "Hello", "different comment")).get(0);
    translate(unusedNameContent, "fr-FR", "name+content unused");
    push(sourceRepo, sourceAsset2);

    // Target has name="greeting", content="Hello" — matches asset2 by name+content,
    // asset1 by name-only. Even though asset1 is USED and asset2 is UNUSED,
    // the higher-precision name+content match should win.
    Repository targetRepo = createRepository("target", "fr-FR");
    Asset targetAsset = createAsset(targetRepo, "target.properties");
    push(targetRepo, targetAsset, tu("greeting", "Hello", "target comment"));

    CopyTmConfig config = new CopyTmConfig();
    config.setSourceRepositoryId(sourceRepo.getId());
    config.setTargetRepositoryId(targetRepo.getId());
    config.setMode(CopyTmConfig.Mode.NAME);
    runCopyTm(config);

    List<TMTextUnitVariant> translations = getTranslations(targetRepo);
    Assert.assertEquals(1, translations.size());
    Assert.assertEquals(
        "Higher-precision unused name+content match should beat used name-only match",
        "name+content unused",
        translations.get(0).getContent());
  }

  @Test
  public void copyByExactMatchLevelBeatsUsedStatus()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    Repository sourceRepo = createRepository("source", "fr-FR");

    // Asset 1: content-only match with target (same content, different name). USED.
    Asset sourceAsset1 = createAsset(sourceRepo, "asset1.properties");
    TMTextUnit usedContentOnly =
        push(sourceRepo, sourceAsset1, tu("old.key", "Hello", "comment")).get(0);
    translate(usedContentOnly, "fr-FR", "content-only used");

    // Asset 2: name+content match with target. Push then empty-push → UNUSED.
    Asset sourceAsset2 = createAsset(sourceRepo, "asset2.properties");
    TMTextUnit unusedNameContent =
        push(sourceRepo, sourceAsset2, tu("greeting", "Hello", "different comment")).get(0);
    translate(unusedNameContent, "fr-FR", "name+content unused");
    push(sourceRepo, sourceAsset2);

    // Target has name="greeting", content="Hello" — matches asset2 by name+content,
    // asset1 by content-only. Even though asset1 is USED and asset2 is UNUSED,
    // the higher-precision name+content match should win.
    Repository targetRepo = createRepository("target", "fr-FR");
    Asset targetAsset = createAsset(targetRepo, "target.properties");
    push(targetRepo, targetAsset, tu("greeting", "Hello", "target comment"));

    CopyTmConfig config = new CopyTmConfig();
    config.setSourceRepositoryId(sourceRepo.getId());
    config.setTargetRepositoryId(targetRepo.getId());
    config.setMode(CopyTmConfig.Mode.EXACT);
    runCopyTm(config);

    List<TMTextUnitVariant> translations = getTranslations(targetRepo);
    Assert.assertEquals(1, translations.size());
    Assert.assertEquals(
        "Higher-precision unused name+content match should beat used content-only match",
        "name+content unused",
        translations.get(0).getContent());
  }

  @Test
  public void copyByNameMd5MatchLevelBeatsUsedStatus()
      throws InterruptedException,
          ExecutionException,
          RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          AssetWithIdNotFoundException,
          RepositoryWithIdNotFoundException {

    Repository sourceRepo = createRepository("source", "fr-FR");

    // Asset 1: name+content match with target (same name+content, different comment). USED.
    Asset sourceAsset1 = createAsset(sourceRepo, "asset1.properties");
    TMTextUnit usedNameContent =
        push(sourceRepo, sourceAsset1, tu("greeting", "Hello", "used comment")).get(0);
    translate(usedNameContent, "fr-FR", "name+content used");

    // Asset 2: MD5 match with target (name+content+comment all match). Push then empty-push →
    // UNUSED.
    Asset sourceAsset2 = createAsset(sourceRepo, "asset2.properties");
    TMTextUnit unusedMd5 =
        push(sourceRepo, sourceAsset2, tu("greeting", "Hello", "target comment")).get(0);
    translate(unusedMd5, "fr-FR", "md5 unused");
    push(sourceRepo, sourceAsset2);

    // Target has name="greeting", content="Hello", comment="target comment" — matches
    // asset2 at MD5 level (unused) and asset1 at name+content level (used).
    // MD5 should win despite being unused.
    Repository targetRepo = createRepository("target", "fr-FR");
    Asset targetAsset = createAsset(targetRepo, "target.properties");
    push(targetRepo, targetAsset, tu("greeting", "Hello", "target comment"));

    CopyTmConfig config = new CopyTmConfig();
    config.setSourceRepositoryId(sourceRepo.getId());
    config.setTargetRepositoryId(targetRepo.getId());
    config.setMode(CopyTmConfig.Mode.NAME);
    runCopyTm(config);

    List<TMTextUnitVariant> translations = getTranslations(targetRepo);
    Assert.assertEquals(1, translations.size());
    Assert.assertEquals(
        "Higher-precision unused MD5 match should beat used name+content match",
        "md5 unused",
        translations.get(0).getContent());
  }
}
