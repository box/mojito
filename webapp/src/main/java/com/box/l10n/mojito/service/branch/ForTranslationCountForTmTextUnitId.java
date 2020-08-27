package com.box.l10n.mojito.service.branch;

public class ForTranslationCountForTmTextUnitId {
    Long tmTextUnitId;
    long forTranslationCount;
    long totalCount;

    public ForTranslationCountForTmTextUnitId(Long tmTextUnitId, long forTranslationCount, long totalCount) {
        this.tmTextUnitId = tmTextUnitId;
        this.forTranslationCount = forTranslationCount;
        this.totalCount = totalCount;
    }

    public Long getTmTextUnitId() {
        return tmTextUnitId;
    }

    public long getForTranslationCount() {
        return forTranslationCount;
    }

    public long getTotalCount() {
        return totalCount;
    }
}
