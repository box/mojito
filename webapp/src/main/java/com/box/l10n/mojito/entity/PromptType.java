package com.box.l10n.mojito.entity;

public enum PromptType {
  SOURCE_STRING_CHECKER,
  TRANSLATION;

  public String toString() {
    return name();
  }
}
