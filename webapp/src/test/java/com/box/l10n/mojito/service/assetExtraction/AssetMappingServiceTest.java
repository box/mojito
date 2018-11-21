package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.entity.AssetTextUnitToTMTextUnit;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TM;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariantComment;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetTextUnit.AssetTextUnitRepository;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.repository.RepositoryLocaleCreationException;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.TMRepository;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantCommentRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.util.Iterator;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author jaurambault
 */
public class AssetMappingServiceTest extends ServiceTestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(AssetMappingServiceTest.class);

    @Autowired
    TMService tmService;

    @Autowired
    TMRepository tmRepository;

    @Autowired
    TMTextUnitVariantRepository tmTextUnitVariantRepository;

    @Autowired
    TMTextUnitRepository tmTextUnitRepository;

    @Autowired
    LocaleService localeService;

    @Autowired
    EntityManager entityManager;

    @Autowired
    AssetExtractionRepository assetExtractionRepository;

    @Autowired
    AssetMappingService assetMappingService;

    @Autowired
    AssetExtractionService assetExtractionService;

    @Autowired
    AssetTextUnitRepository assetTextUnitRepository;

    @Autowired
    AssetService assetService;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    TextUnitSearcher textUnitSearcher;

    @Autowired
    TMTextUnitVariantCommentRepository tmTextUnitVariantCommentRepository;

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    @Test
    public void testUpdateExactMatchesAndGetUnmappedAssetTextUnits() throws RepositoryNameAlreadyUsedException {

        logger.debug("Create data for test");

        // This is to make sure data from other TM don't have side effects
        createOtherTMAssetAndTextUnit("other-repository", "other asset content", "other-asset-path", "TEST1", "Content1", "Comment1");

        AssetExtraction assetExtractionOther = assetExtractionRepository.save(new AssetExtraction());
        assetExtractionService.createAssetTextUnit(assetExtractionOther, "TEST2", "Content2", "Comment2");

        // This is the actual data that should be proccessed
        Repository testRepository = repositoryService.createRepository(testIdWatcher.getEntityName("testUpdateExactMatchesAndGetUnmappedAssetTextUnits"));
        TM tm = testRepository.getTm();
        Asset testAsset = assetService.createAssetWithContent(testRepository.getId(), "fake_for_test_real_asset", "fake for test real tm repo");
        TMTextUnit addTMTextUnit1 = tmService.addTMTextUnit(tm.getId(), testAsset.getId(), "TEST1", "Content1", "Comment1");
        TMTextUnit addTMTextUnit2 = tmService.addTMTextUnit(tm.getId(), testAsset.getId(), "TEST2", "Content2", "Comment2");
        tmService.addTMTextUnit(tm.getId(), testAsset.getId(), "TEST4", "Content4", "Comment4");

        AssetExtraction assetExtraction = assetExtractionRepository.save(new AssetExtraction());
        AssetTextUnit createAssetTextUnit1 = assetExtractionService.createAssetTextUnit(assetExtraction, "TEST1", "Content1", "Comment1");
        AssetTextUnit createAssetTextUnit2 = assetExtractionService.createAssetTextUnit(assetExtraction, "TEST2", "Content2", "Comment2");
        AssetTextUnit createAssetTextUnit3 = assetExtractionService.createAssetTextUnit(assetExtraction, "TEST3", "Content3", "Comment3");

        logger.debug("Done creating data for test, start testing");

        assertEquals("Make sure no AssetTextUnit are mapped", 3, assetTextUnitRepository.getUnmappedAssetTextUnits(assetExtraction.getId()).size());

        assetMappingService.mapExactMatches(assetExtraction.getId(), tm.getId(), testAsset.getId());

        assertEquals("We should now have 1 AssetTextUnit mapped", 1, assetTextUnitRepository.getUnmappedAssetTextUnits(assetExtraction.getId()).size());

        Iterator<AssetTextUnitToTMTextUnit> iterator = getAssetTextUnitToTMTextUnit(assetExtraction);

        AssetTextUnitToTMTextUnit next = iterator.next();
        assertEquals("Check for mapping reccords", addTMTextUnit1.getId(), next.getTmTextUnit().getId());
        assertEquals("Check for mapping reccords", createAssetTextUnit1.getId(), next.getAssetTextUnit().getId());

        next = iterator.next();
        assertEquals("Check for mapping reccords", addTMTextUnit2.getId(), next.getTmTextUnit().getId());
        assertEquals("Check for mapping reccords", createAssetTextUnit2.getId(), next.getAssetTextUnit().getId());

        assertFalse("There shouldn't be any other mapping reccords", iterator.hasNext());

        logger.debug("Running twice updateExactMatches should not change anything because everything have been mapped already");
        assetMappingService.mapExactMatches(assetExtraction.getId(), tm.getId(), testAsset.getId());

        iterator = getAssetTextUnitToTMTextUnit(assetExtraction);

        next = iterator.next();
        assertEquals("Check for mapping reccords", addTMTextUnit1.getId(), next.getTmTextUnit().getId());
        assertEquals("Check for mapping reccords", createAssetTextUnit1.getId(), next.getAssetTextUnit().getId());

        next = iterator.next();
        assertEquals(addTMTextUnit2.getId(), next.getTmTextUnit().getId());
        assertEquals("Check for mapping reccords", createAssetTextUnit2.getId(), next.getAssetTextUnit().getId());

        assertFalse("There shouldn't be any other mapping reccords", iterator.hasNext());

        logger.debug("Adding a new TextUnit that will be a match");
        TMTextUnit addTMTextUnit3 = tmService.addTMTextUnit(tm.getId(), testAsset.getId(), "TEST3", "Content3", "Comment3");

        logger.debug("Re-map, a new entry should be added");
        assetMappingService.mapExactMatches(assetExtraction.getId(), tm.getId(), testAsset.getId());

        assertEquals("All AssetTextUnit must be mapped now", 0, assetTextUnitRepository.getUnmappedAssetTextUnits(assetExtraction.getId()).size());

        iterator = getAssetTextUnitToTMTextUnit(assetExtraction);

        next = iterator.next();
        assertEquals(addTMTextUnit1.getId(), next.getTmTextUnit().getId());
        assertEquals("Check for mapping reccords", createAssetTextUnit1.getId(), next.getAssetTextUnit().getId());

        next = iterator.next();
        assertEquals(addTMTextUnit2.getId(), next.getTmTextUnit().getId());
        assertEquals("Check for mapping reccords", createAssetTextUnit2.getId(), next.getAssetTextUnit().getId());

        next = iterator.next();
        assertEquals(addTMTextUnit3.getId(), next.getTmTextUnit().getId());
        assertEquals("Check for mapping reccords", createAssetTextUnit3.getId(), next.getAssetTextUnit().getId());

        assertFalse("There shouldn't be any other mapping reccords", iterator.hasNext());
    }

    @Test
    public void testCreateTMTextUnitForUnmappedAssetTextUnits() throws RepositoryNameAlreadyUsedException {

        logger.debug("Create data for test");

        // This is to make sure data from other TM don't have side effects
        createOtherTMAssetAndTextUnit("other-repository", "other asset content", "other-asset-path", "TEST1", "Content1", "Comment1");

        AssetExtraction assetExtractionOther = assetExtractionRepository.save(new AssetExtraction());
        assetExtractionService.createAssetTextUnit(assetExtractionOther, "TEST2", "Content2", "Comment2");

        // This is the actual data that should be proccessed
        Repository testRepository = repositoryService.createRepository(testIdWatcher.getEntityName("testCreateTMTextUnitForUnmappedAssetTextUnits"));
        TM tm = testRepository.getTm();
        Asset asset = assetService.createAssetWithContent(testRepository.getId(), "test-asset-path", "test asset content");

        AssetExtraction assetExtraction = assetExtractionRepository.save(new AssetExtraction());
        assetExtractionService.createAssetTextUnit(assetExtraction, "TEST1", "Content1", "Comment1");
        assetExtractionService.createAssetTextUnit(assetExtraction, "TEST2", "Content2", "Comment2");
        assetExtractionService.createAssetTextUnit(assetExtraction, "TEST3", "Content3", "Comment3");

        logger.debug("Done creating data for test, start testing");

        assertEquals("Nothing should be mapped yet", 3, assetTextUnitRepository.getUnmappedAssetTextUnits(assetExtraction.getId()).size());
        assertEquals("No TMTextUnit should be present in the TM", 0, tmTextUnitRepository.findByTm_id(tm.getId()).size());

        assetMappingService.createTMTextUnitForUnmappedAssetTextUnitsWithRetry(assetExtraction.getId(), tm.getId(), asset.getId());

        assertEquals("A TMTextUnit should have been created for each AssetTextUnit", 3, tmTextUnitRepository.findByTm_id(tm.getId()).size());
    }

    @Test
    public void testCreateTMTextUnitForUnmappedAssetTextUnitsIncremental() throws RepositoryNameAlreadyUsedException {

        logger.debug("Create data for test");

        // This is to make sure data from other TM don't have side effects
        createOtherTMAssetAndTextUnit("other-repository", "other asset content", "other-asset-path", "TEST1", "Content1", "Comment1");

        AssetExtraction assetExtractionOther = assetExtractionRepository.save(new AssetExtraction());
        assetExtractionService.createAssetTextUnit(assetExtractionOther, "TEST2", "Content2", "Comment2");

        // This is the actual data that should be proccessed
        Repository testRepository = repositoryService.createRepository(testIdWatcher.getEntityName("testCreateTMTextUnitForUnmappedAssetTextUnitsIncremental"));
        TM tm = testRepository.getTm();
        Asset asset = assetService.createAssetWithContent(testRepository.getId(), "test-asset-path", "test asset content");

        AssetExtraction assetExtraction = assetExtractionRepository.save(new AssetExtraction());
        assetExtractionService.createAssetTextUnit(assetExtraction, "TEST1", "Content1", "Comment1");

        logger.debug("Done creating data for test, start testing");

        assertEquals("Nothing should be mapped yet", 1, assetTextUnitRepository.getUnmappedAssetTextUnits(assetExtraction.getId()).size());
        assertEquals("No TMTextUnit should be present in the TM", 0, tmTextUnitRepository.findByTm_id(tm.getId()).size());

        assetMappingService.createTMTextUnitForUnmappedAssetTextUnitsWithRetry(assetExtraction.getId(), tm.getId(), asset.getId());

        assertEquals("A TMTextUnit should have been created", 1, tmTextUnitRepository.findByTm_id(tm.getId()).size());

        logger.debug("Adding new AssetTextUnit");
        assetExtractionService.createAssetTextUnit(assetExtraction, "TEST2", "Content2", "Comment2");
        assetExtractionService.createAssetTextUnit(assetExtraction, "TEST3", "Content3", "Comment3");

        assetMappingService.mapExactMatches(assetExtraction.getId(), tm.getId(), asset.getId());

        assertEquals("There should be 2 new unmapped AssetTextUnit", 2, assetTextUnitRepository.getUnmappedAssetTextUnits(assetExtraction.getId()).size());
        assertEquals("There should be still a single TMTextUnit", 1, tmTextUnitRepository.findByTm_id(tm.getId()).size());

        assetMappingService.createTMTextUnitForUnmappedAssetTextUnitsWithRetry(assetExtraction.getId(), tm.getId(), asset.getId());

        assertEquals("A TMTextUnit should have been created for each AssetTextUnit", 3, tmTextUnitRepository.findByTm_id(tm.getId()).size());

    }

    @Test
    public void testMap() throws RepositoryLocaleCreationException, RepositoryNameAlreadyUsedException {

        logger.debug("Create data for test");

        // This is to make sure data from other TM don't have side effects
        createOtherTMAssetAndTextUnit("other-repository", "other asset content", "other-asset-path", "TEST1", "Content1", "Comment1");

        AssetExtraction assetExtractionOther = assetExtractionRepository.save(new AssetExtraction());
        assetExtractionService.createAssetTextUnit(assetExtractionOther, "TEST2", "Content2", "Comment2");

        // This is the actual data that should be proccessed
        Repository testRepository = repositoryService.createRepository(testIdWatcher.getEntityName("testMap"));
        repositoryService.addRepositoryLocale(testRepository, "fr-FR");
        repositoryService.addRepositoryLocale(testRepository, "ja-JP");

        TM tm = testRepository.getTm();

        Asset asset = assetService.createAssetWithContent(testRepository.getId(), "fake_for_test", "fake for test");
        AssetExtraction assetExtraction = new AssetExtraction();
        assetExtraction.setAsset(asset);
        assetExtraction = assetExtractionRepository.save(assetExtraction);
        assetExtractionService.createAssetTextUnit(assetExtraction, "TEST1A", "Content1A", "Comment1A");

        logger.debug("Done creating data for test, start testing");

        assertEquals("Nothing should be mapped yet", 1, assetTextUnitRepository.getUnmappedAssetTextUnits(assetExtraction.getId()).size());
        assertEquals("No TMTextUnit should be present in the TM", 0, tmTextUnitRepository.findByTm_id(tm.getId()).size());

        assetMappingService.mapAssetTextUnitAndCreateTMTextUnit(assetExtraction.getId(), tm.getId(), asset.getId(), PollableTask.INJECT_CURRENT_TASK);

        assertEquals("All AssetTextUnit must be mapped", 0, assetTextUnitRepository.getUnmappedAssetTextUnits(assetExtraction.getId()).size());

        Iterator<AssetTextUnitToTMTextUnit> iterator = getAssetTextUnitToTMTextUnit(assetExtraction);

        AssetTextUnitToTMTextUnit next = iterator.next();
        checkMapping(next, assetExtraction, tm);
        assertFalse("There shouldn't be any other mapping reccords", iterator.hasNext());

        Asset updatedAsset = assetRepository.findOne(asset.getId());
        assertEquals("Last successfulAssetExtraction should be set", asset.getId(), updatedAsset.getId());

        logger.debug("Perform mapping on a second asset extraction that has more AssetTextUnit with same TM (what will happen when we keep updating documents)");
        AssetExtraction assetExtraction2 = new AssetExtraction();
        assetExtraction2.setAsset(asset);
        assetExtraction2 = assetExtractionRepository.save(assetExtraction2);
        assetExtractionService.createAssetTextUnit(assetExtraction2, "TEST2A", "Content2A", "Comment2A");
        assetExtractionService.createAssetTextUnit(assetExtraction2, "TEST2B", "Content2B", "Comment2B");
        assetExtractionService.createAssetTextUnit(assetExtraction2, "TEST2C", "Content2C", "Comment2C");

        assetMappingService.mapAssetTextUnitAndCreateTMTextUnit(assetExtraction2.getId(), tm.getId(), asset.getId(), PollableTask.INJECT_CURRENT_TASK);
        assertEquals("All AssetTextUnit must be mapped", 0, assetTextUnitRepository.getUnmappedAssetTextUnits(assetExtraction2.getId()).size());

        iterator = getAssetTextUnitToTMTextUnit(assetExtraction2);

        next = iterator.next();
        checkMapping(next, assetExtraction2, tm);
        checkMapping(iterator.next(), assetExtraction2, tm);
        checkMapping(iterator.next(), assetExtraction2, tm);
        assertFalse("There shouldn't be any other mapping reccords", iterator.hasNext());

        logger.debug("Perform mapping on a third asset extraction where some AssetTextUnit were removed with same TM (what will happen when we keep updating documents and removing some resources)");
        AssetExtraction assetExtraction3 = new AssetExtraction();
        assetExtraction3.setAsset(asset);
        assetExtraction3 = assetExtractionRepository.save(assetExtraction3);
        assetExtractionService.createAssetTextUnit(assetExtraction3, "TEST3A", "Content3A", "Comment3A");
        assetExtractionService.createAssetTextUnit(assetExtraction3, "TEST3C", "Content3C", "Comment3C");

        assetMappingService.mapAssetTextUnitAndCreateTMTextUnit(assetExtraction3.getId(), tm.getId(), asset.getId(), PollableTask.INJECT_CURRENT_TASK);
        assertEquals("All AssetTextUnit must be mapped", 0, assetTextUnitRepository.getUnmappedAssetTextUnits(assetExtraction3.getId()).size());

        iterator = getAssetTextUnitToTMTextUnit(assetExtraction3);
        next = iterator.next();
        checkMapping(next, assetExtraction3, tm);
        checkMapping(iterator.next(), assetExtraction3, tm);
        assertFalse("There shouldn't be any other mapping reccords", iterator.hasNext());
    }

    @Test
    public void testSourceLeveraging() throws RepositoryLocaleCreationException, RepositoryNameAlreadyUsedException {

        logger.debug("Create data for test");

        // This is to make sure data from other TM don't have side effects
        createOtherTMAssetAndTextUnit("other-repository", "other asset content", "other-asset-path", "TEST1", "Content1", "Comment1");

        AssetExtraction assetExtractionOther = assetExtractionRepository.save(new AssetExtraction());
        assetExtractionService.createAssetTextUnit(assetExtractionOther, "TEST2", "Content2", "Comment2");

        // This is the actual data that should be proccessed
        Repository testRepository = repositoryService.createRepository(testIdWatcher.getEntityName("testSourceLeveraging"));
        repositoryService.addRepositoryLocale(testRepository, "fr-FR");
        repositoryService.addRepositoryLocale(testRepository, "ja-JP");
        repositoryService.addRepositoryLocale(testRepository, "ko-KR");

        TM tm = testRepository.getTm();

        Asset asset = assetService.createAssetWithContent(testRepository.getId(), "fake_for_test", "fake for test");
        AssetExtraction assetExtraction = new AssetExtraction();
        assetExtraction.setAsset(asset);
        assetExtraction = assetExtractionRepository.save(assetExtraction);
        AssetTextUnit assetTextUnitForLeveraging = assetExtractionService.createAssetTextUnit(assetExtraction, "TEST1A", "Content1A", "Comment1A");
        assetExtractionService.createAssetTextUnit(assetExtraction, "TEST2A", "Content2A", "Comment2A");

        assetMappingService.mapAssetTextUnitAndCreateTMTextUnit(assetExtraction.getId(), tm.getId(), asset.getId(), PollableTask.INJECT_CURRENT_TASK);
        assetExtractionService.markAssetExtractionAsLastSuccessful(asset, assetExtraction);

        logger.debug("Add initial translation initial to test leveraging (this translation will be copied from 1 TMTextUnit to another");
        TMTextUnit tmTextUnitForLeveraging = tmTextUnitRepository.findFirstByTmAndMd5(tm, assetTextUnitForLeveraging.getMd5());
        TMTextUnitVariant translationfrFR = tmService.addCurrentTMTextUnitVariant(tmTextUnitForLeveraging.getId(), localeService.findByBcp47Tag("fr-FR").getId(), "Content1Afr-FR");
        TMTextUnitVariant translationjaJP = tmService.addCurrentTMTextUnitVariant(tmTextUnitForLeveraging.getId(), localeService.findByBcp47Tag("ja-JP").getId(), "Content1Aja-JP");

        logger.debug("Done creating data for test, start testing");

        logger.debug("Perform mapping where some AssetTextUnit were change to test name+content (comment change only) leveraging");

        AssetExtraction assetExtraction2 = new AssetExtraction();
        assetExtraction2.setAsset(asset);
        assetExtraction2 = assetExtractionRepository.save(assetExtraction2);
        AssetTextUnit assetTextUnitForLeveraging2 = assetExtractionService.createAssetTextUnit(assetExtraction2, "TEST1A", "Content1A", "Comment1B");
        assetExtractionService.createAssetTextUnit(assetExtraction2, "TEST2A", "Content2A", "Comment2A");

        assetMappingService.mapAssetTextUnitAndCreateTMTextUnit(assetExtraction2.getId(), tm.getId(), asset.getId(), PollableTask.INJECT_CURRENT_TASK);
        assetExtractionService.markAssetExtractionAsLastSuccessful(asset, assetExtraction2);

        TMTextUnit tmTextUnitForLeveraging2 = tmTextUnitRepository.findFirstByTmAndMd5(tm, assetTextUnitForLeveraging2.getMd5());

        TextUnitSearcherParameters textUnitSearcherParameters2 = new TextUnitSearcherParameters();
        textUnitSearcherParameters2.setTmTextUnitId(tmTextUnitForLeveraging2.getId());
        textUnitSearcherParameters2.setStatusFilter(StatusFilter.TRANSLATED);
        textUnitSearcherParameters2.setUsedFilter(UsedFilter.USED);

        List<TextUnitDTO> textUnitDTOs2 = textUnitSearcher.search(textUnitSearcherParameters2);
        Iterator<TextUnitDTO> textUnitDTOsIt2 = textUnitDTOs2.iterator();

        TextUnitDTO translation2frFR = textUnitDTOsIt2.next();

        assertEquals("TEST1A", translation2frFR.getName());
        assertEquals("Content1A", translation2frFR.getSource());
        assertEquals("Comment1B", translation2frFR.getComment());
        assertEquals("Content1Afr-FR", translation2frFR.getTarget());
        assertEquals(TMTextUnitVariant.Status.APPROVED, translation2frFR.getStatus());

        TMTextUnitVariantComment comment = tmTextUnitVariantCommentRepository.findAllByTmTextUnitVariant_id(translation2frFR.getTmTextUnitVariantId()).get(0);
        assertEquals(TMTextUnitVariantComment.Type.LEVERAGING, comment.getType());
        assertEquals("by name and content for source leveraging leveraging from: " + translationfrFR.getId() + ", unique match: true", comment.getContent());

        TextUnitDTO translation2jaJP = textUnitDTOsIt2.next();

        assertEquals("TEST1A", translation2frFR.getName());
        assertEquals("Content1A", translation2frFR.getSource());
        assertEquals("Comment1B", translation2frFR.getComment());
        assertEquals("Content1Aja-JP", translation2jaJP.getTarget());
        assertEquals(TMTextUnitVariant.Status.APPROVED, translation2jaJP.getStatus());

        comment = tmTextUnitVariantCommentRepository.findAllByTmTextUnitVariant_id(translation2jaJP.getTmTextUnitVariantId()).get(0);
        assertEquals(TMTextUnitVariantComment.Type.LEVERAGING, comment.getType());
        assertEquals("by name and content for source leveraging leveraging from: " + translationjaJP.getId() + ", unique match: true", comment.getContent());
        assertFalse("There shouldn't be any more translations", textUnitDTOsIt2.hasNext());

        logger.debug("Perform mapping where some AssetTextUnit were change to test name leveraging");

        AssetExtraction assetExtraction3 = new AssetExtraction();
        assetExtraction3.setAsset(asset);
        assetExtraction3 = assetExtractionRepository.save(assetExtraction3);
        AssetTextUnit assetTextUnitForLeveraging3 = assetExtractionService.createAssetTextUnit(assetExtraction3, "TEST1A", "Content1B", "Comment1B");
        assetExtractionService.createAssetTextUnit(assetExtraction3, "TEST2A", "Content2A", "Comment2A");

        assetMappingService.mapAssetTextUnitAndCreateTMTextUnit(assetExtraction3.getId(), tm.getId(), asset.getId(), PollableTask.INJECT_CURRENT_TASK);
        assetExtractionService.markAssetExtractionAsLastSuccessful(asset, assetExtraction3);

        TMTextUnit tmTextUnitForLeveraging3 = tmTextUnitRepository.findFirstByTmAndMd5(tm, assetTextUnitForLeveraging3.getMd5());

        TextUnitSearcherParameters textUnitSearcherParameters3 = new TextUnitSearcherParameters();
        textUnitSearcherParameters3.setTmTextUnitId(tmTextUnitForLeveraging3.getId());
        textUnitSearcherParameters3.setStatusFilter(StatusFilter.TRANSLATED);
        textUnitSearcherParameters3.setUsedFilter(UsedFilter.USED);

        List<TextUnitDTO> textUnitDTOs3 = textUnitSearcher.search(textUnitSearcherParameters3);
        Iterator<TextUnitDTO> textUnitDTOsIt3 = textUnitDTOs3.iterator();
        TextUnitDTO translation3frFR = textUnitDTOsIt3.next();

        assertEquals("TEST1A", translation3frFR.getName());
        assertEquals("Content1B", translation3frFR.getSource());
        assertEquals("Comment1B", translation3frFR.getComment());
        assertEquals("Content1Afr-FR", translation3frFR.getTarget());

        comment = tmTextUnitVariantCommentRepository.findAllByTmTextUnitVariant_id(translation3frFR.getTmTextUnitVariantId()).get(1);
        assertEquals(TMTextUnitVariantComment.Type.LEVERAGING, comment.getType());
        assertEquals("by name for source leveraging leveraging from: " + translation2frFR.getTmTextUnitVariantId() + ", unique match: true", comment.getContent());
        assertEquals("The source text has changed, hence it needs review", TMTextUnitVariant.Status.TRANSLATION_NEEDED, translation3frFR.getStatus());

        TextUnitDTO translation3jaJP = textUnitDTOsIt3.next();

        assertEquals("TEST1A", translation3jaJP.getName());
        assertEquals("Content1B", translation3jaJP.getSource());
        assertEquals("Comment1B", translation3jaJP.getComment());
        assertEquals("Content1Aja-JP", translation3jaJP.getTarget());
        assertEquals("The source text has changed, hence it needs review", TMTextUnitVariant.Status.TRANSLATION_NEEDED, translation3jaJP.getStatus());

        comment = tmTextUnitVariantCommentRepository.findAllByTmTextUnitVariant_id(translation3jaJP.getTmTextUnitVariantId()).get(1);
        assertEquals(TMTextUnitVariantComment.Type.LEVERAGING, comment.getType());
        assertEquals("by name for source leveraging leveraging from: " + translation2jaJP.getTmTextUnitVariantId() + ", unique match: true", comment.getContent());

        assertFalse("There shouldn't be any more translations", textUnitDTOsIt3.hasNext());

        logger.debug("Perform mapping where some AssetTextUnit were change to test content leveraging");

        AssetExtraction assetExtraction4 = new AssetExtraction();
        assetExtraction4.setAsset(asset);
        assetExtraction4 = assetExtractionRepository.save(assetExtraction4);
        AssetTextUnit assetTextUnitForLeveraging4 = assetExtractionService.createAssetTextUnit(assetExtraction4, "TEST3A", "Content1B", "Comment3A");
        assetExtractionService.createAssetTextUnit(assetExtraction4, "TEST2A", "Content2A", "Comment2A");

        assetMappingService.mapAssetTextUnitAndCreateTMTextUnit(assetExtraction4.getId(), tm.getId(), asset.getId(), PollableTask.INJECT_CURRENT_TASK);
        assetExtractionService.markAssetExtractionAsLastSuccessful(asset, assetExtraction4);

        TMTextUnit tmTextUnitForLeveraging4 = tmTextUnitRepository.findFirstByTmAndMd5(tm, assetTextUnitForLeveraging4.getMd5());

        TextUnitSearcherParameters textUnitSearcherParameters4 = new TextUnitSearcherParameters();
        textUnitSearcherParameters4.setTmTextUnitId(tmTextUnitForLeveraging4.getId());
        textUnitSearcherParameters4.setStatusFilter(StatusFilter.TRANSLATED);
        textUnitSearcherParameters4.setUsedFilter(UsedFilter.USED);

        List<TextUnitDTO> textUnitDTOs4 = textUnitSearcher.search(textUnitSearcherParameters4);
        Iterator<TextUnitDTO> textUnitDTOsIt4 = textUnitDTOs4.iterator();
        TextUnitDTO translation4frFR = textUnitDTOsIt4.next();

        assertEquals("TEST3A", translation4frFR.getName());
        assertEquals("Content1B", translation4frFR.getSource());
        assertEquals("Comment3A", translation4frFR.getComment());
        assertEquals("Content1Afr-FR", translation4frFR.getTarget());
        assertEquals("The text unit used for leveraging needed review", TMTextUnitVariant.Status.TRANSLATION_NEEDED, translation4frFR.getStatus());

        comment = tmTextUnitVariantCommentRepository.findAllByTmTextUnitVariant_id(translation4frFR.getTmTextUnitVariantId()).get(2);
        assertEquals(TMTextUnitVariantComment.Type.LEVERAGING, comment.getType());
        assertEquals("by content for source leveraging leveraging from: " + translation3frFR.getTmTextUnitVariantId() + ", unique match: true", comment.getContent());

        TextUnitDTO translation4jaJP = textUnitDTOsIt4.next();

        assertEquals("TEST3A", translation4jaJP.getName());
        assertEquals("Content1B", translation4jaJP.getSource());
        assertEquals("Comment3A", translation4jaJP.getComment());
        assertEquals("Content1Aja-JP", translation4jaJP.getTarget());
        assertEquals("The text unit used for leveraging needed review", TMTextUnitVariant.Status.TRANSLATION_NEEDED, translation4jaJP.getStatus());

        comment = tmTextUnitVariantCommentRepository.findAllByTmTextUnitVariant_id(translation4jaJP.getTmTextUnitVariantId()).get(2);
        assertEquals(TMTextUnitVariantComment.Type.LEVERAGING, comment.getType());
        assertEquals("by content for source leveraging leveraging from: " + translation3jaJP.getTmTextUnitVariantId() + ", unique match: true", comment.getContent());

        assertFalse("There shouldn't be any more translations", textUnitDTOsIt4.hasNext());

        logger.debug("Perform mapping where some AssetTextUnit were change to test different kind of leveraging");

        AssetExtraction assetExtraction5 = new AssetExtraction();
        assetExtraction5.setAsset(asset);
        assetExtraction5 = assetExtractionRepository.save(assetExtraction5);
        AssetTextUnit assetTextUnitForLeveraging5 = assetExtractionService.createAssetTextUnit(assetExtraction5, "TEST3A", "Content3A", "Comment3A");
        AssetTextUnit assetTextUnitForLeveraging5b = assetExtractionService.createAssetTextUnit(assetExtraction5, "TEST4A", "Content1B", "Comment4A");
        AssetTextUnit assetTextUnitForLeveraging5c = assetExtractionService.createAssetTextUnit(assetExtraction5, "TEST2A", "Content1B", "Comment2A");

        assetMappingService.mapAssetTextUnitAndCreateTMTextUnit(assetExtraction5.getId(), tm.getId(), asset.getId(), PollableTask.INJECT_CURRENT_TASK);
        assetExtractionService.markAssetExtractionAsLastSuccessful(asset, assetExtraction5);

        TMTextUnit tmTextUnitForLeveraging5 = tmTextUnitRepository.findFirstByTmAndMd5(tm, assetTextUnitForLeveraging5.getMd5());
        TMTextUnit tmTextUnitForLeveraging5b = tmTextUnitRepository.findFirstByTmAndMd5(tm, assetTextUnitForLeveraging5b.getMd5());
        TMTextUnit tmTextUnitForLeveraging5c = tmTextUnitRepository.findFirstByTmAndMd5(tm, assetTextUnitForLeveraging5c.getMd5());

        TextUnitSearcherParameters textUnitSearcherParameters5 = new TextUnitSearcherParameters();
        textUnitSearcherParameters5.setTmTextUnitId(tmTextUnitForLeveraging5.getId());
        textUnitSearcherParameters5.setStatusFilter(StatusFilter.TRANSLATED);
        textUnitSearcherParameters5.setUsedFilter(UsedFilter.USED);

        List<TextUnitDTO> textUnitDTOs5 = textUnitSearcher.search(textUnitSearcherParameters5);
        Iterator<TextUnitDTO> textUnitDTOsIt5 = textUnitDTOs5.iterator();

        TextUnitDTO translation5frFR = textUnitDTOsIt5.next();

        assertEquals("TEST3A", translation5frFR.getName());
        assertEquals("Content3A", translation5frFR.getSource());
        assertEquals("Comment3A", translation5frFR.getComment());
        assertEquals("Content1Afr-FR", translation5frFR.getTarget());
        assertEquals("The source text has changed, hence it needs review", TMTextUnitVariant.Status.TRANSLATION_NEEDED, translation5frFR.getStatus());

        comment = tmTextUnitVariantCommentRepository.findAllByTmTextUnitVariant_id(translation5frFR.getTmTextUnitVariantId()).get(3);
        assertEquals(TMTextUnitVariantComment.Type.LEVERAGING, comment.getType());
        assertEquals("by name for source leveraging leveraging from: " + translation4frFR.getTmTextUnitVariantId() + ", unique match: true", comment.getContent());

        TextUnitDTO translation5jaJP = textUnitDTOsIt5.next();

        assertEquals("TEST3A", translation5jaJP.getName());
        assertEquals("Content3A", translation5jaJP.getSource());
        assertEquals("Comment3A", translation5jaJP.getComment());
        assertEquals("Content1Aja-JP", translation5jaJP.getTarget());
        assertEquals("The source text has changed, hence it needs review", TMTextUnitVariant.Status.TRANSLATION_NEEDED, translation5jaJP.getStatus());

        comment = tmTextUnitVariantCommentRepository.findAllByTmTextUnitVariant_id(translation5jaJP.getTmTextUnitVariantId()).get(3);
        assertEquals(TMTextUnitVariantComment.Type.LEVERAGING, comment.getType());
        assertEquals("by name for source leveraging leveraging from: " + translation4jaJP.getTmTextUnitVariantId() + ", unique match: true", comment.getContent());

        assertFalse("There shouldn't be any more translations", textUnitDTOsIt5.hasNext());

        TextUnitSearcherParameters textUnitSearcherParameters5b = new TextUnitSearcherParameters();
        textUnitSearcherParameters5b.setTmTextUnitId(tmTextUnitForLeveraging5b.getId());
        textUnitSearcherParameters5b.setStatusFilter(StatusFilter.TRANSLATED);
        textUnitSearcherParameters5b.setUsedFilter(UsedFilter.USED);

        List<TextUnitDTO> textUnitDTOs5b = textUnitSearcher.search(textUnitSearcherParameters5b);
        Iterator<TextUnitDTO> textUnitDTOsIt5b = textUnitDTOs5b.iterator();

        TextUnitDTO translation5frFRb = textUnitDTOsIt5b.next();

        assertEquals("TEST4A", translation5frFRb.getName());
        assertEquals("Content1B", translation5frFRb.getSource());
        assertEquals("Comment4A", translation5frFRb.getComment());
        assertEquals("Content1Afr-FR", translation5frFRb.getTarget());
        assertEquals(TMTextUnitVariant.Status.TRANSLATION_NEEDED, translation5frFRb.getStatus());

        comment = tmTextUnitVariantCommentRepository.findAllByTmTextUnitVariant_id(translation5frFRb.getTmTextUnitVariantId()).get(3);
        assertEquals(TMTextUnitVariantComment.Type.LEVERAGING, comment.getType());
        assertEquals("by content for source leveraging leveraging from: " + translation4frFR.getTmTextUnitVariantId() + ", unique match: true", comment.getContent());

        TextUnitDTO translation5jaJPb = textUnitDTOsIt5b.next();

        assertEquals("TEST4A", translation5jaJPb.getName());
        assertEquals("Content1B", translation5jaJPb.getSource());
        assertEquals("Comment4A", translation5jaJPb.getComment());
        assertEquals("Content1Aja-JP", translation5jaJPb.getTarget());
        assertEquals(TMTextUnitVariant.Status.TRANSLATION_NEEDED, translation5jaJPb.getStatus());

        comment = tmTextUnitVariantCommentRepository.findAllByTmTextUnitVariant_id(translation5jaJPb.getTmTextUnitVariantId()).get(3);
        assertEquals(TMTextUnitVariantComment.Type.LEVERAGING, comment.getType());
        assertEquals("by content for source leveraging leveraging from: " + translation4jaJP.getTmTextUnitVariantId() + ", unique match: true", comment.getContent());

        assertFalse("There shouldn't be any more translations", textUnitDTOsIt5b.hasNext());

        TextUnitSearcherParameters textUnitSearcherParameters5c = new TextUnitSearcherParameters();
        textUnitSearcherParameters5c.setTmTextUnitId(tmTextUnitForLeveraging5c.getId());
        textUnitSearcherParameters5c.setStatusFilter(StatusFilter.TRANSLATED);
        textUnitSearcherParameters5c.setUsedFilter(UsedFilter.USED);

        List<TextUnitDTO> textUnitDTOs5c = textUnitSearcher.search(textUnitSearcherParameters5c);
        Iterator<TextUnitDTO> textUnitDTOsIt5c = textUnitDTOs5c.iterator();

        TextUnitDTO translation5frFRc = textUnitDTOsIt5c.next();

        assertEquals("TEST2A", translation5frFRc.getName());
        assertEquals("Content1B", translation5frFRc.getSource());
        assertEquals("Comment2A", translation5frFRc.getComment());
        assertEquals("Content1Afr-FR", translation5frFRc.getTarget());
        assertEquals(TMTextUnitVariant.Status.TRANSLATION_NEEDED, translation5frFRc.getStatus());

        comment = tmTextUnitVariantCommentRepository.findAllByTmTextUnitVariant_id(translation5frFRc.getTmTextUnitVariantId()).get(3);
        assertEquals(TMTextUnitVariantComment.Type.LEVERAGING, comment.getType());
        assertEquals("by content for source leveraging leveraging from: " + translation4frFR.getTmTextUnitVariantId() + ", unique match: true", comment.getContent());

        TextUnitDTO translation5jaJPc = textUnitDTOsIt5c.next();

        assertEquals("TEST2A", translation5jaJPc.getName());
        assertEquals("Content1B", translation5jaJPc.getSource());
        assertEquals("Comment2A", translation5jaJPc.getComment());
        assertEquals("Content1Aja-JP", translation5jaJPc.getTarget());
        assertEquals(TMTextUnitVariant.Status.TRANSLATION_NEEDED, translation5jaJPc.getStatus());

        comment = tmTextUnitVariantCommentRepository.findAllByTmTextUnitVariant_id(translation5jaJPc.getTmTextUnitVariantId()).get(3);
        assertEquals(TMTextUnitVariantComment.Type.LEVERAGING, comment.getType());
        assertEquals("by content for source leveraging leveraging from: " + translation4jaJP.getTmTextUnitVariantId() + ", unique match: true", comment.getContent());

        assertFalse("There shouldn't be any more translations", textUnitDTOsIt5c.hasNext());

        logger.debug("Perform mapping where some AssetTextUnit were change to test different kind of leveraging");

        AssetExtraction assetExtraction6 = new AssetExtraction();
        assetExtraction6.setAsset(asset);
        assetExtraction6 = assetExtractionRepository.save(assetExtraction6);
        AssetTextUnit assetTextUnitForLeveraging6 = assetExtractionService.createAssetTextUnit(assetExtraction6, "TEST6A", "Content1B", "Comment6A");

        assetMappingService.mapAssetTextUnitAndCreateTMTextUnit(assetExtraction6.getId(), tm.getId(), asset.getId(), PollableTask.INJECT_CURRENT_TASK);
        assetExtractionService.markAssetExtractionAsLastSuccessful(asset, assetExtraction6);

        TMTextUnit tmTextUnitForLeveraging6 = tmTextUnitRepository.findFirstByTmAndMd5(tm, assetTextUnitForLeveraging6.getMd5());

        TextUnitSearcherParameters textUnitSearcherParameters6 = new TextUnitSearcherParameters();
        textUnitSearcherParameters6.setTmTextUnitId(tmTextUnitForLeveraging6.getId());
        textUnitSearcherParameters6.setStatusFilter(StatusFilter.TRANSLATED);
        textUnitSearcherParameters6.setUsedFilter(UsedFilter.USED);

        List<TextUnitDTO> textUnitDTOs6 = textUnitSearcher.search(textUnitSearcherParameters6);
        Iterator<TextUnitDTO> textUnitDTOsIt6 = textUnitDTOs6.iterator();

        TextUnitDTO translation6frFR = textUnitDTOsIt6.next();

        assertEquals("TEST6A", translation6frFR.getName());
        assertEquals("Content1B", translation6frFR.getSource());
        assertEquals("Comment6A", translation6frFR.getComment());
        assertEquals("Content1Afr-FR", translation6frFR.getTarget());
        assertEquals(TMTextUnitVariant.Status.TRANSLATION_NEEDED, translation6frFR.getStatus());

        comment = tmTextUnitVariantCommentRepository.findAllByTmTextUnitVariant_id(translation6frFR.getTmTextUnitVariantId()).get(4);
        assertEquals(TMTextUnitVariantComment.Type.LEVERAGING, comment.getType());
        assertEquals("by content for source leveraging leveraging from: " + translation5frFRb.getTmTextUnitVariantId() + ", unique match: false", comment.getContent());

        TextUnitDTO translation6jaJP = textUnitDTOsIt6.next();

        assertEquals("TEST6A", translation6jaJP.getName());
        assertEquals("Content1B", translation6jaJP.getSource());
        assertEquals("Comment6A", translation6jaJP.getComment());
        assertEquals("Content1Aja-JP", translation6jaJP.getTarget());
        assertEquals(TMTextUnitVariant.Status.TRANSLATION_NEEDED, translation6jaJP.getStatus());

        comment = tmTextUnitVariantCommentRepository.findAllByTmTextUnitVariant_id(translation6jaJP.getTmTextUnitVariantId()).get(4);
        assertEquals(TMTextUnitVariantComment.Type.LEVERAGING, comment.getType());
        assertEquals("by content for source leveraging leveraging from: " + translation5jaJPb.getTmTextUnitVariantId() + ", unique match: false", comment.getContent());

    }

    private void checkMapping(AssetTextUnitToTMTextUnit next, AssetExtraction assetExtraction, TM tm) {
        assertEquals("Check for mapping reccords", next.getAssetTextUnit().getName(), next.getTmTextUnit().getName());
        assertEquals("Check for mapping reccords", next.getAssetTextUnit().getContent(), next.getTmTextUnit().getContent());
        assertEquals("Check for mapping reccords", next.getAssetTextUnit().getComment(), next.getTmTextUnit().getComment());
        assertEquals("Check for mapping reccords", assetExtraction.getId(), next.getAssetTextUnit().getAssetExtraction().getId());
        assertEquals("Check for mapping reccords", next.getTmTextUnit().getTm().getId(), tm.getId());
    }

    private Iterator<AssetTextUnitToTMTextUnit> getAssetTextUnitToTMTextUnit(AssetExtraction assetExtraction) {
        TypedQuery<AssetTextUnitToTMTextUnit> query = entityManager.createQuery(
                "select m from AssetTextUnitToTMTextUnit m where m.assetTextUnit.assetExtraction.id = :assetExtractionId order by m.tmTextUnit.id asc", AssetTextUnitToTMTextUnit.class).setParameter("assetExtractionId", assetExtraction.getId());
        List<AssetTextUnitToTMTextUnit> assetTextUnitToTMTextUnits = query.getResultList();
        return assetTextUnitToTMTextUnits.iterator();
    }

    protected void createOtherTMAssetAndTextUnit(String repoName, String assetContent, String assetPath, String tuName, String tuContent, String tuComment) throws RepositoryNameAlreadyUsedException {
        Repository otherRepo = repositoryService.createRepository(testIdWatcher.getEntityName(repoName));
        TM otherTM = otherRepo.getTm();
        Asset otherAsset = assetService.createAssetWithContent(otherRepo.getId(), assetPath, assetContent);
        tmService.addTMTextUnit(otherTM.getId(), otherAsset.getId(), tuName, tuContent, tuComment);
    }

}
