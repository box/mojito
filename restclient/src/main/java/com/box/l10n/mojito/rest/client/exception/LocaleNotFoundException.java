package com.box.l10n.mojito.rest.client.exception;

/**
 * @author aloison
 */
public class LocaleNotFoundException extends ResourceNotFoundException {
  public LocaleNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public LocaleNotFoundException(String message) {
    super(message);
  }
}
