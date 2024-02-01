package com.box.l10n.mojito.cli.filefinder.locale;

/**
 * @author jaurambault
 */
public abstract class LocaleType {

  public static final String DEFAULT_LOCALE = "en";

  String sourceLocale = DEFAULT_LOCALE;

  public abstract String getTargetLocaleRegex();

  public String getTargetLocaleRepresentation(String targetLocale) {
    return targetLocale;
  }

  public String getSourceLocale() {
    return sourceLocale;
  }

  public void setSourceLocale(String sourceLocale) {
    this.sourceLocale = sourceLocale;
  }
}
