package com.box.l10n.mojito.service.machinetranslation;

/**
 * List of regex patterns to identify common placeholder patterns. Note: the order is important as
 * earlier defined patterns will take precedence over latter-defined ones.
 *
 * @author garion
 */
public enum PlaceholderPatternType {
  GETTEXT_STRING("%s"),
  IOS_STRING("%@"),
  GETTEXT_NUMBER("%,d"),
  MESSAGE_FORMAT_EMPTY("\\{\\}"),
  MESSAGE_FORMAT_DOUBLE_EMPTY("\\{\\{\\}\\}"),
  GETTEXT_POSITIONAL_NUMBER("%(\\d*?)d"),
  GETTEXT_NAMED("([$%])\\(+(.*?)\\)+(s|d)"),
  MESSAGE_FORMAT_NAMED("\\{\\{?[A-Za-z0-9_ .\\[\\]]+?\\}\\}?"),
  IOS_POSITIONAL("%(\\d+)\\$(@|i|s)"),
  IOS_FLOAT("%\\.(\\d*)f"),
  IOS_DOUBLE("%(\\d+)\\$,?d");

  private final String regexValue;

  PlaceholderPatternType(String regexPattern) {
    this.regexValue = regexPattern;
  }

  public String getValue() {
    return regexValue;
  }
}
