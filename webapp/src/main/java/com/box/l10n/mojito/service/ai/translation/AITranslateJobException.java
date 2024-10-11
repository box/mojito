package com.box.l10n.mojito.service.ai.translation;

public class AITranslateJobException extends RuntimeException {

  public AITranslateJobException(String message) {
    super(message);
  }

  public AITranslateJobException(String message, Throwable t) {
    super(message, t);
  }
}
