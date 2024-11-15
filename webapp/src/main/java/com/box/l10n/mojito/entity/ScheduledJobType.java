package com.box.l10n.mojito.entity;

import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;

import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "scheduled_job_type")
@Audited(targetAuditMode = NOT_AUDITED)
public class ScheduledJobType extends BaseEntity {
  @Id private Long id;

  @Basic(optional = false)
  @Column(name = "name")
  @Enumerated(EnumType.STRING)
  @JsonView(View.Repository.class)
  private com.box.l10n.mojito.service.scheduledjob.ScheduledJobType jobType;

  @Override
  public Long getId() {
    return id;
  }

  @Override
  public void setId(Long id) {
    this.id = id;
  }

  public com.box.l10n.mojito.service.scheduledjob.ScheduledJobType getEnum() {
    return jobType;
  }

  public void setEnum(com.box.l10n.mojito.service.scheduledjob.ScheduledJobType jobType) {
    this.jobType = jobType;
  }
}
