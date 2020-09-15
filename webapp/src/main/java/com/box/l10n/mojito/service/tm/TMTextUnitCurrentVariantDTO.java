package com.box.l10n.mojito.service.tm;

public class TMTextUnitCurrentVariantDTO {
    Long tmTextUnitId;
    Long tmTextUnitVariantId;

    public TMTextUnitCurrentVariantDTO(Long tmTextUnitId, Long tmTextUnitVariantId) {
        this.tmTextUnitId = tmTextUnitId;
        this.tmTextUnitVariantId = tmTextUnitVariantId;
    }

    public Long getTmTextUnitId() {
        return tmTextUnitId;
    }

    public void setTmTextUnitId(Long tmTextUnitId) {
        this.tmTextUnitId = tmTextUnitId;
    }

    public Long getTmTextUnitVariantId() {
        return tmTextUnitVariantId;
    }

    public void setTmTextUnitVariantId(Long tmTextUnitVariantId) {
        this.tmTextUnitVariantId = tmTextUnitVariantId;
    }
}
