package com.box.l10n.mojito.quartz;

public class QuartzQueueException extends RuntimeException {

  public QuartzQueueException(String message) {
    super(message);
  }

  public QuartzQueueException(String message, Throwable cause) {
    super(message, cause);
  }
}
