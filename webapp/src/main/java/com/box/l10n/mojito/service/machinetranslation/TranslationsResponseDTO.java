package com.box.l10n.mojito.service.machinetranslation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class contains the full response for the Machine Translation web service calls that retrieve
 * multiple translations.
 *
 * @author garion
 */
public class TranslationsResponseDTO implements Serializable {
  private List<TextUnitTranslationGroupDTO> textUnitTranslationGroups = new ArrayList<>();

  public List<TextUnitTranslationGroupDTO> getTextUnitTranslations() {
    return textUnitTranslationGroups;
  }

  public void setTextUnitTranslations(List<TextUnitTranslationGroupDTO> textUnitTranslationGroups) {
    this.textUnitTranslationGroups = textUnitTranslationGroups;
  }
}
