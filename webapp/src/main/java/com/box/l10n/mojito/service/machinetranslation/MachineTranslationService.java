package com.box.l10n.mojito.service.machinetranslation;

import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.service.leveraging.LeveragerByContent;
import com.box.l10n.mojito.service.leveraging.LeveragerByContentAndRepository;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.micrometer.core.annotation.Timed;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service to apply leveraging and machine translation on a set of provided strings.
 *
 * @author garion
 */
@Component
public class MachineTranslationService {
    static Logger logger = LoggerFactory.getLogger(MachineTranslationService.class);

    public static final String DEFAULT_LOCALE = "en";

    final MachineTranslationEngine machineTranslationEngine;

    final TranslationMerger translationMerger;

    public MachineTranslationService(
            MachineTranslationEngine machineTranslationEngine, TranslationMerger translationMerger) {
        this.machineTranslationEngine = machineTranslationEngine;
        this.translationMerger = translationMerger;
    }

    public TranslationsResponseDTO getTranslations(
            List<String> textSources,
            String sourceBcp47Tag,
            List<String> targetBcp47Tags,
            boolean skipFunctionalProtection,
            boolean skipLeveraging,
            List<Long> repositoryIds,
            List<String> repositoryNames
    ) {
        ImmutableMap<String, ImmutableList<TranslationDTO>> translationsBySourceText = ImmutableMap.of();

        // De-duplicate input strings
        List<String> untranslatedStrings = textSources.stream().distinct().collect(Collectors.toList());

        if (!skipLeveraging) {
            ImmutableMap<String, ImmutableList<TranslationDTO>> leveragedTranslationsBySourceText =
                    getLeveragedTranslationsBySourceText(textSources, targetBcp47Tags, repositoryIds, repositoryNames);
            translationsBySourceText = leveragedTranslationsBySourceText;

            // Only MT strings for which we didn't leverage translationsBySourceText for all the expected target languages.
            // Note: this would benefit from further optimization at some point to take into account partially leveraged
            // text units - though that would require additional MT API calls for certain MT engine implementations.
            if (!leveragedTranslationsBySourceText.isEmpty()) {
                untranslatedStrings = textSources.stream()
                        .filter(sourceText -> !leveragedTranslationsBySourceText.containsKey(sourceText)
                                || leveragedTranslationsBySourceText.get(sourceText).size() != targetBcp47Tags.size())
                        .collect(Collectors.toList());
            }
        }

        ImmutableMap<String, ImmutableList<TranslationDTO>> machineTranslationsBySourceText = getMachineTranslationBySourceText(
                getSourceBcp47TagOrDefault(sourceBcp47Tag),
                targetBcp47Tags,
                untranslatedStrings, skipFunctionalProtection);

        translationsBySourceText = translationMerger.getMergedTranslationsBySourceText(translationsBySourceText, machineTranslationsBySourceText);
        return getSortedTranslationsResponse(textSources, targetBcp47Tags, translationsBySourceText);
    }

    public TranslationDTO getSingleTranslation(
            String textSource,
            String sourceBcp47Tag,
            String targetBcp47Tags,
            boolean skipFunctionalProtection,
            boolean skipLeveraging,
            List<Long> repositoryIds,
            List<String> repositoryNames
    ) {
        return getTranslations(
                Collections.singletonList(textSource),
                sourceBcp47Tag,
                Collections.singletonList(targetBcp47Tags),
                skipFunctionalProtection,
                skipLeveraging,
                repositoryIds,
                repositoryNames)
                .getTextUnitTranslations().get(0)
                .getTranslations().get(0);
    }

    @Timed("MachineTranslationService.getMachineTranslatedResponse")
    ImmutableMap<String, ImmutableList<TranslationDTO>> getMachineTranslationBySourceText(String sourceBcp47Tag, List<String> targetBcp47Tags, List<String> textSources, Boolean skipFunctionalProtection) {
        if (textSources.isEmpty()) {
            return ImmutableMap.of();
        }

        // TODO(garion): Implement functional protection / placeholder processing
        if (skipFunctionalProtection != null && skipFunctionalProtection) {
            logger.debug("function / placeholder protection");
        }

        return machineTranslationEngine.getTranslationsBySourceText(
                textSources,
                sourceBcp47Tag,
                targetBcp47Tags,
                null,
                null);
    }

    public TranslationSource getConfiguredEngineSource() {
        return machineTranslationEngine.getSource();
    }

    /**
     * Leverages translations by source text from Mojito.
     * Note: it relies on the localeTag on being an exact match.
     */
    @Timed("MachineTranslationService.getLeveragedTranslationResponse")
    private ImmutableMap<String, ImmutableList<TranslationDTO>> getLeveragedTranslationsBySourceText(
            List<String> textSources,
            List<String> targetBcp47Tags,
            List<Long> repositoryIds,
            List<String> repositoryNames) {

        return textSources.stream()
                .map((sourceText) -> {
                    // Note: to further increase leverage performance at a later date, we should introduce a
                    // bulk leveraging function that will take in a list of source strings, a list of target
                    // locales and will return one single result for each source string and locale combination.
                    Map<String, TextUnitDTO> mostRecentTranslationsByLanguage =
                            getMostRecentTranslationsByLanguage(repositoryIds, repositoryNames, sourceText);

                    // Note: the leveraging mode is locale code specific. If the translation requests ask for "pt" but
                    // we only have a "pt-BR" translation, or the other way around, we do not use the leveraged match.
                    ImmutableList<TranslationDTO> translations = targetBcp47Tags.stream()
                            .filter(mostRecentTranslationsByLanguage::containsKey)
                            .map(targetBcp47Tag -> {
                                TextUnitDTO matchedLanguageTextUnit = mostRecentTranslationsByLanguage.get(targetBcp47Tag);

                                TranslationDTO translation = new TranslationDTO();
                                translation.setText(matchedLanguageTextUnit.getTarget());
                                translation.setBcp47Tag(targetBcp47Tag);
                                translation.setMatchedTextUnitId(matchedLanguageTextUnit.getTmTextUnitId());
                                translation.setMatchedTextUnitVariantId(matchedLanguageTextUnit.getTmTextUnitVariantId());
                                translation.setTranslationSource(TranslationSource.MOJITO_TM_LEVERAGE);
                                return translation;
                            })
                            .collect(ImmutableList.toImmutableList());

                    return new AbstractMap.SimpleEntry<>(sourceText, translations);
                })
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private ImmutableMap<String, TextUnitDTO> getMostRecentTranslationsByLanguage(List<Long> repositoryIds, List<String> repositoryNames, String sourceText) {
        List<TextUnitDTO> textUnitDTOsForLeveraging = getLeveragedTextUnitDTOS(sourceText, repositoryIds, repositoryNames);

        // Pick the most recent created translations for each language
        return textUnitDTOsForLeveraging.stream()
                .collect(ImmutableMap.toImmutableMap(
                        TextUnitDTO::getTargetLocale,
                        Function.identity(),
                        BinaryOperator.maxBy(Comparator.comparing(TextUnitDTO::getTmTextUnitCreatedDate))));
    }

    List<TextUnitDTO> getLeveragedTextUnitDTOS(
            String sourceText, List<Long> repositoryIds, List<String> repositoryNames) {
        TMTextUnit searchTmTextUnit = new TMTextUnit();
        searchTmTextUnit.setContent(sourceText);

        LeveragerByContentAndRepository leveragerByContentAndRepository =
                getLeveragerByContentAndRepository(repositoryIds, repositoryNames);

        return leveragerByContentAndRepository.getLeveragingMatches(
                searchTmTextUnit, null, null);
    }

    LeveragerByContentAndRepository getLeveragerByContentAndRepository(List<Long> repositoryIds, List<String> repositoryNames) {
        return new LeveragerByContentAndRepository(repositoryIds, repositoryNames);
    }

    private String getSourceBcp47TagOrDefault(String sourceBcp47Tag) {
        if (Strings.isBlank(sourceBcp47Tag)) {
            sourceBcp47Tag = DEFAULT_LOCALE;
        }

        return sourceBcp47Tag;
    }

    /**
     * Returns a translation response with the data sorted according to the order in the provided
     * text sources and target Bcp47 tags.
     */
    private TranslationsResponseDTO getSortedTranslationsResponse(List<String> textSources, List<String> targetBcp47Tags, ImmutableMap<String, ImmutableList<TranslationDTO>> translationsBySourceText) {
        ImmutableList<TextUnitTranslationGroupDTO> textUnitTranslationGroups = textSources.stream()
                .map(sourceText -> {
                    ImmutableMap<String, TranslationDTO> unorderedTranslationsByBcp47Tag =
                            getTranslationsByBcp47Tag(translationsBySourceText, sourceText);

                    ImmutableList<TranslationDTO> sortedTranslations = targetBcp47Tags.stream()
                            .map(targetBcp47Tag -> getTranslationOrDefaultUntranslated(sourceText, unorderedTranslationsByBcp47Tag, targetBcp47Tag))
                            .collect(ImmutableList.toImmutableList());

                    TextUnitTranslationGroupDTO textUnitTranslationGroup = new TextUnitTranslationGroupDTO();
                    textUnitTranslationGroup.setSourceText(sourceText);
                    textUnitTranslationGroup.setTranslations(sortedTranslations);

                    return textUnitTranslationGroup;
                })
                .collect(ImmutableList.toImmutableList());

        TranslationsResponseDTO translationsResponse = new TranslationsResponseDTO();
        translationsResponse.setTextUnitTranslations(textUnitTranslationGroups);
        return translationsResponse;
    }

    private TranslationDTO getTranslationOrDefaultUntranslated(String sourceText, ImmutableMap<String, TranslationDTO> translationsByBcp47Tag, String targetBcp47Tag) {
        if (translationsByBcp47Tag.containsKey(targetBcp47Tag))
            return translationsByBcp47Tag.get(targetBcp47Tag);
        else {
            TranslationDTO translation = new TranslationDTO();
            translation.setText(sourceText);
            translation.setBcp47Tag(targetBcp47Tag);
            translation.setTranslationSource(TranslationSource.UNTRANSLATED);
            return translation;
        }
    }

    private ImmutableMap<String, TranslationDTO> getTranslationsByBcp47Tag(ImmutableMap<String, ImmutableList<TranslationDTO>> translationsBySourceText, String sourceText) {
        return ImmutableMap.copyOf(translationsBySourceText.get(sourceText)
                .stream()
                .collect(Collectors.toMap(TranslationDTO::getBcp47Tag, Function.identity())));
    }
}
