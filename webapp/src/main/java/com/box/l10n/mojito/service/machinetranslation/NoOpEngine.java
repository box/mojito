package com.box.l10n.mojito.service.machinetranslation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

/**
 * Stub implementation of {@link MachineTranslationEngine} that is used whenever a
 * proper machine translation implementation isn't configured through the
 * "l10n.mt.impl" property.
 *
 * @author garion
 */
public class NoOpEngine implements MachineTranslationEngine {
    static Logger logger = LoggerFactory.getLogger(NoOpEngine.class);

    @Override
    public TranslationSource getSource() {
        return TranslationSource.NOOP;
    }

    @Override
    public ImmutableMap<String, ImmutableList<TranslationDTO>> getTranslationsBySourceText(List<String> textSources, String sourceBcp47Tag, List<String> targetBcp47Tags, String sourceMimeType, String customModel) {
        logger.debug("NoOpEngine translate() operation called.");

        return textSources.stream()
                .map(sourceText -> {
                    ImmutableList<TranslationDTO> translations = targetBcp47Tags.stream()
                            .map(targetBcp47Tag -> {
                                TranslationDTO translation = new TranslationDTO();
                                translation.setText(sourceText);
                                translation.setBcp47Tag(targetBcp47Tag);
                                translation.setTranslationSource(getSource());
                                return translation;
                            })
                            .collect(ImmutableList.toImmutableList());

                    return new AbstractMap.SimpleEntry<>(sourceText, translations);
                })
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
