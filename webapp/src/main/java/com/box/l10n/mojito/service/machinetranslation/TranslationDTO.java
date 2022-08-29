package com.box.l10n.mojito.service.machinetranslation;

import java.io.Serializable;

/**
 * Represents an individual translation for one language for a specified source string.
 *
 * @author garion
 */
public class TranslationDTO implements Serializable {
  private String text;
  private long matchedTextUnitId;
  private long matchedTextUnitVariantId;
  private String bcp47Tag;
  private TranslationSource translationSource;

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public long getMatchedTextUnitId() {
    return matchedTextUnitId;
  }

  public void setMatchedTextUnitId(long matchedTextUnitId) {
    this.matchedTextUnitId = matchedTextUnitId;
  }

  public long getMatchedTextUnitVariantId() {
    return matchedTextUnitVariantId;
  }

  public void setMatchedTextUnitVariantId(long matchedTextUnitVariantId) {
    this.matchedTextUnitVariantId = matchedTextUnitVariantId;
  }

  public String getBcp47Tag() {
    return bcp47Tag;
  }

  public void setBcp47Tag(String bcp47Tag) {
    this.bcp47Tag = bcp47Tag;
  }

  public TranslationSource getTranslationSource() {
    return translationSource;
  }

  public void setTranslationSource(TranslationSource translationSource) {
    this.translationSource = translationSource;
  }
}
