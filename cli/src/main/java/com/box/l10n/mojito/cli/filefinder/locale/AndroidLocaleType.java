package com.box.l10n.mojito.cli.filefinder.locale;

import com.google.common.base.Strings;
import java.util.Locale;

/**
 * @author jaurambault
 */
public class AndroidLocaleType extends LocaleType {

  @Override
  public String getTargetLocaleRegex() {
    return ".*?";
  }

  @Override
  public String getTargetLocaleRepresentation(String targetLocale) {
    Locale forLanguageTag = Locale.forLanguageTag(targetLocale);

    String androidLocale = forLanguageTag.getLanguage();

    if (!Strings.isNullOrEmpty(forLanguageTag.getCountry())) {
      androidLocale += "-r" + forLanguageTag.getCountry();
    }

    return androidLocale;
  }
}
