package com.box.l10n.mojito.service.scheduledjob;

import com.fasterxml.jackson.annotation.JsonValue;
import com.box.l10n.mojito.entity.ScheduledJob;


public class ScheduledJobResponse {
  private Status status;
  private String message;
  private String jobId;
  private ScheduledJob scheduledJob;

  public ScheduledJobResponse(Status status, String message, String jobId) {
    this.status = status;
    this.message = message;
    this.jobId = jobId;
  }

  public ScheduledJobResponse(Status status, String message, ScheduledJob scheduledJob) {
    this.status = status;
    this.message = message;
    this.scheduledJob = scheduledJob;
  }

  public ScheduledJob getScheduledJob() {
    return scheduledJob;
  }

  public void setScheduledJob(ScheduledJob scheduledJob) {
    this.scheduledJob = scheduledJob;
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

  public String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }
}
