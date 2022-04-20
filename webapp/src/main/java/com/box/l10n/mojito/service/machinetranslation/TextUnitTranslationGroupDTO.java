package com.box.l10n.mojito.service.machinetranslation;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the set of translations for a given source text.
 *
 * @author garion
 */
public class TextUnitTranslationGroupDTO {
    private String sourceText;
    private List<TranslationDTO> translations = new ArrayList<>();

    public String getSourceText() {
        return sourceText;
    }

    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }

    public List<TranslationDTO> getTranslations() {
        return translations;
    }

    public void setTranslations(List<TranslationDTO> translations) {
        this.translations = translations;
    }
}
