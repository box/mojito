package com.box.l10n.mojito.service.thirdparty.smartling.glossary;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class GlossaryTerm implements Serializable {

  private Long tmTextUnitId;
  private String text;
  private Map<String, String> translations;
  private boolean isExactMatch;
  private boolean isCaseSensitive;
  private boolean isDoNotTranslate;

  public GlossaryTerm() {}

  public GlossaryTerm(
      String text,
      boolean isExactMatch,
      boolean isCaseSensitive,
      boolean isDoNotTranslate,
      Long tmTextUnitId) {
    this.text = text;
    this.isExactMatch = isExactMatch;
    this.isCaseSensitive = isCaseSensitive;
    this.isDoNotTranslate = isDoNotTranslate;
    this.translations = new HashMap<>();
    this.tmTextUnitId = tmTextUnitId;
  }

  public Long getTmTextUnitId() {
    return tmTextUnitId;
  }

  public void addLocaleTranslation(String bcp47Tag, String translation) {
    translations.put(bcp47Tag, translation);
  }

  public String getLocaleTranslation(String bcp47Tag) {
    return translations.get(bcp47Tag);
  }

  public String getText() {
    return text;
  }

  public Map<String, String> getTranslations() {
    return translations;
  }

  public boolean isExactMatch() {
    return isExactMatch;
  }

  public boolean isCaseSensitive() {
    return isCaseSensitive;
  }

  public boolean isDoNotTranslate() {
    return isDoNotTranslate;
  }

  public void setTmTextUnitId(Long tmTextUnitId) {
    this.tmTextUnitId = tmTextUnitId;
  }

  public void setText(String text) {
    this.text = text;
  }

  public void setTranslations(Map<String, String> translations) {
    this.translations = translations;
  }

  public void setExactMatch(boolean exactMatch) {
    isExactMatch = exactMatch;
  }

  public void setCaseSensitive(boolean caseSensitive) {
    isCaseSensitive = caseSensitive;
  }

  public void setDoNotTranslate(boolean doNotTranslate) {
    isDoNotTranslate = doNotTranslate;
  }

  public String toString() {
    return "GlossaryTerm{"
        + "tmTextUnitId="
        + tmTextUnitId
        + ", text='"
        + text
        + '\''
        + ", translations="
        + translations
        + ", isExactMatch="
        + isExactMatch
        + ", isCaseSensitive="
        + isCaseSensitive
        + ", isDoNotTranslate="
        + isDoNotTranslate
        + '}';
  }
}
