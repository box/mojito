package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariantComment;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.asset.AssetService;
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
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import net.sf.okapi.common.resource.TextUnit;
import org.hibernate.proxy.HibernateProxy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
        return addCurrentTMTextUnitVariant;
    }

    @Test
    public void testComputeTMTextUnitMD5() throws IOException {
        String computeTMTextUnitMD5 = tmService.computeTMTextUnitMD5("name", "this is the content", "some comment");
        assertEquals("3063c39d3cf8ab69bcabbbc5d7187dc9", computeTMTextUnitMD5);
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
        String localizedAsset = tmService.generateLocalized(asset, sourceXLIFF, repositoryLocale, null);

        String expectedLocalizedXLIFF = getExpectedLocalizedXLIFFContent(locale.getBcp47Tag(), tmTextUnit1, tmTextUnit2, tmTextUnit3, variant1);
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
        String localizedAsset = tmService.generateLocalized(asset, sourceXLIFF, repositoryLocale, outputBcp47tag);

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

        return xliffDataFactory.generateTargetXliff(Arrays.asList(
                xliffDataFactory.createTextUnit(tmTextUnit1.getId(), tmTextUnit1.getName(), tmTextUnit1.getContent(), tmTextUnit1.getComment(), variant1.getContent(), targetLocaleBcp47Tag, null),
                xliffDataFactory.createTextUnit(tmTextUnit2.getId(), tmTextUnit2.getName(), tmTextUnit2.getContent(), tmTextUnit2.getComment(), tmTextUnit2.getContent(), targetLocaleBcp47Tag, null),
                xliffDataFactory.createTextUnit(tmTextUnit3.getId(), tmTextUnit3.getName(), tmTextUnit3.getContent(), tmTextUnit3.getComment(), tmTextUnit3.getContent(), targetLocaleBcp47Tag, null)
        ), targetLocaleBcp47Tag);
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
                + "<file original=\"test-asset-path.xliff\" source-language=\"en\" target-language=\"fr-fr\" datatype=\"x-undefined\" okp:inputEncoding=\"UTF-8\">\n"
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
                + "<note>{\"sourceComment\":\"This text is shown in the start screen of the application. Keep it short.\",\"targetComment\":\"This text is shown in the start screen of the application. Keep it short.\",\"includedInLocalizedFile\":true,\"status\":\"APPROVED\",\"variantComments\":[]}</note>\n"
                + "</trans-unit>\n"
                + "<trans-unit id=\"\" resname=\"home\" xml:space=\"preserve\">\n"
                + "<source xml:lang=\"en\">Home</source>\n"
                + "<target xml:lang=\"en\">Home</target>\n"
                + "<note>{\"sourceComment\":\"This is the text displayed in the link that takes the user to the home page.\",\"targetComment\":\"This is the text displayed in the link that takes the user to the home page.\",\"includedInLocalizedFile\":true,\"status\":\"APPROVED\",\"variantComments\":[]}</note>\n"
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
                + "<file original=\"test-asset-path.xliff\" source-language=\"en\" target-language=\"fr-fr\" datatype=\"x-undefined\" okp:inputEncoding=\"UTF-8\">\n"
                + "<body>\n"
                + "<trans-unit id=\"\" resname=\"application_name\" xml:space=\"preserve\">\n"
                + "<source xml:lang=\"en\">Application Name</source>\n"
                + "<target xml:lang=\"fr-fr\">Nom de l'application</target>\n"
                + "<note>{\"sourceComment\":\"This text is shown in the start screen of the application. Keep it short.\",\"targetComment\":null,\"includedInLocalizedFile\":true,\"status\":\"APPROVED\",\"variantComments\":[]}</note>\n"
                + "</trans-unit>\n"
                + "<trans-unit id=\"\" resname=\"home\" xml:space=\"preserve\">\n"
                + "<source xml:lang=\"en\">Home</source>\n"
                + "<target xml:lang=\"fr-fr\">Page d'accueil</target>\n"
                + "<note>{\"sourceComment\":\"This is the text displayed in the link that takes the user to the home page.\",\"targetComment\":null,\"includedInLocalizedFile\":true,\"status\":\"APPROVED\",\"variantComments\":[]}</note>\n"
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
                + "<file original=\"test-asset-path.xliff\" source-language=\"en\" target-language=\"fr-fr\" datatype=\"x-undefined\" okp:inputEncoding=\"UTF-8\">\n"
                + "<body>\n"
                + "<trans-unit id=\"\" resname=\"application_name\" xml:space=\"preserve\">\n"
                + "<source xml:lang=\"en\">Application Name</source>\n"
                + "<target xml:lang=\"fr-fr\">Nom de l'application</target>\n"
                + "<note>{\"sourceComment\":\"This text is shown in the start screen of the application. Keep it short.\",\"targetComment\":null,\"includedInLocalizedFile\":true,\"status\":\"APPROVED\",\"variantComments\":[]}</note>\n"
                + "</trans-unit>\n"
                + "<trans-unit id=\"\" resname=\"home\" xml:space=\"preserve\">\n"
                + "<source xml:lang=\"en\">Home</source>\n"
                + "<target xml:lang=\"fr-fr\">Page d'accueil</target>\n"
                + "<note>{\"sourceComment\":\"This is the text displayed in the link that takes the user to the home page.\",\"targetComment\":\"this string need to be reviewed because...\",\"includedInLocalizedFile\":true,\"status\":\"REVIEW_NEEDED\",\"variantComments\":[]}</note>\n"
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
                + "<file original=\"test-asset-path.xliff\" source-language=\"en\" target-language=\"fr-fr\" datatype=\"x-undefined\" okp:inputEncoding=\"UTF-8\">\n"
                + "<body>\n"
                + "<trans-unit id=\"\" resname=\"application_name\" xml:space=\"preserve\">\n"
                + "<source xml:lang=\"en\">Application Name</source>\n"
                + "<target xml:lang=\"fr-fr\">Nom de l'application</target>\n"
                + "<note>{\"sourceComment\":\"This text is shown in the start screen of the application. Keep it short.\",\"targetComment\":null,\"includedInLocalizedFile\":true,\"status\":\"APPROVED\",\"variantComments\":[]}</note>\n"
                + "</trans-unit>\n"
                + "<trans-unit id=\"\" resname=\"home\" xml:space=\"preserve\">\n"
                + "<source xml:lang=\"en\">Home</source>\n"
                + "<target xml:lang=\"fr-fr\">Page d'accueil</target>\n"
                + "<note>{\"sourceComment\":\"This is the text displayed in the link that takes the user to the home page.\",\"targetComment\":\"this string has some comments\",\"includedInLocalizedFile\":false,\"status\":\"REVIEW_NEEDED\",\"variantComments\":[{\"severity\":\"INFO\",\"type\":\"LEVERAGING\",\"content\":\"Leveraging\",\"createdByUser\":{\"username\":\"admin\",\"enabled\":true,\"surname\":null,\"givenName\":null,\"commonName\":null,\"authorities\":[{\"authority\":\"ROLE_ADMIN\"}]}},{\"severity\":\"ERROR\",\"type\":\"INTEGRITY_CHECK\",\"content\":\"Failed Integrity Check\",\"createdByUser\":{\"username\":\"admin\",\"enabled\":true,\"surname\":null,\"givenName\":null,\"commonName\":null,\"authorities\":[{\"authority\":\"ROLE_ADMIN\"}]}}]}</note>\n"
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
     * This test is to test {@link XMLEncoder} with option to override encoding of &lt; and &gt;
     * 
     * According to Android specification in http://developer.android.com/guide/topics/resources/string-resource.html,
     * <b>bold</b>, <i>italian</i> and <u>underline</u> should be in localized file as-is.
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
        
        PollableFuture<Asset> assetResult = assetService.addOrUpdateAssetAndProcessIfNeeded(repo.getId(), assetContent, asset.getPath());
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
        
        String localizedAsset = tmService.generateLocalized(asset, assetContent, repoLocale, "en-GB");
        logger.debug("localized=\n{}", localizedAsset);
        assertEquals(assetContent, localizedAsset);
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
        
        PollableFuture<Asset> assetResult = assetService.addOrUpdateAssetAndProcessIfNeeded(repo.getId(), assetContent, asset.getPath());
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
        
        String localizedAsset = tmService.generateLocalized(asset, assetContent, repoLocale, "en-GB");
        logger.debug("localized=\n{}", localizedAsset);
        assertEquals(assetContent, localizedAsset);
    }
    
    /**
     * This test is to test {@link MacStringsEncoder} with special characters
     * 
     * According to iOS specification in https://developer.apple.com/library/ios/documentation/Cocoa/Conceptual/LoadingResources/Strings/Strings.html,
     * the following characters should be escaped with backslash: double-quote, backslash, newline(\n), carriage return (\r).
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
        
        PollableFuture<Asset> assetResult = assetService.addOrUpdateAssetAndProcessIfNeeded(repo.getId(), assetContent, asset.getPath());
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
        
        String localizedAsset = tmService.generateLocalized(asset, assetContent, repoLocale, "en-GB");
        logger.debug("localized=\n{}", localizedAsset);
        assertEquals(assetContent, localizedAsset);
    }
}
