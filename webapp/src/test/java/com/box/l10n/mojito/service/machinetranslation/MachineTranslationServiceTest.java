package com.box.l10n.mojito.service.machinetranslation;

import com.box.l10n.mojito.service.leveraging.LeveragerByContentAndRepository;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
class MachineTranslationServiceTest {

    MachineTranslationService machineTranslationService;

    LeveragerByContentAndRepository leveragerByContentAndRepositoryMock;

    MachineTranslationEngine machineTranslationEngine = new NoOpEngine();

    MachineTranslationServiceTest() {
        machineTranslationService = spy(new MachineTranslationService(
                machineTranslationEngine,
                new TranslationMerger()));

        leveragerByContentAndRepositoryMock = mock(LeveragerByContentAndRepository.class);
        when(machineTranslationService.getLeveragerByContentAndRepository(null, null))
                .thenReturn(leveragerByContentAndRepositoryMock);
    }

    @Test
    void testGetTranslationsBatchWorks() {
        TextUnitDTO leveragedTextUnitDTO = new TextUnitDTO();
        leveragedTextUnitDTO.setTarget("targetTranslation");
        leveragedTextUnitDTO.setTmTextUnitId(1L);
        leveragedTextUnitDTO.setTmTextUnitVariantId(2L);
        String targetLocale = "fr";
        leveragedTextUnitDTO.setTargetLocale(targetLocale);

        when(leveragerByContentAndRepositoryMock.getLeveragingMatches(any(), any(), any()))
                .thenReturn(ImmutableList.of(leveragedTextUnitDTO));

        String sourceText1 = "unique source text";
        String sourceText2 = "other text";
        TranslationsResponseDTO translationsResponseDTO = machineTranslationService.getTranslations(
                ImmutableList.of(sourceText1, sourceText2),
                "en",
                ImmutableList.of("fr", "de"),
                false,
                false,
                null,
                null);

        List<TranslationDTO> translationsText1 = translationsResponseDTO.getTextUnitTranslations()
                .stream()
                .filter(textUnitTranslationGroupDTO -> textUnitTranslationGroupDTO.getSourceText().equals(sourceText1))
                .findFirst()
                .get()
                .getTranslations();

        List<TranslationDTO> translationsText2 = translationsResponseDTO.getTextUnitTranslations()
                .stream()
                .filter(textUnitTranslationGroupDTO -> textUnitTranslationGroupDTO.getSourceText().equals(sourceText2))
                .findFirst()
                .get()
                .getTranslations();

        TranslationDTO actualText1TranslationLeveraged = translationsText1
                .stream()
                .filter(t -> t.getTranslationSource() == TranslationSource.MOJITO_TM_LEVERAGE)
                .findFirst()
                .get();
        TranslationDTO actualText1TranslationMTed = translationsText1
                .stream()
                .filter(t -> t.getTranslationSource() == TranslationSource.NOOP)
                .findFirst()
                .get();

        Assertions.assertEquals(leveragedTextUnitDTO.getTarget(), actualText1TranslationLeveraged.getText());
        Assertions.assertEquals(TranslationSource.MOJITO_TM_LEVERAGE, actualText1TranslationLeveraged.getTranslationSource());
        Assertions.assertEquals(leveragedTextUnitDTO.getTmTextUnitId(), actualText1TranslationLeveraged.getMatchedTextUnitId());
        Assertions.assertEquals(leveragedTextUnitDTO.getTmTextUnitVariantId(), actualText1TranslationLeveraged.getMatchedTextUnitVariantId());
        Assertions.assertEquals("fr", actualText1TranslationLeveraged.getBcp47Tag());

        Assertions.assertEquals(sourceText1, actualText1TranslationMTed.getText());
        Assertions.assertEquals(TranslationSource.NOOP, actualText1TranslationMTed.getTranslationSource());
        Assertions.assertEquals(0L, actualText1TranslationMTed.getMatchedTextUnitId());
        Assertions.assertEquals(0l, actualText1TranslationMTed.getMatchedTextUnitVariantId());
        Assertions.assertEquals("de", actualText1TranslationMTed.getBcp47Tag());

        TranslationDTO actualText2TranslationLeveraged = translationsText2
                .stream()
                .filter(t -> t.getTranslationSource() == TranslationSource.MOJITO_TM_LEVERAGE)
                .findFirst()
                .get();
        TranslationDTO actualText2TranslationMTed = translationsText2
                .stream()
                .filter(t -> t.getTranslationSource() == TranslationSource.NOOP)
                .findFirst()
                .get();

        Assertions.assertEquals(leveragedTextUnitDTO.getTarget(), actualText2TranslationLeveraged.getText());
        Assertions.assertEquals(TranslationSource.MOJITO_TM_LEVERAGE, actualText2TranslationLeveraged.getTranslationSource());
        Assertions.assertEquals(leveragedTextUnitDTO.getTmTextUnitId(), actualText2TranslationLeveraged.getMatchedTextUnitId());
        Assertions.assertEquals(leveragedTextUnitDTO.getTmTextUnitVariantId(), actualText2TranslationLeveraged.getMatchedTextUnitVariantId());
        Assertions.assertEquals("fr", actualText2TranslationLeveraged.getBcp47Tag());

        Assertions.assertEquals(sourceText2, actualText2TranslationMTed.getText());
        Assertions.assertEquals(TranslationSource.NOOP, actualText2TranslationMTed.getTranslationSource());
        Assertions.assertEquals(0L, actualText2TranslationMTed.getMatchedTextUnitId());
        Assertions.assertEquals(0l, actualText2TranslationMTed.getMatchedTextUnitVariantId());
        Assertions.assertEquals("de", actualText2TranslationMTed.getBcp47Tag());
    }

    @Test
    void testGetSingleTranslationWorks() {
        TextUnitDTO leveragedTextUnitDTO = new TextUnitDTO();
        leveragedTextUnitDTO.setTarget("targetTranslation");
        leveragedTextUnitDTO.setTmTextUnitId(1L);
        leveragedTextUnitDTO.setTmTextUnitVariantId(2L);
        String targetLocale = "fr";
        leveragedTextUnitDTO.setTargetLocale(targetLocale);

        when(leveragerByContentAndRepositoryMock.getLeveragingMatches(any(), any(), any()))
                .thenReturn(ImmutableList.of(leveragedTextUnitDTO));

        TranslationDTO singleTranslation = machineTranslationService.getSingleTranslation(
                "unique source text",
                "en",
                targetLocale,
                false,
                false,
                null,
                null);

        Assertions.assertEquals(leveragedTextUnitDTO.getTarget(), singleTranslation.getText());
        Assertions.assertEquals(TranslationSource.MOJITO_TM_LEVERAGE, singleTranslation.getTranslationSource());
        Assertions.assertEquals(leveragedTextUnitDTO.getTmTextUnitId(), singleTranslation.getMatchedTextUnitId());
        Assertions.assertEquals(leveragedTextUnitDTO.getTmTextUnitVariantId(), singleTranslation.getMatchedTextUnitVariantId());
        Assertions.assertEquals(targetLocale, singleTranslation.getBcp47Tag());
    }

    @Test
    void testGetSingleTranslationMismatchedLocaleMachineTranslate() {
        TextUnitDTO leveragedTextUnitDTO = new TextUnitDTO();
        leveragedTextUnitDTO.setTarget("targetTranslation");
        leveragedTextUnitDTO.setTmTextUnitId(1L);
        leveragedTextUnitDTO.setTmTextUnitVariantId(2L);
        String targetLocale = "fr";
        leveragedTextUnitDTO.setTargetLocale(targetLocale);

        when(leveragerByContentAndRepositoryMock.getLeveragingMatches(any(), any(), any()))
                .thenReturn(ImmutableList.of(leveragedTextUnitDTO));

        String sourceText = "unique source text";
        String targetBcp47Tags = "no-no";
        TranslationDTO singleTranslation = machineTranslationService.getSingleTranslation(
                sourceText,
                "en",
                targetBcp47Tags,
                false,
                false,
                null,
                null);

        verify(leveragerByContentAndRepositoryMock, atLeastOnce()).getLeveragingMatches(any(), any(), any());
        Assertions.assertEquals(sourceText, singleTranslation.getText());
        Assertions.assertEquals(TranslationSource.NOOP, singleTranslation.getTranslationSource());
        Assertions.assertEquals(0L, singleTranslation.getMatchedTextUnitId());
        Assertions.assertEquals(0L, singleTranslation.getMatchedTextUnitVariantId());
        Assertions.assertEquals(targetBcp47Tags, singleTranslation.getBcp47Tag());
    }

    @Test
    void testGetSingleNoLeveragingMatchMachineTranslateFallback() {
        when(leveragerByContentAndRepositoryMock.getLeveragingMatches(any(), any(), any()))
                .thenReturn(ImmutableList.of());

        String sourceText = "unique source text";
        String targetBcp47Tags = "no-no";
        TranslationDTO singleTranslation = machineTranslationService.getSingleTranslation(
                sourceText,
                "en",
                targetBcp47Tags,
                false,
                false,
                null,
                null);

        verify(leveragerByContentAndRepositoryMock, atLeastOnce()).getLeveragingMatches(any(), any(), any());
        Assertions.assertEquals(sourceText, singleTranslation.getText());
        Assertions.assertEquals(TranslationSource.NOOP, singleTranslation.getTranslationSource());
        Assertions.assertEquals(0L, singleTranslation.getMatchedTextUnitId());
        Assertions.assertEquals(0L, singleTranslation.getMatchedTextUnitVariantId());
        Assertions.assertEquals(targetBcp47Tags, singleTranslation.getBcp47Tag());
    }
}