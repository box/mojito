package com.box.l10n.mojito.service.asset;

import com.box.l10n.mojito.service.tm.search.TextUnitDTO;

import java.util.List;

/**
 * @author jaurambault
 */
public class ImportTextUnitJobInput {

    boolean integrityCheckSkipped;
    boolean integrityCheckKeepStatusIfFailedAndSameTarget;
    List<TextUnitDTO> textUnitDTOs;

    public boolean isIntegrityCheckSkipped() {
        return integrityCheckSkipped;
    }

    public void setIntegrityCheckSkipped(boolean integrityCheckSkipped) {
        this.integrityCheckSkipped = integrityCheckSkipped;
    }

    public boolean isIntegrityCheckKeepStatusIfFailedAndSameTarget() {
        return integrityCheckKeepStatusIfFailedAndSameTarget;
    }

    public void setIntegrityCheckKeepStatusIfFailedAndSameTarget(boolean integrityCheckKeepStatusIfFailedAndSameTarget) {
        this.integrityCheckKeepStatusIfFailedAndSameTarget = integrityCheckKeepStatusIfFailedAndSameTarget;
    }

    public List<TextUnitDTO> getTextUnitDTOs() {
        return textUnitDTOs;
    }

    public void setTextUnitDTOs(List<TextUnitDTO> textUnitDTOs) {
        this.textUnitDTOs = textUnitDTOs;
    }
}
