package com.box.l10n.mojito.service.scheduledjob;

public class ScheduledJobException extends RuntimeException {
  public ScheduledJobException(String message) {
    super(message);
  }

  public ScheduledJobException(String message, Exception e) {
    super(message, e);
  }

  public ScheduledJobException(String message, Throwable t) {
    super(message, t);
  }
}
