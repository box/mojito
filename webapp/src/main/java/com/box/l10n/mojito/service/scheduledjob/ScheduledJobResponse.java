package com.box.l10n.mojito.service.scheduledjob;

import com.fasterxml.jackson.annotation.JsonValue;

public class ScheduledJobResponse {
  private Status status;
  private String message;

  public ScheduledJobResponse(Status status, String message) {
    this.status = status;
    this.message = message;
  }

  public enum Status {
    FAILURE,
    SUCCESS;

    @JsonValue
    public String toLowerCase() {
      return this.name().toLowerCase();
    }
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
