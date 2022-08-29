package com.box.l10n.mojito.service.languagedetection;

/**
 * Thrown if request language if not supported for language detection.
 *
 * @author jaurambault
 */
public class UnsupportedLanguageException extends RuntimeException {

  public UnsupportedLanguageException(String string) {
    super(string);
  }
}
