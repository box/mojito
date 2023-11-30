package com.box.l10n.mojito.rest.entity;

public class LocaleInfo {

  Long localeId;

  String outputBcp47tag;

  public Long getLocaleId() {
    return localeId;
  }

  public void setLocaleId(Long localeId) {
    this.localeId = localeId;
  }

  public String getOutputBcp47tag() {
    return outputBcp47tag;
  }

  public void setOutputBcp47tag(String outputBcp47tag) {
    this.outputBcp47tag = outputBcp47tag;
  }
}
