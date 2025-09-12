package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

/** Exception thrown when a Fluent message fails integrity validation. */
public class FluentIntegrityCheckerException extends IntegrityCheckException {

  public FluentIntegrityCheckerException(String message) {
    super(message);
  }

  public FluentIntegrityCheckerException(String message, Throwable cause) {
    super(message, cause);
  }
}
