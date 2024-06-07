package com.box.l10n.mojito.cli.command.extraction;

public class ExtractionDiffNotificationSenderException extends RuntimeException {
  public ExtractionDiffNotificationSenderException(String message) {
    super(message);
  }

  public ExtractionDiffNotificationSenderException(String message, Throwable t) {
    super(message, t);
  }
}
