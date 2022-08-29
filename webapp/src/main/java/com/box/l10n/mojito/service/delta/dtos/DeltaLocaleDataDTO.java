package com.box.l10n.mojito.service.delta.dtos;

import java.util.Map;

/**
 * Delta information for a single locale.
 *
 * @author garion
 */
public class DeltaLocaleDataDTO {
  Map<String, DeltaTranslationDTO> translationsByTextUnitName;

  public Map<String, DeltaTranslationDTO> getTranslationsByTextUnitName() {
    return translationsByTextUnitName;
  }

  public void setTranslationsByTextUnitName(
      Map<String, DeltaTranslationDTO> translationsByTextUnitName) {
    this.translationsByTextUnitName = translationsByTextUnitName;
  }
}
