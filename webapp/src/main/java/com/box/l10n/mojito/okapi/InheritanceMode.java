package com.box.l10n.mojito.okapi;

/**
 * @author jaurambault
 */
public enum InheritanceMode {

  /** If there is no translation the text unit should be removed */
  REMOVE_UNTRANSLATED,
  /** Look for translations in parent locales, if none it will fallback to the source */
  USE_PARENT
}
