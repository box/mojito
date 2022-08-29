package com.box.l10n.mojito.rest.textunit;

import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import java.util.List;

public class ImportTextUnitsBatch {

  boolean integrityCheckSkipped = false;
  boolean integrityCheckKeepStatusIfFailedAndSameTarget = false;
  List<TextUnitDTO> textUnits;

  public boolean isIntegrityCheckSkipped() {
    return integrityCheckSkipped;
  }

  public void setIntegrityCheckSkipped(boolean integrityCheckSkipped) {
    this.integrityCheckSkipped = integrityCheckSkipped;
  }

  public boolean isIntegrityCheckKeepStatusIfFailedAndSameTarget() {
    return integrityCheckKeepStatusIfFailedAndSameTarget;
  }

  public void setIntegrityCheckKeepStatusIfFailedAndSameTarget(
      boolean integrityCheckKeepStatusIfFailedAndSameTarget) {
    this.integrityCheckKeepStatusIfFailedAndSameTarget =
        integrityCheckKeepStatusIfFailedAndSameTarget;
  }

  public List<TextUnitDTO> getTextUnits() {
    return textUnits;
  }

  public void setTextUnits(List<TextUnitDTO> textUnits) {
    this.textUnits = textUnits;
  }
}
