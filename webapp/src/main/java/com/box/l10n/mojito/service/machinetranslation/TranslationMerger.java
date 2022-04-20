package com.box.l10n.mojito.service.machinetranslation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.springframework.stereotype.Component;

import java.util.AbstractMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class enables merging of translations from different sources (leveraging, MT, etc.)
 * according to the respective sources priorities.
 *
 * @author garion
 */
@Component
public class TranslationMerger {

    /**
     * Merges two sets of translations into a new object. Whenever multiple translations are found
     * for the same target bcp47tag, only one translation will be picked based on the priority
     * of the source of the translation.
     */
    public ImmutableMap<String, ImmutableList<TranslationDTO>> getMergedTranslationsBySourceText(ImmutableMap<String, ImmutableList<TranslationDTO>> translationsBySourceTextA, ImmutableMap<String, ImmutableList<TranslationDTO>> translationsBySourceTextB) {
        ImmutableList<String> uniqueTextSources = getUniqueKeys(translationsBySourceTextA, translationsBySourceTextB);

        return uniqueTextSources.stream()
                .map(sourceText -> {
                    ImmutableMap<String, TranslationDTO> translationsByBcp47TagA =
                            getTranslationsByBcp47Tag(translationsBySourceTextA, sourceText);
                    ImmutableMap<String, TranslationDTO> translationsByBcp47TagB =
                            getTranslationsByBcp47Tag(translationsBySourceTextB, sourceText);

                    ImmutableList<String> uniqueBcp57Tags = getUniqueKeys(translationsByBcp47TagA, translationsByBcp47TagB);

                    ImmutableList<TranslationDTO> translations = uniqueBcp57Tags.stream()
                            .map(bcp47Tag -> getPriorityTranslation(
                                    translationsByBcp47TagA.get(bcp47Tag),
                                    translationsByBcp47TagB.get(bcp47Tag)))
                            .collect(ImmutableList.toImmutableList());

                    return new AbstractMap.SimpleEntry<>(sourceText, translations);
                })
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Compares two {@link TranslationDTO} objects and returns the one with the highest priority,
     * where the lower the number defined in {@link TranslationSource} is the one
     * that will be prioritized.
     *
     * If either of the two translation objects is null, the non-null one will be returned.
     */
    public TranslationDTO getPriorityTranslation(TranslationDTO translationA, TranslationDTO translationB) {
        if (translationA == null || translationB == null) {
            if (translationA != null) {
                return translationA;
            } else {
                return translationB;
            }
        }

        if (translationA.getTranslationSource().getPriority() < translationB.getTranslationSource().getPriority()) {
            return translationA;
        } else {
            return translationB;
        }
    }

    private ImmutableList<String> getUniqueKeys(
            Map<String, ?> map1,
            Map<String, ?> map2) {
        return Stream.concat(map1.keySet().stream(), map2.keySet().stream())
                .distinct()
                .collect(ImmutableList.toImmutableList());
    }

    private static ImmutableMap<String, TranslationDTO> getTranslationsByBcp47Tag(
            ImmutableMap<String, ImmutableList<TranslationDTO>> translationsBySourceText,
            String textSource) {
        ImmutableList<TranslationDTO> translations = translationsBySourceText.get(textSource);

        if (translations == null) {
            return ImmutableMap.of();
        }

        return ImmutableMap.copyOf(
                translations.stream()
                        .collect(Collectors.toMap(TranslationDTO::getBcp47Tag, Function.identity())));
    }
}
