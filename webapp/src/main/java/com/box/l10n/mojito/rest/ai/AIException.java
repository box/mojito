package com.box.l10n.mojito.rest.ai;

public class AIException extends RuntimeException {

  public AIException(String message) {
    super(message);
  }

  public AIException(String message, Exception e) {
    super(message, e);
  }
}
