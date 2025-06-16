package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobException;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobProperties;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.text.ParseException;
import java.time.ZonedDateTime;
import org.hibernate.envers.Audited;
import org.quartz.CronExpression;

@Audited
@Entity
@Table(name = "scheduled_job")
public class ScheduledJob extends BaseEntity {
  @Basic(optional = false)
  @Column(name = "uuid")
  private String uuid;

  @ManyToOne
  @JoinColumn(
      name = "repository_id",
      foreignKey = @ForeignKey(name = "FK__SCHEDULED_JOB__IMPORT_REPOSITORY__ID"))
  private Repository repository;

  @ManyToOne
  @JoinColumn(name = "job_type", foreignKey = @ForeignKey(name = "FK__JOB_TYPE__JOB_TYPE_ID"))
  private ScheduledJobType jobType;

  @Column(name = "cron")
  private String cron;

  @Transient private ScheduledJobProperties properties;

  @Basic(optional = false)
  @Column(name = "properties")
  private String propertiesString;

  @ManyToOne
  @JoinColumn(name = "job_status", foreignKey = @ForeignKey(name = "FK__JOB_STATUS__JOB_STATUS_ID"))
  private ScheduledJobStatus jobStatus;

  @Column(name = "start_date")
  private ZonedDateTime startDate;

  @Column(name = "end_date")
  private ZonedDateTime endDate;

  @Basic(optional = false)
  @Column(name = "enabled")
  private Boolean enabled = true;

  @PostLoad
  public void deserializeProperties() {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      this.properties =
          objectMapper.readValue(propertiesString, jobType.getEnum().getPropertiesClass());
    } catch (Exception e) {
      throw new ScheduledJobException(
          "Failed to deserialize properties '"
              + propertiesString
              + "' for class: "
              + jobType.getEnum().getPropertiesClass().getName(),
          e);
    }
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public Repository getRepository() {
    return repository;
  }

  public void setRepository(Repository repository) {
    this.repository = repository;
  }

  public ScheduledJobType getJobType() {
    return jobType;
  }

  public void setJobType(ScheduledJobType jobType) {
    this.jobType = jobType;
  }

  public String getCron() {
    return cron;
  }

  public void setCron(String cron) {
    if (cron == null || cron.isBlank()) {
      throw new ScheduledJobException("Cron expression cannot be null or blank");
    }
    try {
      new CronExpression(cron);
    } catch (ParseException e) {
      throw new ScheduledJobException("Invalid cron expression: " + cron, e);
    }
    this.cron = cron;
  }

  public ScheduledJobProperties getProperties() {
    return properties;
  }

  public void setProperties(ScheduledJobProperties properties) {
    this.properties = properties;

    ObjectMapper objectMapper = new ObjectMapper();
    try {
      this.propertiesString = objectMapper.writeValueAsString(this.properties);
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize properties", e);
    }
  }

  public String getPropertiesString() {
    return propertiesString;
  }

  public void setPropertiesString(String properties) {
    this.propertiesString = properties;
  }

  public ScheduledJobStatus getJobStatus() {
    return jobStatus;
  }

  public void setJobStatus(ScheduledJobStatus jobStatus) {
    this.jobStatus = jobStatus;
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
}
