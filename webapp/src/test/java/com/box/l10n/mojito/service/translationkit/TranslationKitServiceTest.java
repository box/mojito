package com.box.l10n.mojito.service.translationkit;

import com.box.l10n.mojito.entity.Drop;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TranslationKit;
import com.box.l10n.mojito.entity.TranslationKitTextUnit;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.drop.DropService;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTestData;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.box.l10n.mojito.test.XliffUtils;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author jaurambault
 */
public class TranslationKitServiceTest extends ServiceTestBase {

    static Logger logger = LoggerFactory.getLogger(TranslationKitServiceTest.class);

    @Autowired
    TranslationKitService translationKitService;

    @Autowired
    DropService dropService = new DropService();

    @Autowired
    TranslationKitRepository translationKitRepository;

    @Autowired
    TranslationKitTextUnitRepository translationKitTextUnitRepository;

    @Autowired
    TMService tmService;

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    TMTestData tmTestData;

    @Test
    public void testGetXLIFF() {

        tmTestData = new TMTestData(testIdWatcher);

        Drop drop = dropService.createDrop(tmTestData.repository);

        TranslationKitAsXliff translationKitAsXLIFF = translationKitService.generateTranslationKitAsXLIFF(
                drop.getId(),
                tmTestData.tm.getId(),
                tmTestData.frCA.getId(),
                TranslationKit.Type.TRANSLATION);

        logger.debug(translationKitAsXLIFF.getContent());

        String xliffWithoutIds = XliffUtils.replaceXliffVariableContent(translationKitAsXLIFF.getContent());
        logger.debug(xliffWithoutIds);

        assertEquals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:okp=\"okapi-framework:xliff-extensions\">\n"
                + "<file original=\"replaced-original\" source-language=\"en\" target-language=\"fr-CA\" datatype=\"x-undefined\" okp:inputEncoding=\"UTF-8\">\n"
                + "<body>\n"
                + "<trans-unit id=\"replaced-id\" resname=\"zuora_error_message_verify_state_province\" xml:space=\"preserve\">\n"
                + "<source xml:lang=\"en\">Please enter a valid state, region or province</source>\n"
                + "<target xml:lang=\"fr-CA\" state=\"new\">Please enter a valid state, region or province</target>\n"
                + "<note>Comment1</note>\n"
                + "</trans-unit>\n"
                + "</body>\n"
                + "</file>\n"
                + "</xliff>\n", xliffWithoutIds);

        logger.debug("Check the translation kit entities");
        TranslationKit translationKit = translationKitRepository.findOne(translationKitAsXLIFF.getTranslationKitId());
        List<TranslationKitTextUnit> findByTranslationKitId = translationKitTextUnitRepository.findByTranslationKit(translationKit);

        assertEquals("The translation kit must be of type translation", TranslationKit.Type.TRANSLATION, translationKit.getType());
        assertEquals("There must be 1 TranslationKitTextUnit", 1, findByTranslationKitId.size());

        assertEquals("Check the first TextUnit by name", findByTranslationKitId.get(0).getTmTextUnit().getName(), "zuora_error_message_verify_state_province");
        assertNull("There shouldn't be any TmTextUnitVariant for this TextUnit", findByTranslationKitId.get(0).getExportedTmTextUnitVariant());

    }

    @Test
    @Transactional
    public void testTranslationNeeded() {

        tmTestData = new TMTestData(testIdWatcher);

        logger.debug("Mark on translated string as need review");
        tmService.addTMTextUnitCurrentVariant(
                tmTestData.addCurrentTMTextUnitVariant1FrFR.getTmTextUnit().getId(),
                tmTestData.addCurrentTMTextUnitVariant1FrFR.getLocale().getId(),
                tmTestData.addCurrentTMTextUnitVariant1FrFR.getContent(),
                "this translation is bad in context",
                TMTextUnitVariant.Status.TRANSLATION_NEEDED);

        Drop drop = dropService.createDrop(tmTestData.repository);

        TranslationKitAsXliff translationKitAsXLIFF = translationKitService.generateTranslationKitAsXLIFF(
                drop.getId(),
                tmTestData.tm.getId(),
                tmTestData.frFR.getId(),
                TranslationKit.Type.TRANSLATION);

        logger.debug(translationKitAsXLIFF.getContent());

        String xliffWithoutIds = XliffUtils.replaceXliffVariableContent(translationKitAsXLIFF.getContent());
        logger.debug(xliffWithoutIds);

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:okp=\"okapi-framework:xliff-extensions\">\n"
                + "<file original=\"replaced-original\" source-language=\"en\" target-language=\"fr-FR\" datatype=\"x-undefined\" okp:inputEncoding=\"UTF-8\">\n"
                + "<body>\n"
                + "<trans-unit id=\"replaced-id\" resname=\"zuora_error_message_verify_state_province\" xml:space=\"preserve\">\n"
                + "<source xml:lang=\"en\">Please enter a valid state, region or province</source>\n"
                + "<target xml:lang=\"fr-FR\" state=\"needs-translation\">Veuillez indiquer un état, une région ou une province valide.</target>\n"
                + "<note>Comment1</note>\n"
                + "<note annotates=\"target\" from=\"reviewer\">this translation is bad in context</note>\n"
                + "<note annotates=\"target\" from=\"automation\">OK</note>\n"
                + "</trans-unit>\n"
                + "<trans-unit id=\"replaced-id\" resname=\"TEST2\" xml:space=\"preserve\">\n"
                + "<source xml:lang=\"en\">Content2</source>\n"
                + "<target xml:lang=\"fr-FR\" state=\"new\">Content2</target>\n"
                + "<note>Comment2</note>\n"
                + "</trans-unit>\n"
                + "</body>\n"
                + "</file>\n"
                + "</xliff>\n"
                + "", xliffWithoutIds);

        logger.debug("Check the translation kit entities");
        TranslationKit translationKit = translationKitRepository.findOne(translationKitAsXLIFF.getTranslationKitId());
        List<TranslationKitTextUnit> findByTranslationKitId = translationKitTextUnitRepository.findByTranslationKit(translationKit);

        assertEquals("The translation kit must be of type review", TranslationKit.Type.TRANSLATION, translationKit.getType());
        assertEquals("There must be 2 TranslationKitTextUnits", 2, findByTranslationKitId.size());

        int idx = 0;
        assertEquals("Check the first TextUnit by name", findByTranslationKitId.get(idx).getTmTextUnit().getName(), "zuora_error_message_verify_state_province");
        assertEquals("Check the exported TmTextUnitVariant by looking at the content", "Veuillez indiquer un état, une région ou une province valide.", findByTranslationKitId.get(idx).getExportedTmTextUnitVariant().getContent());

        idx++;
        assertEquals("Check the second TextUnit by name", findByTranslationKitId.get(idx).getTmTextUnit().getName(), "TEST2");
        assertNull("There shouldn't be any TmTextUnitVariant for this TextUnit", findByTranslationKitId.get(idx).getExportedTmTextUnitVariant());
    }

    @Test
    @Transactional
    public void testReviewKit() {

        tmTestData = new TMTestData(testIdWatcher);

        logger.debug("Mark on translated string as need review");
        tmService.addTMTextUnitCurrentVariant(
                tmTestData.addCurrentTMTextUnitVariant1FrFR.getTmTextUnit().getId(),
                tmTestData.addCurrentTMTextUnitVariant1FrFR.getLocale().getId(),
                tmTestData.addCurrentTMTextUnitVariant1FrFR.getContent(),
                null,
                TMTextUnitVariant.Status.REVIEW_NEEDED);

        Drop drop = dropService.createDrop(tmTestData.repository);

        TranslationKitAsXliff translationKitAsXLIFF = translationKitService.generateTranslationKitAsXLIFF(
                drop.getId(),
                tmTestData.tm.getId(),
                tmTestData.frFR.getId(),
                TranslationKit.Type.REVIEW);

        logger.debug(translationKitAsXLIFF.getContent());

        String xliffWithoutIds = XliffUtils.replaceXliffVariableContent(translationKitAsXLIFF.getContent());
        logger.info(xliffWithoutIds);

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:okp=\"okapi-framework:xliff-extensions\">\n"
                + "<file original=\"replaced-original\" source-language=\"en\" target-language=\"fr-FR\" datatype=\"x-undefined\" okp:inputEncoding=\"UTF-8\">\n"
                + "<body>\n"
                + "<trans-unit id=\"replaced-id\" resname=\"zuora_error_message_verify_state_province\" xml:space=\"preserve\">\n"
                + "<source xml:lang=\"en\">Please enter a valid state, region or province</source>\n"
                + "<target xml:lang=\"fr-FR\" state=\"needs-review-translation\">Veuillez indiquer un état, une région ou une province valide.</target>\n"
                + "<note>Comment1</note>\n"
                + "<note annotates=\"target\" from=\"automation\">NEEDS REVIEW</note>\n"
                + "</trans-unit>\n"
                + "</body>\n"
                + "</file>\n"
                + "</xliff>\n"
                + "", xliffWithoutIds);

        logger.debug("Check the translation kit entities");
        TranslationKit translationKit = translationKitRepository.findOne(translationKitAsXLIFF.getTranslationKitId());
        List<TranslationKitTextUnit> findByTranslationKitId = translationKitTextUnitRepository.findByTranslationKit(translationKit);

        assertEquals("The translation kit must be of type review", TranslationKit.Type.REVIEW, translationKit.getType());
        assertEquals("There must be 1 TranslationKitTextUnits", 1, findByTranslationKitId.size());

        int idx = 0;
        assertEquals("Check the first TextUnit by name", findByTranslationKitId.get(idx).getTmTextUnit().getName(), "zuora_error_message_verify_state_province");
        assertEquals("Check the exported TmTextUnitVariant by looking at the content", "Veuillez indiquer un état, une région ou une province valide.", findByTranslationKitId.get(idx).getExportedTmTextUnitVariant().getContent());
    }

    @Test
    @Transactional
    public void testCheckEntitiesAndGetTranslationKitExportedAndCurrentTUVs() {

        tmTestData = new TMTestData(testIdWatcher);

        logger.debug("Mark on translated string as need review");
        tmService.addTMTextUnitCurrentVariant(
                tmTestData.addCurrentTMTextUnitVariant1FrFR.getTmTextUnit().getId(),
                tmTestData.addCurrentTMTextUnitVariant1FrFR.getLocale().getId(),
                tmTestData.addCurrentTMTextUnitVariant1FrFR.getContent(),
                "this translation is bad in context",
                TMTextUnitVariant.Status.TRANSLATION_NEEDED);

        Drop drop = dropService.createDrop(tmTestData.repository);

        TranslationKitAsXliff translationKitAsXLIFF = translationKitService.generateTranslationKitAsXLIFF(
                drop.getId(),
                tmTestData.tm.getId(),
                tmTestData.frFR.getId(),
                TranslationKit.Type.TRANSLATION);

        TranslationKit translationKit = translationKitRepository.findOne(translationKitAsXLIFF.getTranslationKitId());

        logger.debug("Check the translation kit entities");
        List<TranslationKitTextUnit> findByTranslationKitTextUnits = translationKitTextUnitRepository.findByTranslationKit(translationKit);

        assertEquals("The translation kit must be of type review", TranslationKit.Type.TRANSLATION, translationKit.getType());
        assertEquals("There must be 2 TranslationKitTextUnits", 2, findByTranslationKitTextUnits.size());

        int idx = 0;
        TranslationKitTextUnit translationKitTextUnitZuora = findByTranslationKitTextUnits.get(idx);
        assertEquals("Check that there is a TranslationKitTextUnit for zuora", translationKitTextUnitZuora.getTmTextUnit().getName(), "zuora_error_message_verify_state_province");
        assertEquals("Check the exported TmTextUnitVariant for zuora", "Veuillez indiquer un état, une région ou une province valide.", translationKitTextUnitZuora.getExportedTmTextUnitVariant().getContent());

        idx++;
        TranslationKitTextUnit translationKitTextUnitTest2 = findByTranslationKitTextUnits.get(idx);
        assertEquals("Check that there is a TextUnit for TEST2", translationKitTextUnitTest2.getTmTextUnit().getName(), "TEST2");
        assertNull("There shouldn't be any TmTextUnitVariant for this TextUnit", translationKitTextUnitTest2.getExportedTmTextUnitVariant());

        logger.debug("Test GetTranslationKitExportedAndCurrentTUVs and check against previously retreived entities");
        Map<Long, TranslationKitExportedImportedAndCurrentTUV> translationKitExportedAndCurrentTUVs = translationKitService.getTranslationKitExportedAndCurrentTUVs(translationKit.getId());

        idx = 0;
        TranslationKitExportedImportedAndCurrentTUV translationKitExportedAndCurrentTUVZuora = translationKitExportedAndCurrentTUVs.get(translationKitTextUnitZuora.getTmTextUnit().getId());
        assertNotNull("There must be an entry for zuora ", translationKitExportedAndCurrentTUVZuora);
        assertEquals("current TmTextUnitVariant from getTranslationKitExportedAndCurrentTUVs invalid", translationKitTextUnitZuora.getExportedTmTextUnitVariant().getId(), translationKitExportedAndCurrentTUVZuora.getCurrentTmTextUnitVariant());
        assertEquals("export TmTextUnitVariant from getTranslationKitExportedAndCurrentTUVs invalid", translationKitTextUnitZuora.getExportedTmTextUnitVariant().getId(), translationKitExportedAndCurrentTUVZuora.getExportedTmTextUnitVariant());

        idx++;
        TranslationKitExportedImportedAndCurrentTUV translationKitExportedAndCurrentTUVTest2 = translationKitExportedAndCurrentTUVs.get(translationKitTextUnitTest2.getTmTextUnit().getId());
        assertNotNull("There must be an entry for TEST2" , translationKitExportedAndCurrentTUVTest2);
        assertNull("current TmTextUnitVariant for TEST2 from getTranslationKitExportedAndCurrentTUVs invalid", translationKitExportedAndCurrentTUVTest2.getCurrentTmTextUnitVariant());
        assertNull("export TmTextUnitVariant for TEST2 from getTranslationKitExportedAndCurrentTUVs invalid", translationKitExportedAndCurrentTUVTest2.getExportedTmTextUnitVariant());
    }

    @Test
    public void testGetTextUnitDTOsForTranslationKitfrFR() {

        tmTestData = new TMTestData(testIdWatcher);

        TranslationKit translationKit = translationKitService.addTranslationKit(
                dropService.createDrop(tmTestData.repository).getId(),
                tmTestData.frFR.getId(),
                TranslationKit.Type.TRANSLATION);

        List<TextUnitDTO> textUnitDTOsForTranslationKit = translationKitService.getTextUnitDTOsForTranslationKit(translationKit.getId(), TranslationKit.Type.TRANSLATION);

        for (TextUnitDTO textUnitDTO : textUnitDTOsForTranslationKit) {
            logger.debug(ToStringBuilder.reflectionToString(textUnitDTO, ToStringStyle.MULTI_LINE_STYLE));
        }

        Iterator<TextUnitDTO> iterator = textUnitDTOsForTranslationKit.iterator();
        TextUnitDTO next = iterator.next();
        assertEquals(tmTestData.asset.getId(), next.getAssetId());
        assertEquals("Comment2", next.getComment());
        assertEquals(tmTestData.frFR.getId(), next.getLocaleId());
        assertEquals("TEST2", next.getName());
        assertEquals("Content2", next.getSource());
        assertNull(next.getTarget());
        assertEquals("fr-FR", next.getTargetLocale());
        assertEquals(tmTestData.addTMTextUnit2.getId(), next.getTmTextUnitId());
        assertEquals(false, next.isTranslated());

        assertFalse(iterator.hasNext());
    }

    @Test
    public void testGetTextUnitDTOsForTranslationKitkoKR() {

        tmTestData = new TMTestData(testIdWatcher);

        TranslationKit translationKit = translationKitService.addTranslationKit(dropService.createDrop(
                tmTestData.repository).getId(),
                tmTestData.koKR.getId(),
                TranslationKit.Type.TRANSLATION);

        List<TextUnitDTO> textUnitDTOsForTranslationKit = translationKitService.getTextUnitDTOsForTranslationKit(translationKit.getId(), TranslationKit.Type.TRANSLATION);

        for (TextUnitDTO textUnitDTO : textUnitDTOsForTranslationKit) {
            logger.debug(ToStringBuilder.reflectionToString(textUnitDTO, ToStringStyle.MULTI_LINE_STYLE));
        }

        Iterator<TextUnitDTO> iterator = textUnitDTOsForTranslationKit.iterator();
        TextUnitDTO next = iterator.next();
        assertEquals(tmTestData.asset.getId(), next.getAssetId());
        assertEquals("Comment2", next.getComment());
        assertEquals(tmTestData.koKR.getId(), next.getLocaleId());
        assertEquals("TEST2", next.getName());
        assertEquals("Content2", next.getSource());
        assertNull(next.getTarget());
        assertEquals("ko-KR", next.getTargetLocale());
        assertEquals(tmTestData.addTMTextUnit2.getId(), next.getTmTextUnitId());
        assertEquals(false, next.isTranslated());

        assertFalse(iterator.hasNext());
    }

    @Test
    public void testGetTextUnitDTOsForTranslationKitfrCA() {

        tmTestData = new TMTestData(testIdWatcher);

        TranslationKit translationKit = translationKitService.addTranslationKit(
                dropService.createDrop(tmTestData.repository).getId(),
                tmTestData.frCA.getId(),
                TranslationKit.Type.TRANSLATION);

        List<TextUnitDTO> textUnitDTOsForTranslationKit = translationKitService.getTextUnitDTOsForTranslationKit(translationKit.getId(), TranslationKit.Type.TRANSLATION);

        for (TextUnitDTO textUnitDTO : textUnitDTOsForTranslationKit) {
            logger.debug(ToStringBuilder.reflectionToString(textUnitDTO, ToStringStyle.MULTI_LINE_STYLE));
        }

        Iterator<TextUnitDTO> iterator = textUnitDTOsForTranslationKit.iterator();
        TextUnitDTO next = iterator.next();
        assertEquals(tmTestData.asset.getId(), next.getAssetId());
        assertEquals("Comment1", next.getComment());
        assertEquals(tmTestData.frCA.getId(), next.getLocaleId());
        assertEquals("zuora_error_message_verify_state_province", next.getName());
        assertEquals("Please enter a valid state, region or province", next.getSource());
        assertNull(next.getTarget());
        assertEquals("fr-CA", next.getTargetLocale());
        assertEquals(tmTestData.addTMTextUnit1.getId(), next.getTmTextUnitId());
        assertEquals(false, next.isTranslated());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testGetTextUnitDTOsWithInheritanceForTranslationKitfrCA() {

        tmTestData = new TMTestData(testIdWatcher);

        TranslationKit translationKit = translationKitService.addTranslationKit(
                dropService.createDrop(tmTestData.repository).getId(),
                tmTestData.frCA.getId(),
                TranslationKit.Type.REVIEW);

        List<TextUnitDTO> textUnitDTOsForTranslationKit = translationKitService.getTextUnitDTOsForTranslationKitWithInheritance(translationKit.getId());

        for (TextUnitDTO textUnitDTO : textUnitDTOsForTranslationKit) {
            logger.debug(ToStringBuilder.reflectionToString(textUnitDTO, ToStringStyle.MULTI_LINE_STYLE));
        }

        Iterator<TextUnitDTO> iterator = textUnitDTOsForTranslationKit.iterator();
        TextUnitDTO next = iterator.next();
        assertEquals(tmTestData.asset.getId(), next.getAssetId());
        assertEquals("Comment1", next.getComment());
        assertEquals(tmTestData.frFR.getId(), next.getLocaleId());
        assertEquals("zuora_error_message_verify_state_province", next.getName());
        assertEquals("Please enter a valid state, region or province", next.getSource());
        assertEquals("fr-FR", next.getTargetLocale());
        assertEquals(tmTestData.addTMTextUnit1.getId(), next.getTmTextUnitId());
        assertEquals(tmTestData.addCurrentTMTextUnitVariant1FrFR.getContent(), next.getTarget());
        assertEquals(true, next.isTranslated());
        assertFalse(iterator.hasNext());
    }
}
