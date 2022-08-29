package com.box.l10n.mojito.slack;

public class SlackClientException extends Exception {
  public SlackClientException(String message) {
    super(message);
  }

  public SlackClientException(Throwable cause) {
    super(cause);
  }
}
