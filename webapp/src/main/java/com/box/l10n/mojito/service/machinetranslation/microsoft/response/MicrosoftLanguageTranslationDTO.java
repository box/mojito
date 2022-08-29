package com.box.l10n.mojito.service.machinetranslation.microsoft.response;

/**
 * DTO that represents an individual language translation from the Microsoft MT Engine API call.
 * Part of {@link MicrosoftTextTranslationDTO}
 *
 * @author garion
 */
public class MicrosoftLanguageTranslationDTO {
  private String text;
  private String to;

  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }
}
