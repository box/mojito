package com.box.l10n.mojito.service.delta.dtos;

import com.box.l10n.mojito.service.delta.DeltaType;

/**
 * Information about a single translation delivered as a delta.
 *
 * @author garion
 */
public class DeltaTranslationDTO {
    String text;

    DeltaType deltaType = DeltaType.UNKNOWN;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public DeltaType getDeltaType() {
        return deltaType;
    }

    public void setDeltaType(DeltaType deltaType) {
        this.deltaType = deltaType;
    }
}
