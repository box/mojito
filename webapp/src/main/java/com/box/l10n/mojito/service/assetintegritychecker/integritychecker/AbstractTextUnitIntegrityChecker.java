package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import java.util.Map;

/**
 * Base class that contains the logic to process non-localizable parts of the string such as
 * placeholders.
 *
 * @author jyi
 */
public abstract class AbstractTextUnitIntegrityChecker implements TextUnitIntegrityChecker {

  /**
   * All non-localizable parts from the given string are extracted and replaced with identifiers.
   * {@link LocalizableString#nonLocalizableParts} is updated to have the map of identifiers and the
   * actual non-localizable parts of the string.
   *
   * @param string
   * @return {@link LocalizableString}
   */
  @Override
  public LocalizableString extractNonLocalizableParts(String string) {
    return new LocalizableString(string);
  }

  /**
   * Returns the string with non-localizable parts restored.
   *
   * @param localizableString
   * @return the actual string with non-localizable parts restored
   */
  @Override
  public String restoreNonLocalizableParts(LocalizableString localizableString) {
    Map<String, String> nonLocalizableParts = localizableString.getNonLocalizableParts();
    String restore = localizableString.getLocalizableString();
    for (String replacement : nonLocalizableParts.keySet()) {
      restore = restore.replace(replacement, nonLocalizableParts.get(replacement));
    }
    return restore;
  }
}
