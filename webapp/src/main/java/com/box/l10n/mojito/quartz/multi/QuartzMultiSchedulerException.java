package com.box.l10n.mojito.quartz.multi;

public class QuartzMultiSchedulerException extends RuntimeException {

  public QuartzMultiSchedulerException(String message) {
    super(message);
  }

  public QuartzMultiSchedulerException(String message, Throwable cause) {
    super(message, cause);
  }
}
