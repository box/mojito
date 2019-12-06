package com.box.l10n.mojito.service.leveraging;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TM;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.rest.asset.AssetWithIdNotFoundException;
import com.box.l10n.mojito.rest.leveraging.CopyTmConfig;
import com.box.l10n.mojito.rest.repository.RepositoryWithIdNotFoundException;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.repository.RepositoryLocaleCreationException;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTestData;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author jaurambault
 */
public class LeveragingServiceTest extends ServiceTestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(LeveragingServiceTest.class);

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    @Autowired
    LeveragingService leveragingService;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    TMTextUnitVariantRepository tmTextUnitVariantRepository;

    @Autowired
    AssetService assetService;

    @Autowired
    TMService tmService;

    @Autowired
    LocaleService localeService;

    @Test
    public void copyAllTranslationsWithMD5MatchBetweenRepositories() throws InterruptedException, ExecutionException, RepositoryNameAlreadyUsedException, AssetWithIdNotFoundException, RepositoryWithIdNotFoundException {

        TMTestData tmTestDataSource = new TMTestData(testIdWatcher);

        Repository sourceRepository = tmTestDataSource.repository;

        logger.debug("Create the target repository");
        Repository targetRepository = repositoryService.createRepository(testIdWatcher.getEntityName("targetRepository"));

        TM tm = targetRepository.getTm();

        Asset asset = assetService.createAssetWithContent(targetRepository.getId(), "fake_for_test", "fake for test");
        Long assetId = asset.getId();

        tmService.addTMTextUnit(tm.getId(), assetId, "zuora_error_message_verify_state_province", "Please enter a valid state, region or province", "Comment1");
        tmService.addTMTextUnit(tm.getId(), assetId, "TEST2", "Content2", "Comment2");
        tmService.addTMTextUnit(tm.getId(), assetId, "TEST3", "Content3", "Comment3");

        CopyTmConfig copyTmConfig = new CopyTmConfig();
        copyTmConfig.setSourceRepositoryId(sourceRepository.getId());
        copyTmConfig.setTargetRepositoryId(targetRepository.getId());

        leveragingService.copyTm(copyTmConfig).get();

        List<TMTextUnitVariant> sourceTranslations = tmTextUnitVariantRepository.findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(sourceRepository, "en");
        List<TMTextUnitVariant> targetTranslations = tmTextUnitVariantRepository.findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(targetRepository, "en");

        Iterator<TMTextUnitVariant> itSource = sourceTranslations.iterator();
        Iterator<TMTextUnitVariant> itTarget = targetTranslations.iterator();

        while (itTarget.hasNext()) {
            TMTextUnitVariant next = itTarget.next();
            Assert.assertEquals("translation in source and target must be the same", itSource.next().getContent(), next.getContent());
        }

        Assert.assertFalse(itSource.hasNext());
    }

    @Test
    public void copyAllTranslationsWithMD5MatchBetweenRepositoriesNameRegex() throws InterruptedException, ExecutionException, RepositoryNameAlreadyUsedException, AssetWithIdNotFoundException, RepositoryWithIdNotFoundException {

        TMTestData tmTestDataSource = new TMTestData(testIdWatcher);

        Repository sourceRepository = tmTestDataSource.repository;

        logger.debug("Create the target repository");
        Repository targetRepository = repositoryService.createRepository(testIdWatcher.getEntityName("targetRepository"));

        TM tm = targetRepository.getTm();

        Asset asset = assetService.createAssetWithContent(targetRepository.getId(), "fake_for_test", "fake for test");
        Long assetId = asset.getId();

        tmService.addTMTextUnit(tm.getId(), assetId, "zuora_error_message_verify_state_province", "Please enter a valid state, region or province", "Comment1");
        tmService.addTMTextUnit(tm.getId(), assetId, "TEST2", "Content2", "Comment2");
        tmService.addTMTextUnit(tm.getId(), assetId, "TEST3", "Content3", "Comment3");

        CopyTmConfig copyTmConfig = new CopyTmConfig();
        copyTmConfig.setSourceRepositoryId(sourceRepository.getId());
        copyTmConfig.setTargetRepositoryId(targetRepository.getId());
        copyTmConfig.setNameRegex("TEST.*");

        leveragingService.copyTm(copyTmConfig).get();

        List<TMTextUnitVariant> sourceTranslations = tmTextUnitVariantRepository.findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(sourceRepository, "en");
        List<TMTextUnitVariant> targetTranslations = tmTextUnitVariantRepository.findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(targetRepository, "en");

        Predicate<TMTextUnitVariant> filterZuora = new Predicate<TMTextUnitVariant>() {
            @Override
            public boolean apply(TMTextUnitVariant tmtuv) {
                return !"zuora_error_message_verify_state_province".equals(tmtuv.getTmTextUnit().getName());
            }

        };

        Iterator<TMTextUnitVariant> itSource = Iterables.filter(sourceTranslations, filterZuora).iterator();
        Iterator<TMTextUnitVariant> itTarget = targetTranslations.iterator();

        while (itTarget.hasNext()) {
            TMTextUnitVariant next = itTarget.next();
            Assert.assertEquals("translation in source and target must be the same", itSource.next().getContent(), next.getContent());
        }

        Assert.assertFalse(itSource.hasNext());

    }

    @Test
    public void copyTranslationForTmTextUnitMapping() throws InterruptedException, ExecutionException, RepositoryNameAlreadyUsedException, AssetWithIdNotFoundException, RepositoryWithIdNotFoundException {

        TMTestData tmTestDataSource = new TMTestData(testIdWatcher);

        Repository sourceRepository = tmTestDataSource.repository;

        logger.debug("Create the target repository");
        Repository targetRepository = repositoryService.createRepository(testIdWatcher.getEntityName("targetRepository"));

        TM tm = targetRepository.getTm();

        Asset asset = assetService.createAssetWithContent(targetRepository.getId(), "fake_for_test", "fake for test");
        Long assetId = asset.getId();

        TMTextUnit targetTmTextUnit1 = tmService.addTMTextUnit(tm.getId(), assetId, "zuora_error_message_verify_state_province other", "Please enter a valid state, region or province", "Comment1");
        TMTextUnit targetTmTextUnit2 = tmService.addTMTextUnit(tm.getId(), assetId, "TEST2 other", "Content2", "Comment2");
        TMTextUnit targetTmTextUnit3 = tmService.addTMTextUnit(tm.getId(), assetId, "TEST3 other", "Content3", "Comment3");

        Map<Long, Long> sourceTotTargetTmTextUnitIds = new HashMap<>();
        sourceTotTargetTmTextUnitIds.put(tmTestDataSource.addTMTextUnit1.getId(), targetTmTextUnit1.getId());
        sourceTotTargetTmTextUnitIds.put(tmTestDataSource.addTMTextUnit2.getId(), targetTmTextUnit2.getId());

        CopyTmConfig copyTmConfig = new CopyTmConfig();
        copyTmConfig.setSourceRepositoryId(sourceRepository.getId());
        copyTmConfig.setTargetRepositoryId(targetRepository.getId());
        copyTmConfig.setMode(CopyTmConfig.Mode.TUIDS);
        copyTmConfig.setSourceToTargetTmTextUnitIds(sourceTotTargetTmTextUnitIds);

        leveragingService.copyTm(copyTmConfig).get();

        List<TMTextUnitVariant> sourceTranslations = tmTextUnitVariantRepository.findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(sourceRepository, "en");
        List<TMTextUnitVariant> targetTranslations = tmTextUnitVariantRepository.findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(targetRepository, "en");

        Assert.assertEquals(5, sourceTranslations.size());
        Assert.assertEquals(3, targetTranslations.size());

        Assert.assertEquals("Content2 fr-CA", targetTranslations.get(0).getContent());
        Assert.assertEquals(targetTmTextUnit2.getId(), targetTranslations.get(0).getTmTextUnit().getId());


        Assert.assertEquals("Veuillez indiquer un état, une région ou une province valide.", targetTranslations.get(1).getContent());
        Assert.assertEquals(targetTmTextUnit1.getId(), targetTranslations.get(1).getTmTextUnit().getId());


        Assert.assertEquals("올바른 국가, 지역 또는 시/도를 입력하십시오.", targetTranslations.get(2).getContent());
        Assert.assertEquals(targetTmTextUnit1.getId(), targetTranslations.get(2).getTmTextUnit().getId());
    }

    @Test
    public void copyAllTranslationsWithExactMatchBetweenRepositories() throws InterruptedException, ExecutionException, RepositoryNameAlreadyUsedException, AssetWithIdNotFoundException, RepositoryWithIdNotFoundException {

        TMTestData tmTestDataSource = new TMTestData(testIdWatcher);

        Repository sourceRepository = tmTestDataSource.repository;

        logger.debug("Create the target repository");
        Repository targetRepository = repositoryService.createRepository(testIdWatcher.getEntityName("targetRepository"));

        TM tm = targetRepository.getTm();

        Asset asset = assetService.createAssetWithContent(targetRepository.getId(), "fake_for_test", "fake for test");
        Long assetId = asset.getId();

        tmService.addTMTextUnit(tm.getId(), assetId, "zuora_error_message_verify_state_province_update", "Please enter a valid state, region or province", "Comment1");
        tmService.addTMTextUnit(tm.getId(), assetId, "TEST2", "Content2", "Comment2");
        tmService.addTMTextUnit(tm.getId(), assetId, "TEST3", "Content3", "Comment3");

        CopyTmConfig copyTmConfig = new CopyTmConfig();
        copyTmConfig.setSourceRepositoryId(sourceRepository.getId());
        copyTmConfig.setTargetRepositoryId(targetRepository.getId());
        copyTmConfig.setMode(CopyTmConfig.Mode.EXACT);

        leveragingService.copyTm(copyTmConfig).get();

        List<TMTextUnitVariant> sourceTranslations = tmTextUnitVariantRepository.findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(sourceRepository, "en");
        List<TMTextUnitVariant> targetTranslations = tmTextUnitVariantRepository.findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(targetRepository, "en");

        Iterator<TMTextUnitVariant> itSource = sourceTranslations.iterator();
        Iterator<TMTextUnitVariant> itTarget = targetTranslations.iterator();

        while (itTarget.hasNext()) {
            TMTextUnitVariant next = itTarget.next();
            Assert.assertEquals("translation in source and target must be the same", itSource.next().getContent(), next.getContent());
        }

        Assert.assertFalse(itSource.hasNext());
    }

    @Test
    public void checkCommentsAreNotCopiedIfTmTextUnitCurrentVariantNotChanged() throws InterruptedException, ExecutionException, RepositoryNameAlreadyUsedException, AssetWithIdNotFoundException, RepositoryWithIdNotFoundException {

        TMTestData tmTestDataSource = new TMTestData(testIdWatcher);

        Repository sourceRepository = tmTestDataSource.repository;

        logger.debug("Create the target repository");
        Repository targetRepository = repositoryService.createRepository(testIdWatcher.getEntityName("targetRepository"));

        TM tm = targetRepository.getTm();

        Asset asset = assetService.createAssetWithContent(targetRepository.getId(), "fake_for_test", "fake for test");
        Long assetId = asset.getId();

        tmService.addTMTextUnit(tm.getId(), assetId, "zuora_error_message_verify_state_province_update", "Please enter a valid state, region or province", "Comment1");
        tmService.addTMTextUnit(tm.getId(), assetId, "TEST2", "Content2", "Comment2");
        tmService.addTMTextUnit(tm.getId(), assetId, "TEST3", "Content3", "Comment3");

        CopyTmConfig copyTmConfig = new CopyTmConfig();
        copyTmConfig.setSourceRepositoryId(sourceRepository.getId());
        copyTmConfig.setTargetRepositoryId(targetRepository.getId());

        leveragingService.copyTm(copyTmConfig).get();
        leveragingService.copyTm(copyTmConfig).get();

        List<TMTextUnitVariant> targetTranslations = tmTextUnitVariantRepository.findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(targetRepository, "en");
        for (TMTextUnitVariant targetTranslation : targetTranslations) {
            Assert.assertEquals(1, targetTranslation.getTmTextUnitVariantComments().size());
        }
    }

    @Test
    public void copyBetweenAssets() throws InterruptedException, ExecutionException, RepositoryNameAlreadyUsedException, RepositoryLocaleCreationException, AssetWithIdNotFoundException, RepositoryWithIdNotFoundException {

        Locale frFR = localeService.findByBcp47Tag("fr-FR");

        logger.debug("Create the source repository");
        Repository sourceRepository = repositoryService.createRepository(testIdWatcher.getEntityName("sourceRepository"));
        repositoryService.addRepositoryLocale(sourceRepository, frFR.getBcp47Tag());

        Asset sourceAsset = assetService.createAssetWithContent(sourceRepository.getId(), "fake_for_test_1", "fake for test");
        Long sourceAssetId = sourceAsset.getId();

        TMTextUnit addTMTextUnit = tmService.addTMTextUnit(sourceRepository.getTm().getId(), sourceAssetId, "TEST3", "Content3", "Comment3");
        tmService.addCurrentTMTextUnitVariant(addTMTextUnit.getId(), frFR.getId(), "Content3 fr-FR from source");

        Asset sourceAsset2 = assetService.createAssetWithContent(sourceRepository.getId(), "fake_for_test2", "fake for test");
        Long sourceAssetId2 = sourceAsset2.getId();

        TMTextUnit addTMTextUnit2 = tmService.addTMTextUnit(sourceRepository.getTm().getId(), sourceAssetId2, "TEST3", "Content3", "Comment3");
        tmService.addCurrentTMTextUnitVariant(addTMTextUnit2.getId(), frFR.getId(), "Content3 fr-FR from source2");

        logger.debug("Create the target repository");
        Repository targetRepository = repositoryService.createRepository(testIdWatcher.getEntityName("targetRepository"));

        Asset targetAsset = assetService.createAssetWithContent(targetRepository.getId(), "fake_for_test", "fake for test");
        Long targetAssetId = targetAsset.getId();

        tmService.addTMTextUnit(targetRepository.getTm().getId(), targetAssetId, "TEST3", "Content3", "Comment3");

        Asset targetAsset2 = assetService.createAssetWithContent(targetRepository.getId(), "fake_for_test2", "fake for test");
        Long targetAssetId2 = targetAsset2.getId();

        tmService.addTMTextUnit(targetRepository.getTm().getId(), targetAssetId2, "TEST3", "Content3", "Comment3");

        CopyTmConfig copyTmConfig = new CopyTmConfig();
        copyTmConfig.setSourceAssetId(sourceAssetId);
        copyTmConfig.setTargetAssetId(targetAssetId);
        copyTmConfig.setMode(CopyTmConfig.Mode.MD5);

        leveragingService.copyTm(copyTmConfig).get();

        List<TMTextUnitVariant> targetTranslations = tmTextUnitVariantRepository.findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(targetRepository, "en");
        Assert.assertEquals("Content3 fr-FR from source", targetTranslations.get(0).getContent());
        Assert.assertEquals(targetAsset.getId(), targetTranslations.get(0).getTmTextUnit().getAsset().getId());

        Assert.assertEquals(1, targetTranslations.size());

        CopyTmConfig copyTmConfig2 = new CopyTmConfig();
        copyTmConfig2.setSourceAssetId(sourceAssetId2);
        copyTmConfig2.setTargetAssetId(targetAssetId2);
        copyTmConfig2.setMode(CopyTmConfig.Mode.MD5);

        leveragingService.copyTm(copyTmConfig2).get();

        List<TMTextUnitVariant> targetTranslations2 = tmTextUnitVariantRepository.findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(targetRepository, "en");
        Assert.assertEquals("Content3 fr-FR from source", targetTranslations2.get(0).getContent());
        Assert.assertEquals(targetAsset.getId(), targetTranslations2.get(0).getTmTextUnit().getAsset().getId());

        Assert.assertEquals("Content3 fr-FR from source2", targetTranslations2.get(1).getContent());
        Assert.assertEquals(targetAsset2.getId(), targetTranslations2.get(1).getTmTextUnit().getAsset().getId());

        Assert.assertEquals(2, targetTranslations2.size());
    }
}
