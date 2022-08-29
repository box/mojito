package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import java.util.Map;
import java.util.TreeMap;

/**
 * This class is used to store a string with non-localizable parts extracted and stored in the map.
 *
 * @author jyi
 */
public class LocalizableString {

  String originalString;
  String localizableString;
  Map<String, String> nonLocalizableParts;

  public LocalizableString(String string) {
    this.originalString = string;
    this.localizableString = null;
    this.nonLocalizableParts = new TreeMap<>();
  }

  public String getOriginalString() {
    return originalString;
  }

  public String getLocalizableString() {
    return localizableString;
  }

  public void setLocalizableString(String localizableString) {
    this.localizableString = localizableString;
  }

  public Map<String, String> getNonLocalizableParts() {
    return nonLocalizableParts;
  }

  public void setNonLocalizableParts(Map<String, String> nonLocalizableParts) {
    this.nonLocalizableParts = nonLocalizableParts;
  }
}
