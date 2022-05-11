package com.box.l10n.mojito.service.machinetranslation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;

/**
 * Interface for retrieving machine translations for a set of text strings.
 *
 * @author garion
 */
public interface MachineTranslationEngine {
    TranslationSource getSource();

    ImmutableMap<String, ImmutableList<TranslationDTO>> getTranslationsBySourceText(
            List<String> textSources,
            String sourceBcp47Tag,
            List<String> targetBcp47Tags,
            TextType sourceTextType,
            String customModel,
            boolean isFunctionalProtectionEnabled);
}
