package com.box.l10n.mojito.service.branch.notification.github;

public enum PRLabel {
  TRANSLATIONS_REQUIRED("translations-required"),
  TRANSLATIONS_READY("translations-ready");

  private String labelName;

  PRLabel(String labelName) {
    this.labelName = labelName;
  }

  @Override
  public String toString() {
    return labelName;
  }
}
