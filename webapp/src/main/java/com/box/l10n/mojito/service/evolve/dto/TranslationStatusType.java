package com.box.l10n.mojito.service.evolve.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TranslationStatusType {
  READY_FOR_TRANSLATION("Ready for translation"),
  IN_TRANSLATION("In Translation"),
  TRANSLATED("Translated");

  private final String name;

  TranslationStatusType(String name) {
    this.name = name;
  }

  @JsonValue
  public String getName() {
    return this.name;
  }

  @JsonCreator
  public static TranslationStatusType fromName(String name) {
    if (name.isBlank()) {
      return null;
    }
    for (TranslationStatusType translationStatusType : TranslationStatusType.values()) {
      if (translationStatusType.getName().equalsIgnoreCase(name)) {
        return translationStatusType;
      }
    }
    throw new IllegalArgumentException("Invalid name: " + name);
  }
}
