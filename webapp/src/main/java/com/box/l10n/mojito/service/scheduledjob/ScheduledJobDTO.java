package com.box.l10n.mojito.service.scheduledjob;

import com.box.l10n.mojito.entity.ScheduledJob;
import java.text.ParseException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import org.quartz.CronExpression;

/** Trimmed down version of the ScheduledJob entity for API responses. */
public class ScheduledJobDTO {
  private String id;
  private String repository;
  private ScheduledJobType type;
  private String cron;
  private ScheduledJobProperties properties;
  private ScheduledJobStatus status;
  private ZonedDateTime startDate;
  private ZonedDateTime endDate;
  private ZonedDateTime nextStartDate;
  private Boolean enabled;

  public ScheduledJobDTO(ScheduledJob scheduledJob) {
    this.id = scheduledJob.getUuid();
    this.repository = scheduledJob.getRepository().getName();
    this.type = scheduledJob.getJobType().getEnum();
    this.cron = scheduledJob.getCron();
    this.properties = scheduledJob.getProperties();
    this.status = scheduledJob.getJobStatus().getEnum();
    this.startDate = scheduledJob.getStartDate();
    this.endDate = scheduledJob.getEndDate();
    this.enabled = scheduledJob.isEnabled();

    // Get the next start date using the cron expression
    try {
      CronExpression cron = new CronExpression(this.cron);
      Date nextValidTime = cron.getNextValidTimeAfter(new Date());
      this.nextStartDate =
          ZonedDateTime.ofInstant(
              nextValidTime.toInstant(), ZoneId.of(ZoneId.systemDefault().getId()));
    } catch (ParseException ignored) {

    }
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getRepository() {
    return repository;
  }

  public void setRepository(String repository) {
    this.repository = repository;
  }

  public ScheduledJobType getType() {
    return type;
  }

  public void seType(ScheduledJobType type) {
    this.type = type;
  }

  public String getCron() {
    return cron;
  }

  public void setCron(String cron) {
    this.cron = cron;
  }

  public ScheduledJobProperties getProperties() {
    return properties;
  }

  public void setProperties(ScheduledJobProperties properties) {
    this.properties = properties;
  }

  public ScheduledJobStatus getStatus() {
    return status;
  }

  public void setStatus(ScheduledJobStatus status) {
    this.status = status;
  }

  public ZonedDateTime getStartDate() {
    return startDate;
  }

  public void setStartDate(ZonedDateTime startDate) {
    this.startDate = startDate;
  }

  public ZonedDateTime getEndDate() {
    return endDate;
  }

  public void setEndDate(ZonedDateTime endDate) {
    this.endDate = endDate;
  }

  public Boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public ZonedDateTime getNextStartDate() {
    return nextStartDate;
  }

  public void setNextStartDate(ZonedDateTime nextStartDate) {
    this.nextStartDate = nextStartDate;
  }

  public Boolean getEnabled() {
    return enabled;
  }
}
