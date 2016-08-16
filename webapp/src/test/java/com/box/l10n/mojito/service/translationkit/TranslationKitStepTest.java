package com.box.l10n.mojito.service.translationkit;

import static com.box.l10n.mojito.common.Mocks.getJpaRepositoryMockForGetOne;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TranslationKit;
import com.box.l10n.mojito.entity.TranslationKitTextUnit;
import com.box.l10n.mojito.okapi.TextUnitDTOAnnotation;
import com.box.l10n.mojito.okapi.TextUnitDTOAnnotations;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import java.util.ArrayList;
import java.util.List;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.TextUnit;
import static org.junit.Assert.*;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jaurambault
 */
public class TranslationKitStepTest {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(TranslationKitStepTest.class);

    @Test
    public void testHandleStartDocument() {

        logger.debug("Ids for entities worked on");
        Long translationKitId = 987654321L;

        logger.debug("Create instance to be tested, inject mocks and state");
        TranslationKitStep translationKitStep = new TranslationKitStep(translationKitId);
        translationKitStep.translationKitTextUnits = null;

        logger.debug("Build test input");
        Event event = new Event(EventType.START_DOCUMENT);

        logger.debug("Test and assert");
        Event result = translationKitStep.handleStartDocument(event);

        assertEquals("the input event should be returned", event, result);
        assertNotNull("TranslationKitTextUnits must have been initialized", translationKitStep.translationKitTextUnits);
    }

    @Test
    public void testHandleTextUnit() {

        logger.debug("Ids for entities worked on");
        Long translationKitId = 987654321L;
        Long tmTextUnitId = 654987321L;
        Long tmTextUnitVariantId = 123456789L;
        TMTextUnit tmTextUnit = new TMTextUnit();
        tmTextUnit.setId(tmTextUnitId);
        tmTextUnit.setWordCount(1);

        logger.debug("State of the instance to be checked");
        List<TranslationKitTextUnit> translationKitTextUnits = new ArrayList<>();

        logger.debug("Create mocks");
        TranslationKitRepository mockTranslationKitRepository = getJpaRepositoryMockForGetOne(TranslationKitRepository.class, TranslationKit.class, translationKitId);
        TMTextUnitVariantRepository mockTmTextUnitVariantRepository = getJpaRepositoryMockForGetOne(TMTextUnitVariantRepository.class, TMTextUnitVariant.class, tmTextUnitVariantId);
        TMTextUnitRepository mockTmTextUnitRepository = mock(TMTextUnitRepository.class);
        when(mockTmTextUnitRepository.getOne(tmTextUnitId)).thenReturn(tmTextUnit);

        logger.debug("Create instance to be tested, inject mocks and state");
        TranslationKitStep translationKitStep = new TranslationKitStep(translationKitId);
        translationKitStep.textUnitDTOAnnotations = new TextUnitDTOAnnotations();
        translationKitStep.translationKitRepository = mockTranslationKitRepository;
        translationKitStep.tmTextUnitRepository = mockTmTextUnitRepository;
        translationKitStep.tmTextUnitVariantRepository = mockTmTextUnitVariantRepository;
        translationKitStep.translationKitTextUnits = translationKitTextUnits;

        logger.debug("Build test input");
        ITextUnit textUnit = new TextUnit(tmTextUnitId.toString());
        TextUnitDTO textUnitDTO = new TextUnitDTO();
        textUnitDTO.setTmTextUnitVariantId(tmTextUnitVariantId);
        textUnit.setAnnotation(new TextUnitDTOAnnotation(textUnitDTO));

        Event event = new Event(EventType.TEXT_UNIT);
        event.setResource(textUnit);

        logger.debug("Test and assert");
        Event result = translationKitStep.handleTextUnit(event);

        assertEquals("the input event should be returned", event, result);
        assertEquals("A TranslationKitTextUnit must be added", 1, translationKitTextUnits.size());

        TranslationKitTextUnit translationKitTextUnit = translationKitTextUnits.get(0);
        assertEquals("translationKitId must match what was in the TextUnitDTO", translationKitId, translationKitTextUnit.getTranslationKit().getId());
        assertEquals("tmTextUnitId must match what was in the TextUnitDTO", tmTextUnitId, translationKitTextUnit.getTmTextUnit().getId());
        assertEquals("tmTextUnitVariantId of entites must match what was in the TextUnitDTO", tmTextUnitVariantId, translationKitTextUnit.getExportedTmTextUnitVariant().getId());
    }

    @Test
    public void testHandleEndDocument() {

        logger.debug("Ids for entities worked on");
        Long translationKitId = 987654321L;

        logger.debug("State of the instance to be checked");
        List<TranslationKitTextUnit> translationKitTextUnits = new ArrayList<>();

        logger.debug("Create mocks");
        TranslationKitService mockTranslationKitService = mock(TranslationKitService.class);

        logger.debug("Create instance to be tested, inject mocks and state");
        TranslationKitStep translationKitStep = new TranslationKitStep(translationKitId);
        translationKitStep.translationKitService = mockTranslationKitService;
        translationKitStep.translationKitTextUnits = translationKitTextUnits;

        logger.debug("Build test input");
        Event event = new Event(EventType.END_DOCUMENT);

        logger.debug("Test and assert");
        Event result = translationKitStep.handleEndDocument(event);

        assertEquals("the input event should be returned", event, result);
        verify(mockTranslationKitService).updateTranslationKitWithTmTextUnits(translationKitId, translationKitTextUnits, 0L);
    }

    @Test
    public void testGetTMTextUnitVariant() {

        logger.debug("Ids for entities worked on");
        Long tmTextUnitVariantId = 123456789L;

        logger.debug("Create mocks");
        TMTextUnitVariantRepository mockTmTextUnitVariantRepository = getJpaRepositoryMockForGetOne(TMTextUnitVariantRepository.class, TMTextUnitVariant.class, tmTextUnitVariantId);

        logger.debug("Create instance to be tested, inject mocks and state");
        TranslationKitStep translationKitStep = new TranslationKitStep(987654321L);
        translationKitStep.textUnitDTOAnnotations = new TextUnitDTOAnnotations();
        translationKitStep.tmTextUnitVariantRepository = mockTmTextUnitVariantRepository;

        logger.debug("Build test input");
        ITextUnit textUnit = new TextUnit();
        TextUnitDTO textUnitDTO = new TextUnitDTO();
        textUnitDTO.setTmTextUnitVariantId(tmTextUnitVariantId);
        textUnit.setAnnotation(new TextUnitDTOAnnotation(textUnitDTO));

        logger.debug("Test and assert");
        TMTextUnitVariant result = translationKitStep.getTMTextUnitVariant(textUnit);

        assertEquals("The tmTextUnitVariant#Id extracted from the text unit must be the same as the one set in the TextUnitDTO", tmTextUnitVariantId, result.getId());
    }

}
