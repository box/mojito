package com.box.l10n.mojito.cli.command.checks;

public class CheckerOptionsMapEntry {

  private final String key;
  private final String value;

  public CheckerOptionsMapEntry(String key, String value) {
    this.key = key;
    this.value = value;
  }

  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }
}
