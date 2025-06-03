package com.box.l10n.mojito.sarif.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ResultLevel {
  ERROR("error"),
  WARNING("warning"),
  NOTE("note");

  private final String value;

  ResultLevel(String sarifValue) {
    this.value = sarifValue.toLowerCase();
  }

  @JsonValue
  public String getValue() {
    return value.toLowerCase();
  }

  @Override
  public String toString() {
    return value.toLowerCase();
  }

  public static ResultLevel fromString(String value) {
    for (ResultLevel level : ResultLevel.values()) {
      if (level.value.equalsIgnoreCase(value)) {
        return level;
      }
    }
    throw new IllegalArgumentException("Unknown SARIF result level: " + value);
  }
}
