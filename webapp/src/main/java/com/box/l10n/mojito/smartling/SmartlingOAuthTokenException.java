package com.box.l10n.mojito.smartling;

public class SmartlingOAuthTokenException extends RuntimeException {

  public SmartlingOAuthTokenException(String message) {
    super(message);
  }

  public SmartlingOAuthTokenException(String message, Throwable cause) {
    super(message, cause);
  }
}
