package com.box.l10n.mojito.service.machinetranslation.microsoft.response;

import java.util.List;

/**
 * DTO that represents the response of the Microsoft MT Engine API call.
 *
 * @author garion
 */
public class MicrosoftTextTranslationDTO {
  private List<MicrosoftLanguageTranslationDTO> translations;

  public List<MicrosoftLanguageTranslationDTO> getTranslations() {
    return translations;
  }

  public void setTranslations(List<MicrosoftLanguageTranslationDTO> translations) {
    this.translations = translations;
  }
}
