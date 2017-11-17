package com.box.l10n.mojito.service.leveraging;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TM;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTestData;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
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

    @Test
    public void copyAllTranslationsWithMD5MatchBetweenRepositories() throws InterruptedException, ExecutionException, RepositoryNameAlreadyUsedException {

        TMTestData tmTestDataSource = new TMTestData(testIdWatcher);

        Repository sourceRepository = tmTestDataSource.repository;

        logger.debug("Create the target repository");
        Repository targetRepository = repositoryService.createRepository(testIdWatcher.getEntityName("targetRepository"));

        TM tm = targetRepository.getTm();

        Asset asset = assetService.createAsset(targetRepository.getId(), "fake for test", "fake_for_test");
        Long assetId = asset.getId();

        tmService.addTMTextUnit(tm.getId(), assetId, "zuora_error_message_verify_state_province", "Please enter a valid state, region or province", "Comment1");
        tmService.addTMTextUnit(tm.getId(), assetId, "TEST2", "Content2", "Comment2");
        tmService.addTMTextUnit(tm.getId(), assetId, "TEST3", "Content3", "Comment3");

        leveragingService.copyAllTranslationsWithMD5MatchBetweenRepositories(sourceRepository, targetRepository).get();

        List<TMTextUnitVariant> sourceTranslations = tmTextUnitVariantRepository.findByTmTextUnitTmRepositoriesOrderByContent(sourceRepository);
        List<TMTextUnitVariant> targetTranslations = tmTextUnitVariantRepository.findByTmTextUnitTmRepositoriesOrderByContent(targetRepository);

        Iterator<TMTextUnitVariant> itSource = sourceTranslations.iterator();
        Iterator<TMTextUnitVariant> itTarget = targetTranslations.iterator();

        while (itTarget.hasNext()) {
            TMTextUnitVariant next = itTarget.next();
            Assert.assertEquals("translation in source and target must be the same", itSource.next().getContent(), next.getContent());
        }

        Assert.assertFalse(itTarget.hasNext());
        Assert.assertFalse(itSource.hasNext());

    }

    @Test
    public void copyAllTranslationsWithExactMatchBetweenRepositories() throws InterruptedException, ExecutionException, RepositoryNameAlreadyUsedException {

        TMTestData tmTestDataSource = new TMTestData(testIdWatcher);

        Repository sourceRepository = tmTestDataSource.repository;

        logger.debug("Create the target repository");
        Repository targetRepository = repositoryService.createRepository(testIdWatcher.getEntityName("targetRepository"));

        TM tm = targetRepository.getTm();

        Asset asset = assetService.createAsset(targetRepository.getId(), "fake for test", "fake_for_test");
        Long assetId = asset.getId();

        tmService.addTMTextUnit(tm.getId(), assetId, "zuora_error_message_verify_state_province_update", "Please enter a valid state, region or province", "Comment1");
        tmService.addTMTextUnit(tm.getId(), assetId, "TEST2", "Content2", "Comment2");
        tmService.addTMTextUnit(tm.getId(), assetId, "TEST3", "Content3", "Comment3");

        leveragingService.copyAllTranslationsWithExactMatchBetweenRepositories(sourceRepository, targetRepository).get();

        List<TMTextUnitVariant> sourceTranslations = tmTextUnitVariantRepository.findByTmTextUnitTmRepositoriesOrderByContent(sourceRepository);
        List<TMTextUnitVariant> targetTranslations = tmTextUnitVariantRepository.findByTmTextUnitTmRepositoriesOrderByContent(targetRepository);

        Iterator<TMTextUnitVariant> itSource = sourceTranslations.iterator();
        Iterator<TMTextUnitVariant> itTarget = targetTranslations.iterator();

        while (itTarget.hasNext()) {
            TMTextUnitVariant next = itTarget.next();
            Assert.assertEquals("translation in source and target must be the same", itSource.next().getContent(), next.getContent());
        }

        Assert.assertFalse(itTarget.hasNext());
        Assert.assertFalse(itSource.hasNext());

    }
    
    @Test
    public void checkCommentsAreNotCopiedIfTmTextUnitCurrentVariantNotChanged() throws InterruptedException, ExecutionException, RepositoryNameAlreadyUsedException {

        TMTestData tmTestDataSource = new TMTestData(testIdWatcher);

        Repository sourceRepository = tmTestDataSource.repository;

        logger.debug("Create the target repository");
        Repository targetRepository = repositoryService.createRepository(testIdWatcher.getEntityName("targetRepository"));

        TM tm = targetRepository.getTm();

        Asset asset = assetService.createAsset(targetRepository.getId(), "fake for test", "fake_for_test");
        Long assetId = asset.getId();

        tmService.addTMTextUnit(tm.getId(), assetId, "zuora_error_message_verify_state_province_update", "Please enter a valid state, region or province", "Comment1");
        tmService.addTMTextUnit(tm.getId(), assetId, "TEST2", "Content2", "Comment2");
        tmService.addTMTextUnit(tm.getId(), assetId, "TEST3", "Content3", "Comment3");

        leveragingService.copyAllTranslationsWithExactMatchBetweenRepositories(sourceRepository, targetRepository).get();
        leveragingService.copyAllTranslationsWithExactMatchBetweenRepositories(sourceRepository, targetRepository).get();

        List<TMTextUnitVariant> targetTranslations = tmTextUnitVariantRepository.findByTmTextUnitTmRepositoriesOrderByContent(targetRepository);
        for (TMTextUnitVariant targetTranslation : targetTranslations) {
            if (!"en".equals(targetTranslation.getLocale().getBcp47Tag())) {
                Assert.assertEquals(1, targetTranslations.get(4).getTmTextUnitVariantComments().size());
            }
        }
    }

}
