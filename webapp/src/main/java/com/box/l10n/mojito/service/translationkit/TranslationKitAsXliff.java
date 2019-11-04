package com.box.l10n.mojito.service.translationkit;

import com.box.l10n.mojito.entity.TranslationKit;
import com.box.l10n.mojito.service.NormalizationUtils;

/**
 * Contains a translation kit in XLIFF format along with the corresponding
 * {@link TranslationKit#id}.
 *
s * @author jaurambault
 */
public class TranslationKitAsXliff {

    Long translationKitId;
    String content;
    boolean empty;

    public Long getTranslationKitId() {
        return translationKitId;
    }

    public void setTranslationKitId(Long translationKitId) {
        this.translationKitId = translationKitId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = NormalizationUtils.normalize(content);
    }

    public boolean isEmpty() {
        return empty;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }
}
