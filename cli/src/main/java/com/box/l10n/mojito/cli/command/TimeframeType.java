package com.box.l10n.mojito.cli.command;

public enum TimeframeType {
  DAYS('D'),
  WEEKS('W'),
  MONTHS('M'),
  YEARS('Y');

  private final char abbreviation;

  TimeframeType(char abbreviation) {
    this.abbreviation = abbreviation;
  }

  public char getAbbreviationInUpperCase() {
    return this.abbreviation;
  }

  public char getAbbreviationInLowerCase() {
    return Character.toLowerCase(this.abbreviation);
  }
}
