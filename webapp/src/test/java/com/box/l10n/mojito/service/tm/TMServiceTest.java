package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariantComment;
import com.box.l10n.mojito.entity.TMXliff;
import com.box.l10n.mojito.okapi.ImportTranslationsFromLocalizedAssetStep.StatusForEqualTarget;
import com.box.l10n.mojito.okapi.InheritanceMode;
import com.box.l10n.mojito.okapi.Status;
import com.box.l10n.mojito.okapi.filters.MacStringsEncoder;
import com.box.l10n.mojito.okapi.filters.XMLEncoder;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.asset.AssetUpdateException;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskException;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.repository.RepositoryLocaleCreationException;
import com.box.l10n.mojito.service.repository.RepositoryLocaleRepository;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import net.sf.okapi.common.resource.TextUnit;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.proxy.HibernateProxy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * @author jaurambault
 */
public class TMServiceTest extends ServiceTestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(TMServiceTest.class);

    @Autowired
    TMService tmService;

    @Autowired
    TMRepository tmRepository;

    @Autowired
    TMTextUnitVariantRepository tmTextUnitVariantRepository;

    @Autowired
    TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;

    @Autowired
    TMTextUnitRepository tmTextUnitRepository;

    @Autowired
    LocaleService localeService;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    AssetService assetService;

    @Autowired
    TMTextUnitVariantCommentRepository tmTextUnitVariantCommentRepository;

    @Autowired
    TMTextUnitVariantCommentService tmTextUnitVariantCommentService;

    @Autowired
    RepositoryLocaleRepository repositoryLocaleRepository;

    @Autowired
    TextUnitSearcher textUnitSearcher;

    @Autowired
    PollableTaskService pollableTaskService;

    @Autowired
    TMXliffRepository tmXliffRepository;

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    Repository repository;
    Asset asset;
    Long tmId;
    Long assetId;

    protected void createTestData() throws RepositoryNameAlreadyUsedException {
        logger.debug("Create data for test");
        if (repository == null) {
            repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));

            try {
                repositoryService.addRepositoryLocale(repository, "fr-FR");
                repositoryService.addRepositoryLocale(repository, "fr-CA");
            } catch (RepositoryLocaleCreationException e) {
                throw new RuntimeException(e);
            }

            asset = assetService.createAsset(repository.getId(), "test asset content", "test-asset-path.xliff");

            //make sure asset and its relationships are loaded
            asset = assetRepository.findOne(asset.getId());

            assetId = asset.getId();
            tmId = repository.getTm().getId();
        }
    }

    @Test
    public void testAddTMTextUnit() throws RepositoryNameAlreadyUsedException {
        createTestData();

        logger.debug("Done creating data for test, start testing");

        logger.debug("Add a first text unit");
        Long addTextUnitAndCheck1 = addTextUnitAndCheck(tmId, assetId, "name", "this is the content", "some comment", "3063c39d3cf8ab69bcabbbc5d7187dc9", "cf8ea6b6848f23345648038bc3abf324");

        logger.debug("Try to add a second text unit with same logical key, throws a DataIntegrityViolationException");
        try {
            tmService.addTMTextUnit(tmId, assetId, "name", "this is the content", "some comment");
            fail();
        } catch (DataIntegrityViolationException e) {
            logger.debug("expected data integrity violation exception");
        }

        logger.debug("Add the second text unit");
        Long addTextUnitAndCheck2 = addTextUnitAndCheck(tmId, assetId, "name2", "content", "comment", "d00c1170937aa79458be2424f4d9720e", "9a0364b9e99bb480dd25e1f0284c8555");

        logger.debug("Check the text units");
        Iterator<TMTextUnit> tmTextUnitIterator = tmTextUnitRepository.findByTm_id(tmId).iterator();
        assertEquals(addTextUnitAndCheck1, tmTextUnitIterator.next().getId());
        assertEquals(addTextUnitAndCheck2, tmTextUnitIterator.next().getId());
        assertFalse(tmTextUnitIterator.hasNext());

        logger.debug("Check the text units variants for the default locale");
        Iterator<TMTextUnitVariant> tmTextUnitVariantIterator = tmTextUnitVariantRepository.findAllByLocale_IdAndTmTextUnit_Tm_id(localeService.getDefaultLocaleId(), tmId).iterator();
        assertEquals("this is the content", tmTextUnitVariantIterator.next().getContent());
        assertEquals("content", tmTextUnitVariantIterator.next().getContent());
        assertFalse(tmTextUnitVariantIterator.hasNext());
    }

    private Long addTextUnitAndCheck(Long tmId, Long assetId, String name, String content, String comment, String md5Check, String contentMd5Check) {
        TMTextUnit addTMTextUnit = tmService.addTMTextUnit(tmId, assetId, name, content, comment);

        assertNotNull(addTMTextUnit.getId());
        assertEquals(name, addTMTextUnit.getName());
        assertEquals(content, addTMTextUnit.getContent());
        assertEquals(comment, addTMTextUnit.getComment());
        assertEquals(md5Check, addTMTextUnit.getMd5());
        assertEquals(contentMd5Check, addTMTextUnit.getContentMd5());
        assertEquals(tmId, ((HibernateProxy) addTMTextUnit.getTm()).getHibernateLazyInitializer().getIdentifier());
        assertNotNull(addTMTextUnit.getCreatedByUser());

        return addTMTextUnit.getId();
    }

    @Test
    public void testAddTMTextUnitInvalidInput() {
        try {
            tmService.addTMTextUnit(-15L, -1L, "fail", "fail", "fail");
            fail();
        } catch (DataIntegrityViolationException e) {
            logger.debug("expected data integrity violation exception");
        }
    }

    @Test
    public void testAddCurrentTMTextUnitVariant() throws RepositoryNameAlreadyUsedException {
        createTestData();

        String name = "name";
        String content = "this is the content";
        String comment = "some comment";
        TMTextUnit addTMTextUnit = tmService.addTMTextUnit(tmId, assetId, name, content, comment);

        logger.debug("Done creating data for test, start testing");

        Locale frFRLocale = localeService.findByBcp47Tag("fr-FR");

        logger.debug("Add a current translation for french");
        TMTextUnitVariant addCurrentTMTextUnitVariant = addCurrentTMTextUnitVariant(addTMTextUnit.getId(), frFRLocale.getId(), "FR[this is the content]", "0a30a359b20fd4095fc17fb586e8db4d");

        logger.debug("Add the same content, it should be skipped and return previous TMTextUnitVariant");
        TMTextUnitVariant addCurrentTMTextUnitVariantSkipped = tmService.addCurrentTMTextUnitVariant(addTMTextUnit.getId(), frFRLocale.getId(), "FR[this is the content]");
        assertEquals(addCurrentTMTextUnitVariant.getId(), addCurrentTMTextUnitVariantSkipped.getId());

        logger.debug("Add a different content, it should be added");
        TMTextUnitVariant addCurrentTMTextUnitVariant1 = addCurrentTMTextUnitVariant(addTMTextUnit.getId(), frFRLocale.getId(), "FR[this is the content 2]", "499bd8403c69151a7a21bf756f57183b");

        logger.debug("Go back to a translation with same content as previous version, should add entities");
        TMTextUnitVariant addCurrentTMTextUnitVariant2 = addCurrentTMTextUnitVariant(addTMTextUnit.getId(), frFRLocale.getId(), "FR[this is the content]", "0a30a359b20fd4095fc17fb586e8db4d");

        Iterator<TMTextUnitVariant> tmTextUnitVariantIteratorFr = tmTextUnitVariantRepository.findAllByLocale_IdAndTmTextUnit_Tm_id(frFRLocale.getId(), tmId).iterator();
        assertEquals(addCurrentTMTextUnitVariant.getId(), tmTextUnitVariantIteratorFr.next().getId());
        assertEquals(addCurrentTMTextUnitVariant1.getId(), tmTextUnitVariantIteratorFr.next().getId());
        assertEquals(addCurrentTMTextUnitVariant2.getId(), tmTextUnitVariantIteratorFr.next().getId());
        assertFalse(tmTextUnitVariantIteratorFr.hasNext());

        Locale frCALocale = localeService.findByBcp47Tag("fr-CA");

        logger.debug("Add a current translation for french France with same content as the french translation");
        TMTextUnitVariant addCurrentTMTextUnitVariant3 = addCurrentTMTextUnitVariant(addTMTextUnit.getId(), frCALocale.getId(), "FR[this is the content]", "0a30a359b20fd4095fc17fb586e8db4d");

        Iterator<TMTextUnitVariant> tmTextUnitVariantIteratorFrFR = tmTextUnitVariantRepository.findAllByLocale_IdAndTmTextUnit_Tm_id(frCALocale.getId(), tmId).iterator();
        assertEquals(addCurrentTMTextUnitVariant3.getId(), tmTextUnitVariantIteratorFrFR.next().getId());

    }

    private TMTextUnitVariant addCurrentTMTextUnitVariant(Long tmTextUnitId, Long localeId, String content, String contentMD5) {
        TMTextUnitVariant addCurrentTMTextUnitVariant = tmService.addCurrentTMTextUnitVariant(tmTextUnitId, localeId, content);
        assertEquals(content, addCurrentTMTextUnitVariant.getContent());
        assertEquals(contentMD5, addCurrentTMTextUnitVariant.getContentMD5());
        assertEquals(localeId, ((HibernateProxy) addCurrentTMTextUnitVariant.getLocale()).getHibernateLazyInitializer().getIdentifier());
        assertEquals(tmTextUnitId, ((HibernateProxy) addCurrentTMTextUnitVariant.getTmTextUnit()).getHibernateLazyInitializer().getIdentifier());
        assertNotNull(addCurrentTMTextUnitVariant.getCreatedByUser());
        return addCurrentTMTextUnitVariant;
    }

    @Test
    public void testComputeTMTextUnitMD5() throws IOException {
        String computeTMTextUnitMD5 = tmService.computeTMTextUnitMD5("name", "this is the content", "some comment");
        assertEquals("3063c39d3cf8ab69bcabbbc5d7187dc9", computeTMTextUnitMD5);
    }
    
    @Test
    public void testComputeTMTextUnitMD5Null() throws IOException {
        String computeTMTextUnitMD5 = tmService.computeTMTextUnitMD5(null, "this is the content", null);
        assertEquals("ad549ec93687843d638c9a712dff0238", computeTMTextUnitMD5);
    }

    @Test
    public void testUpdateTMWithLocalizedXLIFFWithNewVariants() throws RepositoryNameAlreadyUsedException {
        createTestData();

        TMTextUnit tmTextUnit1 = tmService.addTMTextUnit(tmId, assetId, "application_name", "Application name", "This text is shown in the start screen of the application. Keep it short.");
        TMTextUnit tmTextUnit2 = tmService.addTMTextUnit(tmId, assetId, "home", "Home", "This is the text displayed in the link that takes the user to the home page.");
        Locale targetLocale = localeService.findByBcp47Tag("fr-FR");

        String localizedXLIFFContents = getLocalizedXLIFFContent(targetLocale, tmTextUnit1, tmTextUnit2);

        List<TMTextUnitVariant> tmTextUnitVariants = tmTextUnitVariantRepository.findAllByLocale_IdAndTmTextUnit_Tm_id(targetLocale.getId(), tmId);
        assertTrue(tmTextUnitVariants.isEmpty());

        tmService.updateTMWithXLIFFById(localizedXLIFFContents, null);

        tmTextUnitVariants = tmTextUnitVariantRepository.findAllByLocale_IdAndTmTextUnit_Tm_id(targetLocale.getId(), tmId);
        assertEquals("2 variants should have been added", 2, tmTextUnitVariants.size());
        assertEquals("Nom de l'application", tmTextUnitVariants.get(0).getContent());
        assertEquals("Accueil", tmTextUnitVariants.get(1).getContent());

        TMTextUnitCurrentVariant tmTextUnitCurrentVariant1 = tmTextUnitCurrentVariantRepository.findByLocale_IdAndTmTextUnit_Id(targetLocale.getId(), tmTextUnit1.getId());
        TMTextUnitCurrentVariant tmTextUnitCurrentVariant2 = tmTextUnitCurrentVariantRepository.findByLocale_IdAndTmTextUnit_Id(targetLocale.getId(), tmTextUnit2.getId());
        assertEquals(tmTextUnitVariants.get(0).getId(), tmTextUnitCurrentVariant1.getTmTextUnitVariant().getId());
        assertEquals(tmTextUnitVariants.get(1).getId(), tmTextUnitCurrentVariant2.getTmTextUnitVariant().getId());
    }

    @Test
    public void testUpdateTMWithLocalizedXLIFFWithExistingVariants() throws RepositoryNameAlreadyUsedException {
        createTestData();

        TMTextUnit tmTextUnit1 = tmService.addTMTextUnit(tmId, assetId, "application_name", "Application name", "This text is shown in the start screen of the application. Keep it short.");
        TMTextUnit tmTextUnit2 = tmService.addTMTextUnit(tmId, assetId, "home", "Home", "This is the text displayed in the link that takes the user to the home page.");
        Locale targetLocale = localeService.findByBcp47Tag("fr-FR");

        String localizedXLIFFContents = getLocalizedXLIFFContent(targetLocale, tmTextUnit1, tmTextUnit2);

        List<TMTextUnit> tmTextUnits = tmTextUnitRepository.findByTm_id(tmId);
        for (TMTextUnit tmTextUnit : tmTextUnits) {
            tmService.addCurrentTMTextUnitVariant(tmTextUnit.getId(), targetLocale.getId(), "Existing translation...");
        }

        List<TMTextUnitVariant> tmTextUnitVariants = tmTextUnitVariantRepository.findAllByLocale_IdAndTmTextUnit_Tm_id(targetLocale.getId(), tmId);
        assertEquals("There should already be 2 variants", 2, tmTextUnitVariants.size());
        assertEquals("Existing translation...", tmTextUnitVariants.get(0).getContent());

        tmService.updateTMWithXLIFFById(localizedXLIFFContents, null);

        tmTextUnitVariants = tmTextUnitVariantRepository.findAllByLocale_IdAndTmTextUnit_Tm_id(targetLocale.getId(), tmId);
        assertEquals("2 variants should have been added (so 4 now)", 4, tmTextUnitVariants.size());

        // Sort tmTextUnitVariants list by ID to easily get new variants
        Collections.sort(tmTextUnitVariants, new Comparator<TMTextUnitVariant>() {
            @Override
            public int compare(final TMTextUnitVariant tmTextUnitVariant1, final TMTextUnitVariant tmTextUnitVariant2) {
                return tmTextUnitVariant1.getId().compareTo(tmTextUnitVariant2.getId());
            }
        });
        assertEquals("Nom de l'application", tmTextUnitVariants.get(2).getContent());
        assertEquals("Accueil", tmTextUnitVariants.get(3).getContent());

        TMTextUnitCurrentVariant tmTextUnitCurrentVariant1 = tmTextUnitCurrentVariantRepository.findByLocale_IdAndTmTextUnit_Id(targetLocale.getId(), tmTextUnit1.getId());
        TMTextUnitCurrentVariant tmTextUnitCurrentVariant2 = tmTextUnitCurrentVariantRepository.findByLocale_IdAndTmTextUnit_Id(targetLocale.getId(), tmTextUnit2.getId());
        assertEquals(tmTextUnitVariants.get(2).getId(), tmTextUnitCurrentVariant1.getTmTextUnitVariant().getId());
        assertEquals(tmTextUnitVariants.get(3).getId(), tmTextUnitCurrentVariant2.getTmTextUnitVariant().getId());
    }

    @Test
    public void testUpdateTMWithLocalizedXLIFFWithQualityCheckErrors() throws RepositoryNameAlreadyUsedException {
        createTestData();

        TMTextUnit tmTextUnit1 = tmService.addTMTextUnit(tmId, assetId, "application_name", "Application name name", "This text is shown in the start screen of the application. Keep it short.");
        TMTextUnit tmTextUnit2 = tmService.addTMTextUnit(tmId, assetId, "home", "Home", "This is the text displayed in the link that takes the user to the home page.");
        Locale targetLocale = localeService.findByBcp47Tag("fr-FR");

        List<TMTextUnit> tmTextUnits = tmTextUnitRepository.findByTm_id(tmId);
        for (TMTextUnit tmTextUnit : tmTextUnits) {
            tmService.addCurrentTMTextUnitVariant(tmTextUnit.getId(), targetLocale.getId(), "Existing translation...");
        }

        List<TMTextUnitVariant> tmTextUnitVariants = tmTextUnitVariantRepository.findAllByLocale_IdAndTmTextUnit_Tm_id(targetLocale.getId(), tmId);
        assertEquals("There should already be 2 variants", 2, tmTextUnitVariants.size());
        assertEquals("Existing translation...", tmTextUnitVariants.get(0).getContent());

        String targetBcp47Tag = targetLocale.getBcp47Tag();
        String localizedXLIFFContents = xliffDataFactory.generateTargetXliff(Arrays.asList(
                xliffDataFactory.createTextUnit(tmTextUnit1.getId(), tmTextUnit1.getName(), tmTextUnit1.getContent(), tmTextUnit1.getComment(), "Application name name", targetBcp47Tag, null), // doubled word + source == target
                xliffDataFactory.createTextUnit(tmTextUnit2.getId(), tmTextUnit2.getName(), tmTextUnit2.getContent(), tmTextUnit2.getComment(), "", targetBcp47Tag, null) // empty translation
        ), targetBcp47Tag);
        tmService.updateTMWithXLIFFById(localizedXLIFFContents, null);

        tmTextUnitVariants = tmTextUnitVariantRepository.findAllByLocale_IdAndTmTextUnit_Tm_id(targetLocale.getId(), tmId);
        assertEquals("2 variants should have been added (so 4 now)", 4, tmTextUnitVariants.size());

        // Sort tmTextUnitVariants list by ID to easily get new variants
        Collections.sort(tmTextUnitVariants, new Comparator<TMTextUnitVariant>() {
            @Override
            public int compare(final TMTextUnitVariant tmTextUnitVariant1, final TMTextUnitVariant tmTextUnitVariant2) {
                return tmTextUnitVariant1.getId().compareTo(tmTextUnitVariant2.getId());
            }
        });

        TMTextUnitVariant tmTextUnitVariant3 = tmTextUnitVariants.get(2);
        TMTextUnitVariant tmTextUnitVariant4 = tmTextUnitVariants.get(3);
        assertEquals("Application name name", tmTextUnitVariant3.getContent());
        assertEquals("", tmTextUnitVariant4.getContent());

        assertEquals(TMTextUnitVariant.Status.REVIEW_NEEDED, tmTextUnitVariant3.getStatus());
        assertEquals(TMTextUnitVariant.Status.REVIEW_NEEDED, tmTextUnitVariant4.getStatus());
        assertTrue(tmTextUnitVariant3.isIncludedInLocalizedFile());
        assertTrue(tmTextUnitVariant4.isIncludedInLocalizedFile());

        List<TMTextUnitVariantComment> variant3Comments = tmTextUnitVariantCommentRepository.findAllByTmTextUnitVariant_id(tmTextUnitVariant3.getId());
        assertEquals("There should be 2 comments associated to the variant 3", 2, variant3Comments.size());
        for (TMTextUnitVariantComment variantComment : variant3Comments) {
            assertEquals(TMTextUnitVariantComment.Type.QUALITY_CHECK, variantComment.getType());
            assertEquals(TMTextUnitVariantComment.Severity.WARNING, variantComment.getSeverity());
        }

        List<TMTextUnitVariantComment> variant4Comments = tmTextUnitVariantCommentRepository.findAllByTmTextUnitVariant_id(tmTextUnitVariant4.getId());
        assertEquals("There should be 1 comment associated to the variant 4", 1, variant4Comments.size());
        assertEquals(TMTextUnitVariantComment.Type.QUALITY_CHECK, variant4Comments.get(0).getType());
        assertEquals(TMTextUnitVariantComment.Severity.WARNING, variant4Comments.get(0).getSeverity());
    }

    private String getLocalizedXLIFFContent(Locale targetLocale, TMTextUnit tmTextUnit1, TMTextUnit tmTextUnit2) {

        String targetBcp47Tag = targetLocale.getBcp47Tag();

        return xliffDataFactory.generateTargetXliff(Arrays.asList(
                xliffDataFactory.createTextUnit(tmTextUnit1.getId(), tmTextUnit1.getName(), tmTextUnit1.getContent(), tmTextUnit1.getComment(), "Nom de l'application", targetBcp47Tag, null),
                xliffDataFactory.createTextUnit(tmTextUnit2.getId(), tmTextUnit2.getName(), tmTextUnit2.getContent(), tmTextUnit2.getComment(), "Accueil", targetBcp47Tag, null)
        ), targetBcp47Tag);
    }

    @Test
    public void testGenerateLocalizedXLIFF() throws RepositoryNameAlreadyUsedException {

        createTestData();

        TMTextUnit tmTextUnit1 = tmService.addTMTextUnit(tmId, assetId, "application_name", "Application Name", "This text is shown in the start screen of the application. Keep it short.");
        TMTextUnit tmTextUnit2 = tmService.addTMTextUnit(tmId, assetId, "home", "Home", "This is the text displayed in the link that takes the user to the home page.");
        TMTextUnit tmTextUnit3 = tmService.addTMTextUnit(tmId, assetId, "fail_integrity_check", "I fail integrity check", null);

        RepositoryLocale repositoryLocale = repositoryLocaleRepository.findByRepositoryAndLocale_Bcp47Tag(repository, "fr-FR");
        Locale locale = repositoryLocale.getLocale();

        tmService.addCurrentTMTextUnitVariant(tmTextUnit1.getId(), locale.getId(), "Nom de l'app");
        TMTextUnitVariant variant1 = tmService.addCurrentTMTextUnitVariant(tmTextUnit1.getId(), locale.getId(), "Nom de l'application");

        // Adding current variant that failed integrity checks (should not be included in localized XLIFF)
        //TODO(P1) need to save in comments
        tmService.addTMTextUnitCurrentVariant(tmTextUnit3.getId(), locale.getId(), "!?!?!?!?!", null, TMTextUnitVariant.Status.REVIEW_NEEDED, false);

        String sourceXLIFF = getSourceXLIFFContent(Lists.newArrayList(tmTextUnit1, tmTextUnit2, tmTextUnit3));
        String localizedAsset = tmService.generateLocalized(asset, sourceXLIFF, repositoryLocale, null, null, InheritanceMode.USE_PARENT, Status.ALL);

        String expectedLocalizedXLIFF = getExpectedLocalizedXLIFFContent(locale.getBcp47Tag(), tmTextUnit1, tmTextUnit2, tmTextUnit3, variant1);
        assertEquals(
                removeLeadingAndTrailingSpacesOnEveryLine(expectedLocalizedXLIFF),
                removeLeadingAndTrailingSpacesOnEveryLine(localizedAsset)
        );
    }

    @Test
    public void testGenerateLocalizedXLIFFRemoveUntranslated() throws RepositoryNameAlreadyUsedException {

        createTestData();

        TMTextUnit tmTextUnit1 = tmService.addTMTextUnit(tmId, assetId, "application_name", "Application Name", "This text is shown in the start screen of the application. Keep it short.");
        TMTextUnit tmTextUnit2 = tmService.addTMTextUnit(tmId, assetId, "home", "Home", "This is the text displayed in the link that takes the user to the home page.");
        TMTextUnit tmTextUnit3 = tmService.addTMTextUnit(tmId, assetId, "fail_integrity_check", "I fail integrity check", null);

        RepositoryLocale repositoryLocale = repositoryLocaleRepository.findByRepositoryAndLocale_Bcp47Tag(repository, "fr-FR");
        Locale locale = repositoryLocale.getLocale();

        tmService.addCurrentTMTextUnitVariant(tmTextUnit1.getId(), locale.getId(), "Nom de l'app");
        TMTextUnitVariant variant1 = tmService.addCurrentTMTextUnitVariant(tmTextUnit1.getId(), locale.getId(), "Nom de l'application", TMTextUnitVariant.Status.REVIEW_NEEDED, true);

        TMTextUnitVariant variant2 = tmService.addCurrentTMTextUnitVariant(tmTextUnit2.getId(), locale.getId(), "Maison",  TMTextUnitVariant.Status.REVIEW_NEEDED,true);

        // Adding current variant that failed integrity checks (should not be included in localized XLIFF)
        //TODO(P1) need to save in comments
        tmService.addTMTextUnitCurrentVariant(tmTextUnit3.getId(), locale.getId(), "!?!?!?!?!", null, TMTextUnitVariant.Status.REVIEW_NEEDED, false);

        String sourceXLIFF = getSourceXLIFFContent(Lists.newArrayList(tmTextUnit1, tmTextUnit2, tmTextUnit3));
        String localizedAsset = tmService.generateLocalized(asset, sourceXLIFF, repositoryLocale, null, null, InheritanceMode.REMOVE_UNTRANSLATED, Status.ALL);

        String expectedLocalizedXLIFF = getExpectedLocalizedXLIFFContent(locale.getBcp47Tag(), newTmTextUnitWithVariant(tmTextUnit1, variant1), newTmTextUnitWithVariant(tmTextUnit2, variant2));
        assertEquals(
                removeLeadingAndTrailingSpacesOnEveryLine(expectedLocalizedXLIFF),
                removeLeadingAndTrailingSpacesOnEveryLine(localizedAsset)
        );
    }

    @Test
    public void testGenerateLocalizedXLIFFRemoveUntranslatedOnlyApproved() throws RepositoryNameAlreadyUsedException {

        createTestData();

        TMTextUnit tmTextUnit1 = tmService.addTMTextUnit(tmId, assetId, "application_name", "Application Name", "This text is shown in the start screen of the application. Keep it short.");
        TMTextUnit tmTextUnit2 = tmService.addTMTextUnit(tmId, assetId, "home", "Home", "This is the text displayed in the link that takes the user to the home page.");

        RepositoryLocale repositoryLocale = repositoryLocaleRepository.findByRepositoryAndLocale_Bcp47Tag(repository, "fr-FR");
        Locale locale = repositoryLocale.getLocale();

        tmService.addCurrentTMTextUnitVariant(tmTextUnit1.getId(), locale.getId(), "Nom de l'app");
        TMTextUnitVariant variant1 = tmService.addCurrentTMTextUnitVariant(tmTextUnit1.getId(), locale.getId(), "Nom de l'application",  TMTextUnitVariant.Status.APPROVED,true);

        TMTextUnitVariant variant2 = tmService.addCurrentTMTextUnitVariant(tmTextUnit2.getId(), locale.getId(), "Maison",  TMTextUnitVariant.Status.REVIEW_NEEDED,true);

        String sourceXLIFF = getSourceXLIFFContent(Lists.newArrayList(tmTextUnit1, tmTextUnit2));
        String localizedAsset = tmService.generateLocalized(asset, sourceXLIFF, repositoryLocale, null, null, InheritanceMode.REMOVE_UNTRANSLATED, Status.ACCEPTED);

        String expectedLocalizedXLIFF = getExpectedLocalizedXLIFFContent(locale.getBcp47Tag(), newTmTextUnitWithVariant(tmTextUnit1, variant1));
        assertEquals(
                removeLeadingAndTrailingSpacesOnEveryLine(expectedLocalizedXLIFF),
                removeLeadingAndTrailingSpacesOnEveryLine(localizedAsset)
        );
    }

    @Test
    public void testGenerateLocalizedXLIFFRemoveUntranslatedApprovedOrNeedsReview() throws RepositoryNameAlreadyUsedException {

        createTestData();

        TMTextUnit tmTextUnit1 = tmService.addTMTextUnit(tmId, assetId, "application_name", "Application Name", "This text is shown in the start screen of the application. Keep it short.");
        TMTextUnit tmTextUnit2 = tmService.addTMTextUnit(tmId, assetId, "home", "Home", "This is the text displayed in the link that takes the user to the home page.");
        TMTextUnit tmTextUnit3 = tmService.addTMTextUnit(tmId, assetId, "test", "Not approved", null);

        RepositoryLocale repositoryLocale = repositoryLocaleRepository.findByRepositoryAndLocale_Bcp47Tag(repository, "fr-FR");
        Locale locale = repositoryLocale.getLocale();

        tmService.addCurrentTMTextUnitVariant(tmTextUnit1.getId(), locale.getId(), "Nom de l'app");
        TMTextUnitVariant variant1 = tmService.addCurrentTMTextUnitVariant(tmTextUnit1.getId(), locale.getId(), "Nom de l'application",  TMTextUnitVariant.Status.APPROVED,true);

        TMTextUnitVariant variant2 = tmService.addCurrentTMTextUnitVariant(tmTextUnit2.getId(), locale.getId(), "Maison",  TMTextUnitVariant.Status.REVIEW_NEEDED,true);

        TMTextUnitVariant variant3 = tmService.addCurrentTMTextUnitVariant(tmTextUnit3.getId(), locale.getId(), "Non approuve",  TMTextUnitVariant.Status.TRANSLATION_NEEDED,true);

        String sourceXLIFF = getSourceXLIFFContent(Lists.newArrayList(tmTextUnit1, tmTextUnit2, tmTextUnit3));
        String localizedAsset = tmService.generateLocalized(asset, sourceXLIFF, repositoryLocale, null, null, InheritanceMode.REMOVE_UNTRANSLATED, Status.ACCEPTED_OR_NEEDS_REVIEW);

        String expectedLocalizedXLIFF = getExpectedLocalizedXLIFFContent(locale.getBcp47Tag(), newTmTextUnitWithVariant(tmTextUnit1, variant1), newTmTextUnitWithVariant(tmTextUnit2, variant2));
        assertEquals(
                removeLeadingAndTrailingSpacesOnEveryLine(expectedLocalizedXLIFF),
                removeLeadingAndTrailingSpacesOnEveryLine(localizedAsset)
        );
    }

    @Test
    public void testGenerateLocalizedXLIFFRemoveUntranslatedAll() throws RepositoryNameAlreadyUsedException {

        createTestData();

        TMTextUnit tmTextUnit1 = tmService.addTMTextUnit(tmId, assetId, "application_name", "Application Name", "This text is shown in the start screen of the application. Keep it short.");
        TMTextUnit tmTextUnit2 = tmService.addTMTextUnit(tmId, assetId, "home", "Home", "This is the text displayed in the link that takes the user to the home page.");
        TMTextUnit tmTextUnit3 = tmService.addTMTextUnit(tmId, assetId, "test", "Not approved", null);

        RepositoryLocale repositoryLocale = repositoryLocaleRepository.findByRepositoryAndLocale_Bcp47Tag(repository, "fr-FR");
        Locale locale = repositoryLocale.getLocale();

        tmService.addCurrentTMTextUnitVariant(tmTextUnit1.getId(), locale.getId(), "Nom de l'app");
        TMTextUnitVariant variant1 = tmService.addCurrentTMTextUnitVariant(tmTextUnit1.getId(), locale.getId(), "Nom de l'application",  TMTextUnitVariant.Status.APPROVED,true);

        TMTextUnitVariant variant2 = tmService.addCurrentTMTextUnitVariant(tmTextUnit2.getId(), locale.getId(), "Maison",  TMTextUnitVariant.Status.REVIEW_NEEDED,true);

        TMTextUnitVariant variant3 = tmService.addCurrentTMTextUnitVariant(tmTextUnit3.getId(), locale.getId(), "Non approuve",  TMTextUnitVariant.Status.TRANSLATION_NEEDED,true);

        String sourceXLIFF = getSourceXLIFFContent(Lists.newArrayList(tmTextUnit1, tmTextUnit2, tmTextUnit3));
        String localizedAsset = tmService.generateLocalized(asset, sourceXLIFF, repositoryLocale, null, null, InheritanceMode.REMOVE_UNTRANSLATED, Status.ALL);

        String expectedLocalizedXLIFF = getExpectedLocalizedXLIFFContent(locale.getBcp47Tag(), newTmTextUnitWithVariant(tmTextUnit1, variant1),
                newTmTextUnitWithVariant(tmTextUnit2, variant2), newTmTextUnitWithVariant(tmTextUnit3, variant3));
        assertEquals(
                removeLeadingAndTrailingSpacesOnEveryLine(expectedLocalizedXLIFF),
                removeLeadingAndTrailingSpacesOnEveryLine(localizedAsset)
        );
    }


    @Test
    public void testGenerateLocalizedXLIFFWithDifferentOutputTag() throws RepositoryNameAlreadyUsedException {

        createTestData();

        TMTextUnit tmTextUnit1 = tmService.addTMTextUnit(tmId, assetId, "application_name", "Application Name", "This text is shown in the start screen of the application. Keep it short.");
        TMTextUnit tmTextUnit2 = tmService.addTMTextUnit(tmId, assetId, "home", "Home", "This is the text displayed in the link that takes the user to the home page.");
        TMTextUnit tmTextUnit3 = tmService.addTMTextUnit(tmId, assetId, "fail_integrity_check", "I fail integrity check", null);

        RepositoryLocale repositoryLocale = repositoryLocaleRepository.findByRepositoryAndLocale_Bcp47Tag(repository, "fr-FR");
        Locale locale = repositoryLocale.getLocale();

        tmService.addCurrentTMTextUnitVariant(tmTextUnit1.getId(), locale.getId(), "Nom de l'app");
        TMTextUnitVariant variant1 = tmService.addCurrentTMTextUnitVariant(tmTextUnit1.getId(), locale.getId(), "Nom de l'application");

        // Adding current variant that failed integrity checks (should not be included in localized XLIFF)
        //TODO(P1) need to save in comments
        tmService.addTMTextUnitCurrentVariant(tmTextUnit3.getId(), locale.getId(), "!?!?!?!?!", null, TMTextUnitVariant.Status.REVIEW_NEEDED, false);

        String sourceXLIFF = getSourceXLIFFContent(Lists.newArrayList(tmTextUnit1, tmTextUnit2, tmTextUnit3));

        String outputBcp47tag = "fr-FR";
        String localizedAsset = tmService.generateLocalized(asset, sourceXLIFF, repositoryLocale, outputBcp47tag, null, InheritanceMode.USE_PARENT, Status.ALL);

        String expectedLocalizedXLIFF = getExpectedLocalizedXLIFFContent(outputBcp47tag, tmTextUnit1, tmTextUnit2, tmTextUnit3, variant1);
        assertEquals(
                removeLeadingAndTrailingSpacesOnEveryLine(expectedLocalizedXLIFF),
                removeLeadingAndTrailingSpacesOnEveryLine(localizedAsset)
        );
    }

    private String getSourceXLIFFContent(List<TMTextUnit> tmTextUnits) {

        List<TextUnit> textUnits = new ArrayList<>();
        for (TMTextUnit tmTextUnit : tmTextUnits) {
            textUnits.add(xliffDataFactory.createTextUnit(
                    tmTextUnit.getId(),
                    tmTextUnit.getName(),
                    tmTextUnit.getContent(),
                    tmTextUnit.getComment())
            );
        }

        return xliffDataFactory.generateSourceXliff(textUnits);
    }

    private String getExpectedLocalizedXLIFFContent(String targetLocaleBcp47Tag,
            TMTextUnit tmTextUnit1, TMTextUnit tmTextUnit2, TMTextUnit tmTextUnit3,
            TMTextUnitVariant variant1) {

        return getExpectedLocalizedXLIFFContent(targetLocaleBcp47Tag, newTmTextUnitWithVariant(tmTextUnit1, variant1), newTmTextUnitWithoutVariant(tmTextUnit2), newTmTextUnitWithoutVariant(tmTextUnit3));
    }

    private Pair<TMTextUnit, TMTextUnitVariant> newTmTextUnitWithoutVariant(TMTextUnit tmTextUnit) {
        return newTmTextUnitWithVariant(tmTextUnit, null);
    }

    private Pair<TMTextUnit, TMTextUnitVariant> newTmTextUnitWithVariant(TMTextUnit tmTextUnit, TMTextUnitVariant tmTextUnitVariant) {
        return new ImmutablePair<>(tmTextUnit, tmTextUnitVariant);
    }

    private String getExpectedLocalizedXLIFFContent(final String targetLocaleBcp47Tag,
                                                    Pair<TMTextUnit, TMTextUnitVariant>... tmTextUnitsWithVariants) {

        Function<Pair<TMTextUnit, TMTextUnitVariant>, TextUnit> toTextUnitFunction = new Function<Pair<TMTextUnit, TMTextUnitVariant>, TextUnit>() {
            @Override
            public TextUnit apply(Pair<TMTextUnit, TMTextUnitVariant> input) {
                return createTextUnitFromTmTextUnitsWithVariant(targetLocaleBcp47Tag, input.getLeft(), input.getRight());
            }
        };

        List<Pair<TMTextUnit, TMTextUnitVariant>> tmTextUnitWithVariantList = Arrays.asList(tmTextUnitsWithVariants);

        List<TextUnit> textUnitList = FluentIterable.from(tmTextUnitWithVariantList).transform(toTextUnitFunction).toList();

        return xliffDataFactory.generateTargetXliff(textUnitList, targetLocaleBcp47Tag);
    }

    private TextUnit createTextUnitFromTmTextUnitsWithVariant(String targetLocaleBcp47Tag, TMTextUnit tmTextUnit, TMTextUnitVariant tmTextUnitVariant) {
        if (tmTextUnitVariant == null) {
            return xliffDataFactory.createTextUnit(tmTextUnit.getId(), tmTextUnit.getName(), tmTextUnit.getContent(), tmTextUnit.getComment(), tmTextUnit.getContent(), targetLocaleBcp47Tag, null);
        }

        return xliffDataFactory.createTextUnit(tmTextUnit.getId(), tmTextUnit.getName(), tmTextUnit.getContent(), tmTextUnit.getComment(), tmTextUnitVariant.getContent(), targetLocaleBcp47Tag, null);
    }

    private String removeLeadingAndTrailingSpacesOnEveryLine(String string) {
        return string.replaceAll("(?m)^[\\s&&[^\\n]]+|[\\s+&&[^\\n]]+$", "");
    }

    @Test
    public void testExportAssetAsXLIFF() throws RepositoryNameAlreadyUsedException {
        createTestData();

        String targetLocaleBcp47Tag = "fr-FR";

        logger.debug("Export empty TM for source (en)");
        String exportAssetAsXLIFF = tmService.exportAssetAsXLIFF(assetId, "en");

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:okp=\"okapi-framework:xliff-extensions\">\n"
                + "<file original=\"test-asset-path.xliff\" source-language=\"en\" target-language=\"en\" datatype=\"x-undefined\" okp:inputEncoding=\"UTF-8\">\n"
                + "<body>\n"
                + "</body>\n"
                + "</file>\n"
                + "</xliff>\n", exportAssetAsXLIFF);

        logger.debug("Export empty TM for locale");

        exportAssetAsXLIFF = tmService.exportAssetAsXLIFF(assetId, targetLocaleBcp47Tag);

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:okp=\"okapi-framework:xliff-extensions\">\n"
                + "<file original=\"test-asset-path.xliff\" source-language=\"en\" target-language=\"fr-FR\" datatype=\"x-undefined\" okp:inputEncoding=\"UTF-8\">\n"
                + "<body>\n"
                + "</body>\n"
                + "</file>\n"
                + "</xliff>\n", exportAssetAsXLIFF);

        logger.debug("Add translations");
        TMTextUnit tmTextUnit1 = tmService.addTMTextUnit(tmId, assetId, "application_name", "Application Name", "This text is shown in the start screen of the application. Keep it short.");
        TMTextUnit tmTextUnit2 = tmService.addTMTextUnit(tmId, assetId, "home", "Home", "This is the text displayed in the link that takes the user to the home page.");

        Locale targetLocale = localeService.findByBcp47Tag(targetLocaleBcp47Tag);

        tmService.addCurrentTMTextUnitVariant(tmTextUnit1.getId(), targetLocale.getId(), "Nom de l'application");
        tmService.addCurrentTMTextUnitVariant(tmTextUnit2.getId(), targetLocale.getId(), "Page d'accueil");

        logger.debug("Export TM for source (en) with 2 translation");
        exportAssetAsXLIFF = tmService.exportAssetAsXLIFF(assetId, "en");
        exportAssetAsXLIFF = removeIdsAndDatesFromJson(exportAssetAsXLIFF);
        logger.debug(exportAssetAsXLIFF);

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:okp=\"okapi-framework:xliff-extensions\">\n"
                + "<file original=\"test-asset-path.xliff\" source-language=\"en\" target-language=\"en\" datatype=\"x-undefined\" okp:inputEncoding=\"UTF-8\">\n"
                + "<body>\n"
                + "<trans-unit id=\"\" resname=\"application_name\" xml:space=\"preserve\">\n"
                + "<source xml:lang=\"en\">Application Name</source>\n"
                + "<target xml:lang=\"en\">Application Name</target>\n"
                + "<note>{\"sourceComment\":\"This text is shown in the start screen of the application. Keep it short.\",\"targetComment\":\"This text is shown in the start screen of the application. Keep it short.\",\"includedInLocalizedFile\":true,\"status\":\"APPROVED\",\"variantComments\":[],\"pluralForm\":null,\"pluralFormOther\":null}</note>\n"
                + "</trans-unit>\n"
                + "<trans-unit id=\"\" resname=\"home\" xml:space=\"preserve\">\n"
                + "<source xml:lang=\"en\">Home</source>\n"
                + "<target xml:lang=\"en\">Home</target>\n"
                + "<note>{\"sourceComment\":\"This is the text displayed in the link that takes the user to the home page.\",\"targetComment\":\"This is the text displayed in the link that takes the user to the home page.\",\"includedInLocalizedFile\":true,\"status\":\"APPROVED\",\"variantComments\":[],\"pluralForm\":null,\"pluralFormOther\":null}</note>\n"
                + "</trans-unit>\n"
                + "</body>\n"
                + "</file>\n"
                + "</xliff>\n", exportAssetAsXLIFF);

        logger.debug("Export TM for locale with 2 translation");
        exportAssetAsXLIFF = tmService.exportAssetAsXLIFF(assetId, targetLocaleBcp47Tag);
        exportAssetAsXLIFF = removeIdsAndDatesFromJson(exportAssetAsXLIFF);
        logger.debug(exportAssetAsXLIFF);

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:okp=\"okapi-framework:xliff-extensions\">\n"
                + "<file original=\"test-asset-path.xliff\" source-language=\"en\" target-language=\"fr-FR\" datatype=\"x-undefined\" okp:inputEncoding=\"UTF-8\">\n"
                + "<body>\n"
                + "<trans-unit id=\"\" resname=\"application_name\" xml:space=\"preserve\">\n"
                + "<source xml:lang=\"en\">Application Name</source>\n"
                + "<target xml:lang=\"fr-FR\">Nom de l'application</target>\n"
                + "<note>{\"sourceComment\":\"This text is shown in the start screen of the application. Keep it short.\",\"targetComment\":null,\"includedInLocalizedFile\":true,\"status\":\"APPROVED\",\"variantComments\":[],\"pluralForm\":null,\"pluralFormOther\":null}</note>\n"
                + "</trans-unit>\n"
                + "<trans-unit id=\"\" resname=\"home\" xml:space=\"preserve\">\n"
                + "<source xml:lang=\"en\">Home</source>\n"
                + "<target xml:lang=\"fr-FR\">Page d'accueil</target>\n"
                + "<note>{\"sourceComment\":\"This is the text displayed in the link that takes the user to the home page.\",\"targetComment\":null,\"includedInLocalizedFile\":true,\"status\":\"APPROVED\",\"variantComments\":[],\"pluralForm\":null,\"pluralFormOther\":null}</note>\n"
                + "</trans-unit>\n"
                + "</body>\n"
                + "</file>\n"
                + "</xliff>\n", exportAssetAsXLIFF);

        logger.debug("Export TM with a string that needs review");
        tmService.addTMTextUnitCurrentVariant(tmTextUnit2.getId(), targetLocale.getId(), "Page d'accueil", "this string need to be reviewed because...", TMTextUnitVariant.Status.REVIEW_NEEDED, true);

        exportAssetAsXLIFF = tmService.exportAssetAsXLIFF(assetId, targetLocaleBcp47Tag);
        exportAssetAsXLIFF = removeIdsAndDatesFromJson(exportAssetAsXLIFF);
        logger.debug(exportAssetAsXLIFF);

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:okp=\"okapi-framework:xliff-extensions\">\n"
                + "<file original=\"test-asset-path.xliff\" source-language=\"en\" target-language=\"fr-FR\" datatype=\"x-undefined\" okp:inputEncoding=\"UTF-8\">\n"
                + "<body>\n"
                + "<trans-unit id=\"\" resname=\"application_name\" xml:space=\"preserve\">\n"
                + "<source xml:lang=\"en\">Application Name</source>\n"
                + "<target xml:lang=\"fr-FR\">Nom de l'application</target>\n"
                + "<note>{\"sourceComment\":\"This text is shown in the start screen of the application. Keep it short.\",\"targetComment\":null,\"includedInLocalizedFile\":true,\"status\":\"APPROVED\",\"variantComments\":[],\"pluralForm\":null,\"pluralFormOther\":null}</note>\n"
                + "</trans-unit>\n"
                + "<trans-unit id=\"\" resname=\"home\" xml:space=\"preserve\">\n"
                + "<source xml:lang=\"en\">Home</source>\n"
                + "<target xml:lang=\"fr-FR\">Page d'accueil</target>\n"
                + "<note>{\"sourceComment\":\"This is the text displayed in the link that takes the user to the home page.\",\"targetComment\":\"this string need to be reviewed because...\",\"includedInLocalizedFile\":true,\"status\":\"REVIEW_NEEDED\",\"variantComments\":[],\"pluralForm\":null,\"pluralFormOther\":null}</note>\n"
                + "</trans-unit>\n"
                + "</body>\n"
                + "</file>\n"
                + "</xliff>\n", exportAssetAsXLIFF);

        logger.debug("Export TM with a string that has comments");
        TMTextUnitCurrentVariant currentVariant = tmService.addTMTextUnitCurrentVariant(tmTextUnit2.getId(), targetLocale.getId(), "Page d'accueil", "this string has some comments", TMTextUnitVariant.Status.REVIEW_NEEDED, false);
        tmTextUnitVariantCommentService.addComment(
                currentVariant.getTmTextUnitVariant().getId(),
                TMTextUnitVariantComment.Type.LEVERAGING,
                TMTextUnitVariantComment.Severity.INFO,
                "Leveraging"
        );
        tmTextUnitVariantCommentService.addComment(
                currentVariant.getTmTextUnitVariant().getId(),
                TMTextUnitVariantComment.Type.INTEGRITY_CHECK,
                TMTextUnitVariantComment.Severity.ERROR,
                "Failed Integrity Check"
        );

        exportAssetAsXLIFF = tmService.exportAssetAsXLIFF(assetId, targetLocaleBcp47Tag);
        exportAssetAsXLIFF = removeIdsAndDatesFromJson(exportAssetAsXLIFF);
        logger.debug(exportAssetAsXLIFF);

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:okp=\"okapi-framework:xliff-extensions\">\n"
                + "<file original=\"test-asset-path.xliff\" source-language=\"en\" target-language=\"fr-FR\" datatype=\"x-undefined\" okp:inputEncoding=\"UTF-8\">\n"
                + "<body>\n"
                + "<trans-unit id=\"\" resname=\"application_name\" xml:space=\"preserve\">\n"
                + "<source xml:lang=\"en\">Application Name</source>\n"
                + "<target xml:lang=\"fr-FR\">Nom de l'application</target>\n"
                + "<note>{\"sourceComment\":\"This text is shown in the start screen of the application. Keep it short.\",\"targetComment\":null,\"includedInLocalizedFile\":true,\"status\":\"APPROVED\",\"variantComments\":[],\"pluralForm\":null,\"pluralFormOther\":null}</note>\n"
                + "</trans-unit>\n"
                + "<trans-unit id=\"\" resname=\"home\" xml:space=\"preserve\">\n"
                + "<source xml:lang=\"en\">Home</source>\n"
                + "<target xml:lang=\"fr-FR\">Page d'accueil</target>\n"
                + "<note>{\"sourceComment\":\"This is the text displayed in the link that takes the user to the home page.\",\"targetComment\":\"this string has some comments\",\"includedInLocalizedFile\":false,\"status\":\"REVIEW_NEEDED\",\"variantComments\":[{\"severity\":\"INFO\",\"type\":\"LEVERAGING\",\"content\":\"Leveraging\",\"createdByUser\":{\"username\":\"admin\",\"enabled\":true,\"surname\":null,\"givenName\":null,\"commonName\":null,\"authorities\":[{\"authority\":\"ROLE_ADMIN\"}]}},{\"severity\":\"ERROR\",\"type\":\"INTEGRITY_CHECK\",\"content\":\"Failed Integrity Check\",\"createdByUser\":{\"username\":\"admin\",\"enabled\":true,\"surname\":null,\"givenName\":null,\"commonName\":null,\"authorities\":[{\"authority\":\"ROLE_ADMIN\"}]}}],\"pluralForm\":null,\"pluralFormOther\":null}</note>\n"
                + "</trans-unit>\n"
                + "</body>\n"
                + "</file>\n"
                + "</xliff>\n", exportAssetAsXLIFF);
    }

    private String removeIdsAndDatesFromJson(String xliff) {
        String cleanXliff = xliff.replaceAll("\"id\":\\d+,?", "");
        cleanXliff = cleanXliff.replaceAll("\"createdDate\":\\d+,?", "");
        cleanXliff = cleanXliff.replaceAll("\"lastModifiedDate\":\\d+,?", "");
        cleanXliff = cleanXliff.replaceAll(",\\}", "}");

        return cleanXliff;
    }

    /**
     * This test is to test {@link XMLEncoder} with option to override encoding
     * of &lt; and &gt;
     *
     * According to Android specification in
     * http://developer.android.com/guide/topics/resources/string-resource.html,
     * <b>bold</b>, <i>italian</i> and <u>underline</u> should be in localized
     * file as-is.
     *
     * @throws Exception
     */
    @Test
    public void testLocalizeAndroidStringsWithSpecialCharacters() throws Exception {

        Repository repo = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        RepositoryLocale repoLocale;
        try {
            repoLocale = repositoryService.addRepositoryLocale(repo, "en-GB");
        } catch (RepositoryLocaleCreationException e) {
            throw new RuntimeException(e);
        }

        String assetContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<resources>\n"
                + "    <string description=\"Example html markup string1\" name=\"welcome1\">Welcome to <b>Android</b>!</string>\n"
                + "    <string description=\"Example html markup string2\" name=\"welcome2\">Welcome to <i>Android</i>!</string>\n"
                + "    <string description=\"Example html markup string3\" name=\"welcome3\">Welcome to <u>Android</u>!</string>\n"
                + "    <string name=\"subheader_text1\">\\\'Make sure you\\\'d \\\"escaped\\\" special characters like quotes &amp; ampersands.\\n</string>\n"
                + "    <string name=\"subheader_text2\">\"This'll also work\"</string>\n"
                + "</resources>";
        asset = assetService.createAsset(repo.getId(), assetContent, "res/values/strings.xml");
        asset = assetRepository.findOne(asset.getId());
        assetId = asset.getId();
        tmId = repo.getTm().getId();

        PollableFuture<Asset> assetResult = assetService.addOrUpdateAssetAndProcessIfNeeded(repo.getId(), assetContent, asset.getPath(), null);
        try {
            pollableTaskService.waitForPollableTask(assetResult.getPollableTask().getId());
        } catch (PollableTaskException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        assetResult.get();

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repo.getId());
        textUnitSearcherParameters.setStatusFilter(StatusFilter.FOR_TRANSLATION);
        List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);
        for (TextUnitDTO textUnitDTO : textUnitDTOs) {
            logger.debug("source=[{}]", textUnitDTO.getSource());
        }

        String localizedAsset = tmService.generateLocalized(asset, assetContent, repoLocale, "en-GB", null, InheritanceMode.USE_PARENT, Status.ALL);
        logger.debug("localized=\n{}", localizedAsset);
        assertEquals(assetContent, localizedAsset);
    }

    /**
     * This test is to test {@link XMLEncoder} with escaped HTML and CDATA
     *
     * @throws Exception
     */
    @Test
    public void testLocalizeAndroidStringsWithEscapedHTMLAndCDATA() throws Exception {

        Repository repo = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        RepositoryLocale repoLocale;
        try {
            repoLocale = repositoryService.addRepositoryLocale(repo, "en-GB");
        } catch (RepositoryLocaleCreationException e) {
            throw new RuntimeException(e);
        }

        String assetContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<resources>\n"
                + "    <string name=\"welcome_messages0\">Hello, %1$s! You have <b>%2$d new messages</b>.</string>\n"
                + "    <string name=\"welcome_messages1\">Hello, %1$s! You have &lt;b>%2$d new messages&lt;/b>.</string>\n"
                + "    <string name=\"welcome_messages2\">Hello, %1$s! You have <![CDATA[<b>%2$d new messages</b>]]>.</string>\n"
                + "</resources>";
        String expectedLocalizedAsset = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<resources>\n"
                + "    <string name=\"welcome_messages0\">Hello, %1$s! You have &lt;b>%2$d new messages&lt;/b>.</string>\n"
                + "    <string name=\"welcome_messages1\">Hello, %1$s! You have &lt;b>%2$d new messages&lt;/b>.</string>\n"
                + "    <string name=\"welcome_messages2\">Hello, %1$s! You have &lt;b>%2$d new messages&lt;/b>.</string>\n"
                + "</resources>";
        asset = assetService.createAsset(repo.getId(), assetContent, "res/values/strings.xml");
        asset = assetRepository.findOne(asset.getId());
        assetId = asset.getId();
        tmId = repo.getTm().getId();

        PollableFuture<Asset> assetResult = assetService.addOrUpdateAssetAndProcessIfNeeded(repo.getId(), assetContent, asset.getPath(), null);
        try {
            pollableTaskService.waitForPollableTask(assetResult.getPollableTask().getId());
        } catch (PollableTaskException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        assetResult.get();

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repo.getId());
        textUnitSearcherParameters.setStatusFilter(StatusFilter.FOR_TRANSLATION);
        List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);
        for (TextUnitDTO textUnitDTO : textUnitDTOs) {
            logger.debug("source=[{}]", textUnitDTO.getSource());
            assertEquals("Hello, %1$s! You have <b>%2$d new messages</b>.", textUnitDTO.getSource());
        }

        String localizedAsset = tmService.generateLocalized(asset, assetContent, repoLocale, "en-GB", null, InheritanceMode.USE_PARENT, Status.ALL);
        logger.debug("localized=\n{}", localizedAsset);
        assertEquals(expectedLocalizedAsset, localizedAsset);
    }

    @Test
    public void testLocalizeAndroidStringsPlural() throws Exception {

        Repository repo = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        RepositoryLocale repoLocale;
        try {
            repoLocale = repositoryService.addRepositoryLocale(repo, "ja-JP");
            repoLocale = repositoryService.addRepositoryLocale(repo, "en-GB");
        } catch (RepositoryLocaleCreationException e) {
            throw new RuntimeException(e);
        }

        String assetContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<resources>\n"
                + "  <!-- Example of plurals -->\n"
                + "  <plurals name=\"numberOfCollaborators\">\n"
                + "    <item quantity=\"one\">%1$d people</item>\n"
                + "    <item quantity=\"other\">%1$d people</item>\n"
                + "  </plurals>\n"
                + "  <!-- Example2 of plurals -->\n"
                + "  <plurals name=\"numberOfCollaborators2\">\n"
                + "    <item quantity=\"one\">%1$d people</item>\n"
                + "    <item quantity=\"other\">%1$d people</item>\n"
                + "  </plurals>\n"
                + "  <plurals name=\"numberOfCollaborators3\">\n"
                + "    <item quantity=\"other\">%1$d people</item>\n"
                + "  </plurals>\n"
                + "</resources>";
        String expectedLocalizedAsset_jaJP = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<resources>\n"
                + "  <!-- Example of plurals -->\n"
                + "  <plurals name=\"numberOfCollaborators\">\n"
                + "    <item quantity=\"other\">%1$d people</item>\n"
                + "  </plurals>\n"
                + "  <!-- Example2 of plurals -->\n"
                + "  <plurals name=\"numberOfCollaborators2\">\n"
                + "    <item quantity=\"other\">%1$d people</item>\n"
                + "  </plurals>\n"
                + "  <plurals name=\"numberOfCollaborators3\">\n"
                + "    <item quantity=\"other\">%1$d people</item>\n"
                + "  </plurals>\n"
                + "</resources>";
        String expectedLocalizedAsset_enGB = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<resources>\n"
                + "  <!-- Example of plurals -->\n"
                + "  <plurals name=\"numberOfCollaborators\">\n"
                + "    <item quantity=\"one\">%1$d people</item>\n"
                + "    <item quantity=\"other\">%1$d people</item>\n"
                + "  </plurals>\n"
                + "  <!-- Example2 of plurals -->\n"
                + "  <plurals name=\"numberOfCollaborators2\">\n"
                + "    <item quantity=\"one\">%1$d people</item>\n"
                + "    <item quantity=\"other\">%1$d people</item>\n"
                + "  </plurals>\n"
                + "  <plurals name=\"numberOfCollaborators3\">\n"
                + "    <item quantity=\"one\">%1$d people</item>\n"
                + "    <item quantity=\"other\">%1$d people</item>\n"
                + "  </plurals>\n"
                + "</resources>";
        asset = assetService.createAsset(repo.getId(), assetContent, "res/values/strings.xml");
        asset = assetRepository.findOne(asset.getId());
        assetId = asset.getId();
        tmId = repo.getTm().getId();

        PollableFuture<Asset> assetResult = assetService.addOrUpdateAssetAndProcessIfNeeded(repo.getId(), assetContent, asset.getPath(), null);
        try {
            pollableTaskService.waitForPollableTask(assetResult.getPollableTask().getId());
        } catch (PollableTaskException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        assetResult.get();

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repo.getId());
        textUnitSearcherParameters.setStatusFilter(StatusFilter.FOR_TRANSLATION);
        List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);

        for (TextUnitDTO textUnitDTO : textUnitDTOs) {
            logger.debug("source [{}]=[{}]", textUnitDTO.getName(), textUnitDTO.getSource());
        }

        String localizedAsset = tmService.generateLocalized(asset, assetContent, repoLocale, "ja-JP", null, InheritanceMode.USE_PARENT, Status.ALL);
        logger.debug("localized=\n{}", localizedAsset);
        assertEquals(expectedLocalizedAsset_jaJP, localizedAsset);

        localizedAsset = tmService.generateLocalized(asset, assetContent, repoLocale, "en-GB", null, InheritanceMode.USE_PARENT, Status.ALL);
        logger.debug("localized=\n{}", localizedAsset);
        assertEquals(expectedLocalizedAsset_enGB, localizedAsset);
    }

    /**
     * This test is to test AndroidStrings array with empty item
     *
     * @throws Exception
     */
    @Test
    public void testLocalizeAndroidStringsArrayWithEmptyItem() throws Exception {

        Repository repo = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        RepositoryLocale repoLocale;
        try {
            repoLocale = repositoryService.addRepositoryLocale(repo, "en-GB");
        } catch (RepositoryLocaleCreationException e) {
            throw new RuntimeException(e);
        }

        String assetContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<resources>\n"
                + "    <string-array name=\"N_items_failed_to_move\">\n"
                + "        <item/>\n"
                + "        <item>1 item failed to move</item>\n"
                + "        <item>%1$d items failed to move</item>\n"
                + "    </string-array>\n"
                + "</resources>";
        asset = assetService.createAsset(repo.getId(), assetContent, "res/values/strings.xml");
        asset = assetRepository.findOne(asset.getId());
        assetId = asset.getId();
        tmId = repo.getTm().getId();

        PollableFuture<Asset> assetResult = assetService.addOrUpdateAssetAndProcessIfNeeded(repo.getId(), assetContent, asset.getPath(), null);
        try {
            pollableTaskService.waitForPollableTask(assetResult.getPollableTask().getId());
        } catch (PollableTaskException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        assetResult.get();

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repo.getId());
        textUnitSearcherParameters.setStatusFilter(StatusFilter.FOR_TRANSLATION);
        List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);
        for (TextUnitDTO textUnitDTO : textUnitDTOs) {
            logger.debug("source=[{}]", textUnitDTO.getSource());
        }

        String localizedAsset = tmService.generateLocalized(asset, assetContent, repoLocale, "en-GB", null, InheritanceMode.USE_PARENT, Status.ALL);
        logger.debug("localized=\n{}", localizedAsset);
        assertEquals(assetContent, localizedAsset);
    }

    /**
     * This test is to test AndroidStrings array with no xml version
     *
     * @throws Exception
     */
    @Test
    public void testLocalizeAndroidStringsNoXMLVersion() throws Exception {

        Repository repo = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        RepositoryLocale repoLocale;
        try {
            repoLocale = repositoryService.addRepositoryLocale(repo, "en-GB");
        } catch (RepositoryLocaleCreationException e) {
            throw new RuntimeException(e);
        }

        String assetContent
                = "<resources>\n"
                + "    <string name=\"test\">\"This is test\"</string>\n"
                + "</resources>";
        asset = assetService.createAsset(repo.getId(), assetContent, "res/values/strings.xml");
        asset = assetRepository.findOne(asset.getId());
        assetId = asset.getId();
        tmId = repo.getTm().getId();

        PollableFuture<Asset> assetResult = assetService.addOrUpdateAssetAndProcessIfNeeded(repo.getId(), assetContent, asset.getPath(), null);
        try {
            pollableTaskService.waitForPollableTask(assetResult.getPollableTask().getId());
        } catch (PollableTaskException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        assetResult.get();

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repo.getId());
        textUnitSearcherParameters.setStatusFilter(StatusFilter.FOR_TRANSLATION);
        List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);
        for (TextUnitDTO textUnitDTO : textUnitDTOs) {
            logger.debug("source=[{}]", textUnitDTO.getSource());
        }

        String localizedAsset = tmService.generateLocalized(asset, assetContent, repoLocale, "en-GB", null, InheritanceMode.USE_PARENT, Status.ALL);
        logger.debug("localized=\n{}", localizedAsset);
        assertEquals(assetContent, localizedAsset);
    }

    /**
     * This test is to test AndroidStrings with REMOVE_UNTRANSLATED inheritance mode
     *
     * @throws Exception
     */
    @Test
    public void testLocalizeAndroidStringsRemoveUntranslated() throws Exception {

        Repository repo = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        RepositoryLocale repoLocale;
        try {
            repoLocale = repositoryService.addRepositoryLocale(repo, "en-GB");
        } catch (RepositoryLocaleCreationException e) {
            throw new RuntimeException(e);
        }

        String assetContent
                = "<resources>\n"
                + "    <string name=\"test\">\"This is test\"</string>\n"
                + "    <string name=\"desc\">\"This is a description\"</string>\n"
                + "</resources>";
        asset = assetService.createAsset(repo.getId(), assetContent, "res/values/strings.xml");
        asset = assetRepository.findOne(asset.getId());
        assetId = asset.getId();
        tmId = repo.getTm().getId();

        PollableFuture<Asset> assetResult = assetService.addOrUpdateAssetAndProcessIfNeeded(repo.getId(), assetContent, asset.getPath(), null);
        try {
            pollableTaskService.waitForPollableTask(assetResult.getPollableTask().getId());
        } catch (PollableTaskException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        assetResult.get();

        String forImport
                = "<resources>\n"
                + "    <string name=\"test\">\"Le test\"</string>\n"
                + "</resources>";

        tmService.importLocalizedAsset(asset, forImport, repoLocale, StatusForEqualTarget.TRANSLATION_NEEDED, null).get();

        String localizedAsset = tmService.generateLocalized(asset, assetContent, repoLocale, "en-GB", null, InheritanceMode.REMOVE_UNTRANSLATED, Status.ALL);
        logger.debug("localized=\n{}", localizedAsset);
        assertEquals(forImport, localizedAsset);
    }

    /**
     * This test is to test {@link MacStringsEncoder} with special characters
     *
     * According to iOS specification in
     * https://developer.apple.com/library/ios/documentation/Cocoa/Conceptual/LoadingResources/Strings/Strings.html,
     * the following characters should be escaped with backslash: double-quote,
     * backslash, newline(\n), carriage return (\r).
     *
     * @throws Exception
     */
    @Test
    public void testLocalizeMacStringsWithSpecialCharacters() throws Exception {

        Repository repo = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        RepositoryLocale repoLocale;
        try {
            repoLocale = repositoryService.addRepositoryLocale(repo, "en-GB");
        } catch (RepositoryLocaleCreationException e) {
            throw new RuntimeException(e);
        }

        String assetContent = "\"100_character_description\" = \"\\\"100\\\" character description:\";\n"
                + "\"two_lines\" = \"first\\nsecond\";";
        asset = assetService.createAsset(repo.getId(), assetContent, "en.lproj/Localizable.strings");
        asset = assetRepository.findOne(asset.getId());
        assetId = asset.getId();
        tmId = repo.getTm().getId();

        PollableFuture<Asset> assetResult = assetService.addOrUpdateAssetAndProcessIfNeeded(repo.getId(), assetContent, asset.getPath(), null);
        try {
            pollableTaskService.waitForPollableTask(assetResult.getPollableTask().getId());
        } catch (PollableTaskException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        assetResult.get();

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repo.getId());
        textUnitSearcherParameters.setStatusFilter(StatusFilter.FOR_TRANSLATION);
        List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);
        for (TextUnitDTO textUnitDTO : textUnitDTOs) {
            logger.debug("source=[{}]", textUnitDTO.getSource());
        }

        String localizedAsset = tmService.generateLocalized(asset, assetContent, repoLocale, "en-GB", null, InheritanceMode.USE_PARENT, Status.ALL);
        logger.debug("localized=\n{}", localizedAsset);
        assertEquals(assetContent, localizedAsset);
    }

    @Test
    public void testLocalizePoPluralJp() throws Exception {

        Repository repo = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        RepositoryLocale repoLocale;
        try {
            repoLocale = repositoryService.addRepositoryLocale(repo, "ja-JP");
        } catch (RepositoryLocaleCreationException e) {
            throw new RuntimeException(e);
        }

        String assetContent = "msgid \"\"\n"
                + "msgstr \"\"\n"
                + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
                + "\"Report-Msgid-Bugs-To: \\n\"\n"
                + "\"POT-Creation-Date: 2017-09-15 11:53-0500\\n\"\n"
                + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
                + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
                + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
                + "\"MIME-Version: 1.0\\n\"\n"
                + "\"Plural-Forms: nplurals=2; plural=(n != 1);\\n\"\n"
                + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
                + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
                + "#. Comments\n"
                + "#: core/logic/week_in_review_email_logic.py:49\n"
                + "msgid \"repin\"\n"
                + "msgid_plural \"repins\"\n"
                + "msgstr[0] \"\"\n"
                + "msgstr[1] \"\"";

        String expectedLocalizedAsset = "msgid \"\"\n"
                + "msgstr \"\"\n"
                + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
                + "\"Report-Msgid-Bugs-To: \\n\"\n"
                + "\"POT-Creation-Date: 2017-09-15 11:53-0500\\n\"\n"
                + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
                + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
                + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
                + "\"MIME-Version: 1.0\\n\"\n"
                + "\"Plural-Forms: nplurals=1; plural=0;\\n\"\n"
                + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
                + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
                + "#. Comments\n"
                + "#: core/logic/week_in_review_email_logic.py:49\n"
                + "msgid \"repin\"\n"
                + "msgid_plural \"repins\"\n"
                + "msgstr[0] \"repins\"\n";

        asset = assetService.createAsset(repo.getId(), assetContent, "messages.pot");
        asset = assetRepository.findOne(asset.getId());
        assetId = asset.getId();
        tmId = repo.getTm().getId();

        PollableFuture<Asset> assetResult = assetService.addOrUpdateAssetAndProcessIfNeeded(repo.getId(), assetContent, asset.getPath(), null);
        try {
            pollableTaskService.waitForPollableTask(assetResult.getPollableTask().getId());
        } catch (PollableTaskException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        assetResult.get();

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repo.getId());
        textUnitSearcherParameters.setStatusFilter(StatusFilter.FOR_TRANSLATION);
        List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);

        for (TextUnitDTO textUnitDTO : textUnitDTOs) {
            logger.debug("source=[{}]", textUnitDTO.getSource());
        }
         
        String localizedAsset = tmService.generateLocalized(asset, assetContent, repoLocale, "ja-JP", null, InheritanceMode.USE_PARENT, Status.ALL);
        logger.debug("localized=\n{}", localizedAsset);
        assertEquals(expectedLocalizedAsset, localizedAsset);

        String forImport = "msgid \"\"\n"
                + "msgstr \"\"\n"
                + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
                + "\"Report-Msgid-Bugs-To: \\n\"\n"
                + "\"POT-Creation-Date: 2017-09-15 11:53-0500\\n\"\n"
                + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
                + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
                + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
                + "\"MIME-Version: 1.0\\n\"\n"
                + "\"Plural-Forms: nplurals=1; plural=0;\\n\"\n"
                + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
                + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
                + "#. Comments\n"
                + "#: core/logic/week_in_review_email_logic.py:49\n"
                + "msgid \"repin\"\n"
                + "msgid_plural \"repins\"\n"
                + "msgstr[0] \"repin-jp\"\n";

        tmService.importLocalizedAsset(asset, forImport, repoLocale, StatusForEqualTarget.TRANSLATION_NEEDED, null).get();

        localizedAsset = tmService.generateLocalized(asset, assetContent, repoLocale, "ja-JP", null, InheritanceMode.USE_PARENT, Status.ALL);
        logger.debug("localized after import=\n{}", localizedAsset);
        assertEquals(forImport, localizedAsset);
    }

    @Test
    public void testLocalizePoRemoveUntranslated() throws Exception {

        Repository repo = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        RepositoryLocale repoLocale;
        try {
            repoLocale = repositoryService.addRepositoryLocale(repo, "ja-JP");
        } catch (RepositoryLocaleCreationException e) {
            throw new RuntimeException(e);
        }

        String assetContent = "msgid \"\"\n"
                + "msgstr \"\"\n"
                + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
                + "\"Report-Msgid-Bugs-To: \\n\"\n"
                + "\"POT-Creation-Date: 2017-09-15 11:53-0500\\n\"\n"
                + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
                + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
                + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
                + "\"MIME-Version: 1.0\\n\"\n"
                + "\"Plural-Forms: nplurals=2; plural=(n != 1);\\n\"\n"
                + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
                + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
                + "#. Comments\n"
                + "#: core/logic/week_in_review_email_logic.py:49\n"
                + "msgid \"repin\"\n"
                + "msgstr \"\"\n"
                + "#. Description\n"
                + "#: core/logic/week_in_review_email_logic.py:50\n"
                + "msgid \"description\"\n"
                + "msgstr \"\"\n";


        String expectedLocalizedAsset = "msgid \"\"\n"
                + "msgstr \"\"\n"
                + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
                + "\"Report-Msgid-Bugs-To: \\n\"\n"
                + "\"POT-Creation-Date: 2017-09-15 11:53-0500\\n\"\n"
                + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
                + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
                + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
                + "\"MIME-Version: 1.0\\n\"\n"
                + "\"Plural-Forms: nplurals=1; plural=0;\\n\"\n"
                + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
                + "\"Content-Transfer-Encoding: 8bit\\n\"\n";

        asset = assetService.createAsset(repo.getId(), assetContent, "messages.pot");
        asset = assetRepository.findOne(asset.getId());
        assetId = asset.getId();
        tmId = repo.getTm().getId();

        PollableFuture<Asset> assetResult = assetService.addOrUpdateAssetAndProcessIfNeeded(repo.getId(), assetContent, asset.getPath(), null);
        try {
            pollableTaskService.waitForPollableTask(assetResult.getPollableTask().getId());
        } catch (PollableTaskException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        assetResult.get();

        String localizedAsset = tmService.generateLocalized(asset, assetContent, repoLocale, "ja-JP", null, InheritanceMode.REMOVE_UNTRANSLATED, Status.ALL);
        logger.debug("localized=\n{}", localizedAsset);
        assertEquals(expectedLocalizedAsset, localizedAsset);

        String forImport = "msgid \"\"\n"
                + "msgstr \"\"\n"
                + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
                + "\"Report-Msgid-Bugs-To: \\n\"\n"
                + "\"POT-Creation-Date: 2017-09-15 11:53-0500\\n\"\n"
                + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
                + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
                + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
                + "\"MIME-Version: 1.0\\n\"\n"
                + "\"Plural-Forms: nplurals=1; plural=0;\\n\"\n"
                + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
                + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
                + "#. Comments\n"
                + "#: core/logic/week_in_review_email_logic.py:49\n"
                + "msgid \"repin\"\n"
                + "msgstr \"repin-jp\"\n";

        tmService.importLocalizedAsset(asset, forImport, repoLocale, StatusForEqualTarget.TRANSLATION_NEEDED, null).get();

        localizedAsset = tmService.generateLocalized(asset, assetContent, repoLocale, "ja-JP", null, InheritanceMode.REMOVE_UNTRANSLATED, Status.ALL);
        logger.debug("localized after import=\n{}", localizedAsset);
        assertEquals(forImport, localizedAsset);
    }


    @Test
    public void testLocalizePoPluralRu() throws Exception {

        Repository repo = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        RepositoryLocale repoLocale;
        try {
            repoLocale = repositoryService.addRepositoryLocale(repo, "ru-RU");
        } catch (RepositoryLocaleCreationException e) {
            throw new RuntimeException(e);
        }

        String assetContent = "msgid \"\"\n"
                + "msgstr \"\"\n"
                + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
                + "\"Report-Msgid-Bugs-To: \\n\"\n"
                + "\"POT-Creation-Date: 2017-09-15 11:53-0500\\n\"\n"
                + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
                + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
                + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
                + "\"MIME-Version: 1.0\\n\"\n"
                + "\"Plural-Forms: nplurals=2; plural=(n != 1);\\n\"\n"
                + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
                + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
                + "#. Comments\n"
                + "#: core/logic/week_in_review_email_logic.py:49\n"
                + "msgid \"repin\"\n"
                + "msgid_plural \"repins\"\n"
                + "msgstr[0] \"\"\n"
                + "msgstr[1] \"\"";

        String expectedLocalizedAsset = "msgid \"\"\n"
                + "msgstr \"\"\n"
                + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
                + "\"Report-Msgid-Bugs-To: \\n\"\n"
                + "\"POT-Creation-Date: 2017-09-15 11:53-0500\\n\"\n"
                + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
                + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
                + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
                + "\"MIME-Version: 1.0\\n\"\n"
                + "\"Plural-Forms: nplurals=3; plural=n%10==1 && n%100!=11 ? 0 : n%10>=2 && n%10<=4 && (n%100<10 || n%100>=20) ? 1 : 2;\\n\"\n"
                + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
                + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
                + "#. Comments\n"
                + "#: core/logic/week_in_review_email_logic.py:49\n"
                + "msgid \"repin\"\n"
                + "msgid_plural \"repins\"\n"
                + "msgstr[0] \"repin\"\n"
                + "msgstr[1] \"repins\"\n"
                + "msgstr[2] \"repins\"\n";

        asset = assetService.createAsset(repo.getId(), assetContent, "messages.pot");
        asset = assetRepository.findOne(asset.getId());
        assetId = asset.getId();
        tmId = repo.getTm().getId();

        PollableFuture<Asset> assetResult = assetService.addOrUpdateAssetAndProcessIfNeeded(repo.getId(), assetContent, asset.getPath(), null);
        try {
            pollableTaskService.waitForPollableTask(assetResult.getPollableTask().getId());
        } catch (PollableTaskException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        assetResult.get();

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repo.getId());
        textUnitSearcherParameters.setStatusFilter(StatusFilter.FOR_TRANSLATION);
        List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);

        for (TextUnitDTO textUnitDTO : textUnitDTOs) {
            logger.debug("source=[{}]", textUnitDTO.getSource());
        }

//        assertEquals("Hello, %1$s! You have <b>%2$d new messages</b>.", textUnitDTO.getSource());
        String localizedAsset = tmService.generateLocalized(asset, assetContent, repoLocale, "ru-RU", null, InheritanceMode.USE_PARENT, Status.ALL);
        logger.debug("localized=\n{}", localizedAsset);
        assertEquals(expectedLocalizedAsset, localizedAsset);

        String forImport = "msgid \"\"\n"
                + "msgstr \"\"\n"
                + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
                + "\"Report-Msgid-Bugs-To: \\n\"\n"
                + "\"POT-Creation-Date: 2017-09-15 11:53-0500\\n\"\n"
                + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
                + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
                + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
                + "\"MIME-Version: 1.0\\n\"\n"
                + "\"Plural-Forms: nplurals=3; plural=n%10==1 && n%100!=11 ? 0 : n%10>=2 && n%10<=4 && (n%100<10 || n%100>=20) ? 1 : 2;\\n\"\n"
                + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
                + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
                + "#. Comments\n"
                + "#: core/logic/week_in_review_email_logic.py:49\n"
                + "msgid \"repin\"\n"
                + "msgid_plural \"repins\"\n"
                + "msgstr[0] \"repin-ru\"\n"
                + "msgstr[1] \"repins-ru-1\"\n"
                + "msgstr[2] \"repins-ru-2\"\n";

        tmService.importLocalizedAsset(asset, forImport, repoLocale, StatusForEqualTarget.TRANSLATION_NEEDED, null).get();

        localizedAsset = tmService.generateLocalized(asset, assetContent, repoLocale, "ru-RU", null, InheritanceMode.USE_PARENT, Status.ALL);
        logger.debug("localized after import=\n{}", localizedAsset);

        assertEquals(forImport, localizedAsset);
    }

    @Test
    public void testLocalizePoPluralCs() throws Exception {

        Repository repo = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        RepositoryLocale repoLocale;
        try {
            repoLocale = repositoryService.addRepositoryLocale(repo, "cs-CZ");
        } catch (RepositoryLocaleCreationException e) {
            throw new RuntimeException(e);
        }

        String assetContent = "msgid \"\"\n"
                + "msgstr \"\"\n"
                + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
                + "\"Report-Msgid-Bugs-To: \\n\"\n"
                + "\"POT-Creation-Date: 2017-09-15 11:53-0500\\n\"\n"
                + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
                + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
                + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
                + "\"MIME-Version: 1.0\\n\"\n"
                + "\"Plural-Forms: nplurals=2; plural=(n != 1);\\n\"\n"
                + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
                + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
                + "#. Comments\n"
                + "#: core/logic/week_in_review_email_logic.py:49\n"
                + "msgid \"repin\"\n"
                + "msgid_plural \"repins\"\n"
                + "msgstr[0] \"\"\n"
                + "msgstr[1] \"\"";

        String expectedLocalizedAsset = "msgid \"\"\n"
                + "msgstr \"\"\n"
                + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
                + "\"Report-Msgid-Bugs-To: \\n\"\n"
                + "\"POT-Creation-Date: 2017-09-15 11:53-0500\\n\"\n"
                + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
                + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
                + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
                + "\"MIME-Version: 1.0\\n\"\n"
                + "\"Plural-Forms: nplurals=3; plural=(n==1) ? 0 : (n>=2 && n<=4) ? 1 : 2;\\n\"\n"
                + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
                + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
                + "#. Comments\n"
                + "#: core/logic/week_in_review_email_logic.py:49\n"
                + "msgid \"repin\"\n"
                + "msgid_plural \"repins\"\n"
                + "msgstr[0] \"repin\"\n"
                + "msgstr[1] \"repins\"\n"
                + "msgstr[2] \"repins\"\n";

        asset = assetService.createAsset(repo.getId(), assetContent, "messages.pot");
        asset = assetRepository.findOne(asset.getId());
        assetId = asset.getId();
        tmId = repo.getTm().getId();

        PollableFuture<Asset> assetResult = assetService.addOrUpdateAssetAndProcessIfNeeded(repo.getId(), assetContent, asset.getPath(), null);
        try {
            pollableTaskService.waitForPollableTask(assetResult.getPollableTask().getId());
        } catch (PollableTaskException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        assetResult.get();

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repo.getId());
        textUnitSearcherParameters.setStatusFilter(StatusFilter.FOR_TRANSLATION);
        List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);

        for (TextUnitDTO textUnitDTO : textUnitDTOs) {
            logger.debug("source=[{}]", textUnitDTO.getSource());
        }

//        assertEquals("Hello, %1$s! You have <b>%2$d new messages</b>.", textUnitDTO.getSource());
        String localizedAsset = tmService.generateLocalized(asset, assetContent, repoLocale, "cs-CZ", null, InheritanceMode.USE_PARENT, Status.ALL);
        logger.debug("localized=\n{}", localizedAsset);
        assertEquals(expectedLocalizedAsset, localizedAsset);

        String forImport = "msgid \"\"\n"
                + "msgstr \"\"\n"
                + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
                + "\"Report-Msgid-Bugs-To: \\n\"\n"
                + "\"POT-Creation-Date: 2017-09-15 11:53-0500\\n\"\n"
                + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
                + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
                + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
                + "\"MIME-Version: 1.0\\n\"\n"
                + "\"Plural-Forms: nplurals=3; plural=(n==1) ? 0 : (n>=2 && n<=4) ? 1 : 2;\\n\"\n"
                + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
                + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
                + "#. Comments\n"
                + "#: core/logic/week_in_review_email_logic.py:49\n"
                + "msgid \"repin\"\n"
                + "msgid_plural \"repins\"\n"
                + "msgstr[0] \"repin-cs\"\n"
                + "msgstr[1] \"repins-cz-1\"\n"
                + "msgstr[2] \"repins-cz-2\"\n";

        tmService.importLocalizedAsset(asset, forImport, repoLocale, StatusForEqualTarget.TRANSLATION_NEEDED, null).get();

        localizedAsset = tmService.generateLocalized(asset, assetContent, repoLocale, "cs-CZ", null, InheritanceMode.USE_PARENT, Status.ALL);
        logger.debug("localized after import=\n{}", localizedAsset);

        assertEquals(forImport, localizedAsset);
    }

    @Test
    public void testLocalizeMacStringsNamessNotEnclosedInDoubleQuotes() throws Exception {

        Repository repo = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        RepositoryLocale repoLocale;
        try {
            repoLocale = repositoryService.addRepositoryLocale(repo, "en-GB");
        } catch (RepositoryLocaleCreationException e) {
            throw new RuntimeException(e);
        }

        String assetContent = "NSUsageDescription = \"Usage description:\";\n";
        asset = assetService.createAsset(repo.getId(), assetContent, "en.lproj/Localizable.strings");
        asset = assetRepository.findOne(asset.getId());
        assetId = asset.getId();
        tmId = repo.getTm().getId();

        PollableFuture<Asset> assetResult = assetService.addOrUpdateAssetAndProcessIfNeeded(repo.getId(), assetContent, asset.getPath(), null);
        try {
            pollableTaskService.waitForPollableTask(assetResult.getPollableTask().getId());
        } catch (PollableTaskException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        assetResult.get();

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repo.getId());
        textUnitSearcherParameters.setStatusFilter(StatusFilter.FOR_TRANSLATION);
        List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);
        assertEquals(1, textUnitDTOs.size());
        for (TextUnitDTO textUnitDTO : textUnitDTOs) {
            logger.debug("source=[{}]", textUnitDTO.getSource());
        }

        String localizedAsset = tmService.generateLocalized(asset, assetContent, repoLocale, "en-GB", null, InheritanceMode.USE_PARENT, Status.ALL);
        logger.debug("localized=\n{}", localizedAsset);
        assertEquals(assetContent, localizedAsset);
    }

    @Test
    public void testLocalizeXtb() throws Exception {
        Repository repo = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        RepositoryLocale repoLocale;
        try {
            repoLocale = repositoryService.addRepositoryLocale(repo, "en-GB");
        } catch (RepositoryLocaleCreationException e) {
            throw new RuntimeException(e);
        }

        String assetContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<!DOCTYPE translationbundle>\n"
                + "<translationbundle lang=\"en-US\">\n"
                + "	<translation id=\"0\" key=\"MSG_DIALOG_OK_\" source=\"lib/closure-library/closure/goog/ui/dialog.js\" desc=\"Standard caption for the dialog 'OK' button.\">OK</translation>\n"
                + "     <translation id=\"1\" key=\"MSG_VIEWER_MENU\" source=\"src/js/box/dicom/viewer/toolbar.js\" desc=\"Tooltip text for the &quot;More&quot; menu.\">More</translation>\n"
                + "     <translation id=\"2\" key=\"MSG_GONSTEAD_STEP\" source=\"src/js/box/dicom/viewer/gonsteaddialog.js\" desc=\"Instructions for the Gonstead method.\">Select the &lt;strong&gt;left Iliac crest&lt;/strong&gt;</translation>\n"
                + "</translationbundle>";
        String expectedContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<translationbundle lang=\"en-US\">\n"
                + "	<translation desc=\"Standard caption for the dialog 'OK' button.\" id=\"0\" key=\"MSG_DIALOG_OK_\" source=\"lib/closure-library/closure/goog/ui/dialog.js\">OK</translation>\n"
                + "     <translation desc=\"Tooltip text for the &quot;More&quot; menu.\" id=\"1\" key=\"MSG_VIEWER_MENU\" source=\"src/js/box/dicom/viewer/toolbar.js\">More</translation>\n"
                + "     <translation desc=\"Instructions for the Gonstead method.\" id=\"2\" key=\"MSG_GONSTEAD_STEP\" source=\"src/js/box/dicom/viewer/gonsteaddialog.js\">Select the &lt;strong&gt;left Iliac crest&lt;/strong&gt;</translation>\n"
                + "</translationbundle>";
        asset = assetService.createAsset(repo.getId(), assetContent, "xtb/messages-en-US.xtb");
        asset = assetRepository.findOne(asset.getId());
        assetId = asset.getId();
        tmId = repo.getTm().getId();

        PollableFuture<Asset> assetResult = assetService.addOrUpdateAssetAndProcessIfNeeded(repo.getId(), assetContent, asset.getPath(), null);
        try {
            pollableTaskService.waitForPollableTask(assetResult.getPollableTask().getId());
        } catch (PollableTaskException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        assetResult.get();

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repo.getId());
        textUnitSearcherParameters.setStatusFilter(StatusFilter.FOR_TRANSLATION);
        List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);
        assertEquals(3, textUnitDTOs.size());
        for (TextUnitDTO textUnitDTO : textUnitDTOs) {
            logger.debug("source=[{}]", textUnitDTO.getSource());
        }

        String localizedAsset = tmService.generateLocalized(asset, assetContent, repoLocale, "en-GB", null, InheritanceMode.USE_PARENT, Status.ALL);
        logger.debug("localized=\n{}", localizedAsset);
        assertEquals(expectedContent, localizedAsset);
    }

    @Ignore("Bug: does not output translationbundle opening tag")
    @Test
    public void testLocalizeXtbRemoveUntranslated() throws Exception {
        Repository repo = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        RepositoryLocale repoLocale;
        try {
            repoLocale = repositoryService.addRepositoryLocale(repo, "en-GB");
        } catch (RepositoryLocaleCreationException e) {
            throw new RuntimeException(e);
        }

        String assetContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<!DOCTYPE translationbundle>\n"
                + "<translationbundle lang=\"en-US\">\n"
                + "	<translation id=\"0\" key=\"MSG_DIALOG_OK_\" source=\"lib/closure-library/closure/goog/ui/dialog.js\" desc=\"Standard caption for the dialog 'OK' button.\">OK</translation>\n"
                + "     <translation id=\"1\" key=\"MSG_VIEWER_MENU\" source=\"src/js/box/dicom/viewer/toolbar.js\" desc=\"Tooltip text for the &quot;More&quot; menu.\">More</translation>\n"
                + "     <translation id=\"2\" key=\"MSG_GONSTEAD_STEP\" source=\"src/js/box/dicom/viewer/gonsteaddialog.js\" desc=\"Instructions for the Gonstead method.\">Select the &lt;strong&gt;left Iliac crest&lt;/strong&gt;</translation>\n"
                + "</translationbundle>";
        String expectedContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<translationbundle />\n";

        asset = assetService.createAsset(repo.getId(), assetContent, "xtb/messages-en-US.xtb");
        asset = assetRepository.findOne(asset.getId());
        assetId = asset.getId();
        tmId = repo.getTm().getId();

        PollableFuture<Asset> assetResult = assetService.addOrUpdateAssetAndProcessIfNeeded(repo.getId(), assetContent, asset.getPath(), null);
        try {
            pollableTaskService.waitForPollableTask(assetResult.getPollableTask().getId());
        } catch (PollableTaskException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        String localizedAsset = tmService.generateLocalized(asset, assetContent, repoLocale, "en-GB", null, InheritanceMode.REMOVE_UNTRANSLATED, Status.ALL);
        logger.debug("localized=\n{}", localizedAsset);
        //assertEquals(expectedContent, localizedAsset);

        String forImport = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<!DOCTYPE translationbundle>\n"
                + "<translationbundle lang=\"ja-JP\">\n"
                + "<translation id=\"1\" key=\"MSG_VIEWER_MENU\" source=\"src/js/box/dicom/viewer/toolbar.js\" desc=\"Tooltip text for the &quot;More&quot; menu.\">Plus</translation>\n"
                + "</translationbundle>";

        tmService.importLocalizedAsset(asset, forImport, repoLocale, StatusForEqualTarget.TRANSLATION_NEEDED, null).get();

        localizedAsset = tmService.generateLocalized(asset, assetContent, repoLocale, "ja-JP", null, InheritanceMode.REMOVE_UNTRANSLATED, Status.ALL);
        logger.debug("localized after import=\n{}", localizedAsset);
        assertEquals(forImport, localizedAsset);

    }

    @Test
    public void testImportLocalizedAssetXLIFFApproved() throws RepositoryNameAlreadyUsedException, ExecutionException, InterruptedException, AssetUpdateException, AssetUpdateException {

        baseTestImportLocalizedAsset(StatusForEqualTarget.APPROVED);

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repository.getId());
        textUnitSearcherParameters.setStatusFilter(StatusFilter.TRANSLATED);
        List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);

        Iterator<TextUnitDTO> iterator = textUnitDTOs.iterator();
        TextUnitDTO next = iterator.next();
        assertEquals("Bonjour", next.getTarget());
        assertEquals(TMTextUnitVariant.Status.APPROVED, next.getStatus());

        next = iterator.next();
        assertEquals("Au revoir", next.getTarget());
        assertEquals(TMTextUnitVariant.Status.APPROVED, next.getStatus());

        next = iterator.next();
        assertEquals("target", next.getTarget());
        assertEquals(TMTextUnitVariant.Status.APPROVED, next.getStatus());

        assertFalse(iterator.hasNext());
    }

    @Test
    public void testImportLocalizedAssetXLIFFReviewNeeded() throws RepositoryNameAlreadyUsedException, ExecutionException, InterruptedException, AssetUpdateException {

        baseTestImportLocalizedAsset(StatusForEqualTarget.REVIEW_NEEDED);

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repository.getId());
        textUnitSearcherParameters.setStatusFilter(StatusFilter.TRANSLATED);
        List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);

        Iterator<TextUnitDTO> iterator = textUnitDTOs.iterator();
        TextUnitDTO next = iterator.next();
        assertEquals("Bonjour", next.getTarget());
        assertEquals(TMTextUnitVariant.Status.APPROVED, next.getStatus());

        next = iterator.next();
        assertEquals("Au revoir", next.getTarget());
        assertEquals(TMTextUnitVariant.Status.APPROVED, next.getStatus());

        next = iterator.next();
        assertEquals("target", next.getTarget());
        assertEquals(TMTextUnitVariant.Status.REVIEW_NEEDED, next.getStatus());

        assertFalse(iterator.hasNext());

    }

    @Test
    public void testImportLocalizedAssetXLIFFTranslationNeeded() throws RepositoryNameAlreadyUsedException, ExecutionException, InterruptedException, AssetUpdateException {

        baseTestImportLocalizedAsset(StatusForEqualTarget.TRANSLATION_NEEDED);

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repository.getId());
        textUnitSearcherParameters.setStatusFilter(StatusFilter.TRANSLATED);
        List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);

        Iterator<TextUnitDTO> iterator = textUnitDTOs.iterator();
        TextUnitDTO next = iterator.next();
        assertEquals("Bonjour", next.getTarget());
        assertEquals(TMTextUnitVariant.Status.APPROVED, next.getStatus());

        next = iterator.next();
        assertEquals("Au revoir", next.getTarget());
        assertEquals(TMTextUnitVariant.Status.APPROVED, next.getStatus());

        next = iterator.next();
        assertEquals("target", next.getTarget());
        assertEquals(TMTextUnitVariant.Status.TRANSLATION_NEEDED, next.getStatus());

        assertFalse(iterator.hasNext());

    }

    @Test
    public void testImportLocalizedAssetXLIFFSkip() throws RepositoryNameAlreadyUsedException, ExecutionException, InterruptedException, AssetUpdateException {

        baseTestImportLocalizedAsset(StatusForEqualTarget.SKIPPED);

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repository.getId());
        textUnitSearcherParameters.setStatusFilter(StatusFilter.TRANSLATED);
        List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);

        Iterator<TextUnitDTO> iterator = textUnitDTOs.iterator();
        TextUnitDTO next = iterator.next();
        assertEquals("Bonjour", next.getTarget());
        assertEquals(TMTextUnitVariant.Status.APPROVED, next.getStatus());

        next = iterator.next();
        assertEquals("Au revoir", next.getTarget());
        assertEquals(TMTextUnitVariant.Status.APPROVED, next.getStatus());

        assertFalse(iterator.hasNext());

    }

    @Test
    public void testImportLocalizedAssetNotFullyTranslated() throws RepositoryNameAlreadyUsedException, ExecutionException, InterruptedException, AssetUpdateException {

        baseTestImportLocalizedAsset(StatusForEqualTarget.APPROVED);

        RepositoryLocale repoLocale;
        try {
            repoLocale = repositoryService.addRepositoryLocale(repository, "fr-CA", "fr-FR", false);
        } catch (RepositoryLocaleCreationException e) {
            throw new RuntimeException(e);
        }

        String localizedAssetContent = "hello=Bonjour\nbye=Au revoir CA\nsource=target";
        tmService.importLocalizedAsset(asset, localizedAssetContent, repoLocale, StatusForEqualTarget.APPROVED, null).get();

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repository.getId());
        textUnitSearcherParameters.setStatusFilter(StatusFilter.TRANSLATED);
        textUnitSearcherParameters.setLocaleId(repoLocale.getLocale().getId());
        List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);

        Iterator<TextUnitDTO> iterator = textUnitDTOs.iterator();
        TextUnitDTO next = iterator.next();
        assertEquals("Au revoir CA", next.getTarget());
        assertEquals(TMTextUnitVariant.Status.APPROVED, next.getStatus());

        assertFalse(iterator.hasNext());

    }
    
     @Test
    public void testReImportLocalizedAsset() throws RepositoryNameAlreadyUsedException, ExecutionException, InterruptedException, AssetUpdateException {

        baseTestImportLocalizedAsset(StatusForEqualTarget.APPROVED);

        RepositoryLocale repoLocale;
        try {
            repoLocale = repositoryService.addRepositoryLocale(repository, "fr-CA", "fr-FR", false);
        } catch (RepositoryLocaleCreationException e) {
            throw new RuntimeException(e);
        }

        String localizedAssetContent = "hello=Bonjour\nbye=Au revoir CA\nsource=target";
        tmService.importLocalizedAsset(asset, localizedAssetContent, repoLocale, StatusForEqualTarget.APPROVED, null).get();
        tmService.importLocalizedAsset(asset, localizedAssetContent, repoLocale, StatusForEqualTarget.APPROVED, null).get();

        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repository.getId());
        textUnitSearcherParameters.setStatusFilter(StatusFilter.TRANSLATED);
        textUnitSearcherParameters.setLocaleId(repoLocale.getLocale().getId());
        List<TextUnitDTO> textUnitDTOs = textUnitSearcher.search(textUnitSearcherParameters);

        Iterator<TextUnitDTO> iterator = textUnitDTOs.iterator();
        TextUnitDTO next = iterator.next();
        assertEquals("Au revoir CA", next.getTarget());
        assertEquals(TMTextUnitVariant.Status.APPROVED, next.getStatus());

        assertFalse(iterator.hasNext());
    }

    private void baseTestImportLocalizedAsset(StatusForEqualTarget statusForEqualTarget) throws InterruptedException, ExecutionException, RepositoryNameAlreadyUsedException, AssetUpdateException, AssetUpdateException {
        repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        RepositoryLocale repoLocale;
        try {
            repoLocale = repositoryService.addRepositoryLocale(repository, "fr-FR");
        } catch (RepositoryLocaleCreationException e) {
            throw new RuntimeException(e);
        }

        String assetContent = "hello=Hello\nbye=Bye\nsource=target";
        asset = assetService.createAsset(repository.getId(), assetContent, "demo.properties");
        asset = assetRepository.findOne(asset.getId());
        assetId = asset.getId();
        tmId = repository.getTm().getId();

        PollableFuture<Asset> assetResult = assetService.addOrUpdateAssetAndProcessIfNeeded(repository.getId(), assetContent, asset.getPath(), null);
        try {
            pollableTaskService.waitForPollableTask(assetResult.getPollableTask().getId());
        } catch (PollableTaskException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        assetResult.get();

        String localizedAssetContent = "hello=Bonjour\nbye=Au revoir\nsource=target";
        tmService.importLocalizedAsset(asset, localizedAssetContent, repoLocale, statusForEqualTarget, null).get();
    }

    @Test
    public void testImportLocalizedAssetPoPluralSinglePlural() throws Exception {
        repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        RepositoryLocale repoLocale;
        try {
            repoLocale = repositoryService.addRepositoryLocale(repository, "ja-JP");
        } catch (RepositoryLocaleCreationException e) {
            throw new RuntimeException(e);
        }

        String assetContent = "# SOME DESCRIPTIVE TITLE.\n"
                + "# Copyright (C) YEAR THE PACKAGE'S COPYRIGHT HOLDER\n"
                + "# This file is distributed under the same license as the PACKAGE package.\n"
                + "# FIRST AUTHOR <EMAIL@ADDRESS>, YEAR.\n"
                + "#\n"
                + "#, fuzzy\n"
                + "msgid \"\"\n"
                + "msgstr \"\"\n"
                + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
                + "\"Report-Msgid-Bugs-To: \\n\"\n"
                + "\"POT-Creation-Date: 2017-02-24 11:50-0800\\n\"\n"
                + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
                + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
                + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
                + "\"Language: \\n\"\n"
                + "\"MIME-Version: 1.0\\n\"\n"
                + "\"Plural-Forms: nplurals=2; plural=(n != 1);\\n\"\n"
                + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
                + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
                + " \n"
                + "   \n"
                + "#. Test plural\n"
                + "#: file.js:20\n"
                + "msgctxt \"car\"\n"
                + "msgid \"There is {number} car\"\n"
                + "msgid_plural \"There are {number} cars\"\n"
                + "msgstr[0] \"\"\n"
                + "msgstr[1] \"\"\n"
                + "\n"
                + "#. Test plural okapi bug\n"
                + "#: file.js:24\n"
                + "msgctxt \"testpluralokapibug\"\n"
                + "msgid \"test okapi bug\"\n"
                + "msgstr \"\"\n"
                + "";
        asset = assetService.createAsset(repository.getId(), assetContent, "messages.pot");
        asset = assetRepository.findOne(asset.getId());
        assetId = asset.getId();
        tmId = repository.getTm().getId();

        PollableFuture<Asset> assetResult = assetService.addOrUpdateAssetAndProcessIfNeeded(repository.getId(), assetContent, asset.getPath(), null);
        try {
            pollableTaskService.waitForPollableTask(assetResult.getPollableTask().getId());
        } catch (PollableTaskException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        assetResult.get();

        String localizedAssetContent = "# SOME DESCRIPTIVE TITLE.\n"
                + "# Copyright (C) YEAR THE PACKAGE'S COPYRIGHT HOLDER\n"
                + "# This file is distributed under the same license as the PACKAGE package.\n"
                + "# FIRST AUTHOR <EMAIL@ADDRESS>, YEAR.\n"
                + "#\n"
                + "#, fuzzy\n"
                + "msgid \"\"\n"
                + "msgstr \"\"\n"
                + "\"Project-Id-Version: PACKAGE VERSION\\n\"\n"
                + "\"Report-Msgid-Bugs-To: \\n\"\n"
                + "\"POT-Creation-Date: 2017-02-24 11:50-0800\\n\"\n"
                + "\"PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n\"\n"
                + "\"Last-Translator: FULL NAME <EMAIL@ADDRESS>\\n\"\n"
                + "\"Language-Team: LANGUAGE <LL@li.org>\\n\"\n"
                + "\"Language: \\n\"\n"
                + "\"MIME-Version: 1.0\\n\"\n"
                + "\"Plural-Forms: nplurals=1; plural=0;\\n\"\n"
                + "\"Content-Type: text/plain; charset=utf-8\\n\"\n"
                + "\"Content-Transfer-Encoding: 8bit\\n\"\n"
                + "  \n"
                + "   \n"
                + "#. Test plural\n"
                + "#: file.js:20\n"
                + "msgctxt \"car\"\n"
                + "msgid \"There is {number} car\"\n"
                + "msgid_plural \"There are {number} cars\"\n"
                + "msgstr[0] \"There is {number} car\"\n"
                + "\n"
                + "#. Test plural okapi bug\n"
                + "#: file.js:24\n"
                + "msgctxt \"testpluralokapibug\"\n"
                + "msgid \"test okapi bug\"\n"
                + "msgstr \"jp test okapi bug\"\n"
                + "\n"
                + "";
            tmService.importLocalizedAsset(asset, localizedAssetContent, repoLocale, StatusForEqualTarget.APPROVED, null).get();
    }

    @Test
    public void testExportAssetAsXLIFFAsync() throws Exception {
        createTestData();

        logger.debug("Export empty TM for source (en)");
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:okp=\"okapi-framework:xliff-extensions\">\n"
                + "<file original=\"test-asset-path.xliff\" source-language=\"en\" target-language=\"en\" datatype=\"x-undefined\" okp:inputEncoding=\"UTF-8\">\n"
                + "<body>\n"
                + "</body>\n"
                + "</file>\n"
                + "</xliff>\n";

        TMXliff tmXliff = tmService.createTMXliff(assetId, "en", null, null);
        PollableFuture<String> exportResult = tmService.exportAssetAsXLIFFAsync(tmXliff.getId(), assetId, "en", PollableTask.INJECT_CURRENT_TASK);

        try {
            pollableTaskService.waitForPollableTask(exportResult.getPollableTask().getId());
        } catch (PollableTaskException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        String exportAssetAsXLIFF = exportResult.get();
        assertEquals(expected, exportAssetAsXLIFF);

        PollableTask pollableTask = pollableTaskService.getPollableTask(exportResult.getPollableTask().getId());
        tmXliff = tmXliffRepository.findByPollableTask(pollableTask);
        assertEquals(expected, tmXliff.getContent());
    }
}
