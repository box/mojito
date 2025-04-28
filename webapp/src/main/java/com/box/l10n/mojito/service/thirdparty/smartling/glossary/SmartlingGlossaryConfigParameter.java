package com.box.l10n.mojito.service.thirdparty.smartling.glossary;

public enum SmartlingGlossaryConfigParameter {
  VARIATIONS("Variations"),
  CASE_SENSITIVE("Case Sensitive"),
  EXACT_MATCH("Exact Match"),
  DO_NOT_TRANSLATE("Do Not Translate");

  private String name;

  SmartlingGlossaryConfigParameter(String name) {
    this.name = name;
  }

  public String toString() {
    return name;
  }
}
