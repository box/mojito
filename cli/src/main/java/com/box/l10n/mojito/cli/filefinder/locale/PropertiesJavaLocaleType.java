package com.box.l10n.mojito.cli.filefinder.locale;

import java.util.Locale;

/** @author jaurambault */
public class PropertiesJavaLocaleType extends LocaleType {

  @Override
  public String getTargetLocaleRegex() {
    return ".*?";
  }

  @Override
  public String getTargetLocaleRepresentation(String targetLocale) {
    Locale forLanguageTag = Locale.forLanguageTag(targetLocale);
    return forLanguageTag.toString();
  }
}
