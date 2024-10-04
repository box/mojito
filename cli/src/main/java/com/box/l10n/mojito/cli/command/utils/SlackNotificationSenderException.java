package com.box.l10n.mojito.cli.command.utils;

public class SlackNotificationSenderException extends RuntimeException {
  public SlackNotificationSenderException(String message) {
    super(message);
  }

  public SlackNotificationSenderException(String message, Throwable t) {
    super(message, t);
  }
}
