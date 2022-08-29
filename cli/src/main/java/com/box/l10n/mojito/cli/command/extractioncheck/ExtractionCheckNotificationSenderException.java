package com.box.l10n.mojito.cli.command.extractioncheck;

public class ExtractionCheckNotificationSenderException extends RuntimeException {

  public ExtractionCheckNotificationSenderException(String message) {
    super(message);
  }

  public ExtractionCheckNotificationSenderException(String message, Throwable t) {
    super(message, t);
  }
}
