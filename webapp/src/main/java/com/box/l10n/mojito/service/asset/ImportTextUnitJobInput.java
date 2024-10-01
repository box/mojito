package com.box.l10n.mojito.service.asset;

import com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import java.util.List;

/**
 * @author jaurambault
 */
public class ImportTextUnitJobInput {

  List<TextUnitDTO> textUnitDTOs;
  TextUnitBatchImporterService.IntegrityChecksType integrityChecksType;

  public TextUnitBatchImporterService.IntegrityChecksType getIntegrityChecksType() {
    return integrityChecksType;
  }

  public void setIntegrityChecksType(
      TextUnitBatchImporterService.IntegrityChecksType integrityChecksType) {
    this.integrityChecksType = integrityChecksType;
  }

  public List<TextUnitDTO> getTextUnitDTOs() {
    return textUnitDTOs;
  }

  public void setTextUnitDTOs(List<TextUnitDTO> textUnitDTOs) {
    this.textUnitDTOs = textUnitDTOs;
  }
}
