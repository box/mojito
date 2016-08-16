package com.box.l10n.mojito.service.translationkit;

import com.box.l10n.mojito.entity.TMTextUnitVariant;

/**
 * DTO that contains information about the current and exported (in a
 * TranslationKit) {@link TMTextUnitVariant}s for a {@link TMTextUnit}.
 *
 * See
 * {@link  TranslationKitService#getTranslationKitExportedAndCurrentTUVs(java.lang.Long)}
 *
 * @author jaurambault
 */
public class TranslationKitExportedImportedAndCurrentTUV {

    Long tmTextUnitId;
    Long exportedTmTextUnitVariant;
    Long importedTmTextUnitVariant;
    Long currentTmTextUnitVariant;

    public TranslationKitExportedImportedAndCurrentTUV(Long tmTextUnitId, Long exportedTmTextUnitVariant, Long importedTmTextUnitVariant, Long currentTmTextUnitVariant) {
        this.tmTextUnitId = tmTextUnitId;
        this.exportedTmTextUnitVariant = exportedTmTextUnitVariant;
        this.importedTmTextUnitVariant = importedTmTextUnitVariant;
        this.currentTmTextUnitVariant = currentTmTextUnitVariant;
    }

    public Long getTmTextUnitId() {
        return tmTextUnitId;
    }

    public void setTmTextUnitId(Long tmTextUnitId) {
        this.tmTextUnitId = tmTextUnitId;
    }

    public Long getExportedTmTextUnitVariant() {
        return exportedTmTextUnitVariant;
    }

    public void setExportedTmTextUnitVariant(Long exportedTmTextUnitVariant) {
        this.exportedTmTextUnitVariant = exportedTmTextUnitVariant;
    }

    public Long getCurrentTmTextUnitVariant() {
        return currentTmTextUnitVariant;
    }

    public void setCurrentTmTextUnitVariant(Long currentTmTextUnitVariant) {
        this.currentTmTextUnitVariant = currentTmTextUnitVariant;
    }

    public Long getImportedTmTextUnitVariant() {
        return importedTmTextUnitVariant;
    }

    public void setImportedTmTextUnitVariant(Long importedTmTextUnitVariant) {
        this.importedTmTextUnitVariant = importedTmTextUnitVariant;
    }

}
