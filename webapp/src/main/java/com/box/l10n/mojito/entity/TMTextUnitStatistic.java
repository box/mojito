package com.box.l10n.mojito.entity;

import java.time.ZonedDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.hibernate.annotations.BatchSize;

/** @author garion */
@Entity
@Table(
    name = "tm_text_unit_statistic",
    indexes = {
      @Index(
          name = "UK__TM_TEXT_UNIT_STATISTIC__BRANCH_ID",
          columnList = "tm_text_unit_id",
          unique = true),
    })
@BatchSize(size = 1000)
public class TMTextUnitStatistic extends AuditableEntity {

  @OneToOne(optional = false)
  @JoinColumn(
      name = "tm_text_unit_id",
      foreignKey = @ForeignKey(name = "FK__TM_TEXT_UNIT_STATISTIC__BRANCH_ID"))
  private TMTextUnit tmTextUnit;

  @Column(name = "last_day_usage_count")
  private Double lastDayUsageCount = 0d;

  @Column(name = "last_period_usage_count")
  private Double lastPeriodUsageCount = 0d;

  @Column(name = "last_seen_date")
  private ZonedDateTime lastSeenDate;

  public TMTextUnit getTMTextUnit() {
    return tmTextUnit;
  }

  public void setTMTextUnit(TMTextUnit tmTextUnit) {
    this.tmTextUnit = tmTextUnit;
  }

  public Double getLastDayUsageCount() {
    return lastDayUsageCount;
  }

  public void setLastDayUsageCount(Double lastDayUsageCount) {
    this.lastDayUsageCount = lastDayUsageCount;
  }

  public Double getLastPeriodUsageCount() {
    return lastPeriodUsageCount;
  }

  public void setLastPeriodUsageCount(Double lastPeriodUsageCount) {
    this.lastPeriodUsageCount = lastPeriodUsageCount;
  }

  public ZonedDateTime getLastSeenDate() {
    return lastSeenDate;
  }

  public void setLastSeenDate(ZonedDateTime lastSeenDate) {
    this.lastSeenDate = lastSeenDate;
  }
}
