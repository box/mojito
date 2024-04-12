package com.box.l10n.mojito.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import org.hibernate.annotations.BatchSize;

/**
 * @author garion
 */
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
@NamedEntityGraph(
    name = "TMTextUnitStatistic.legacy",
    attributeNodes = {
      @NamedAttributeNode(value = "tmTextUnit", subgraph = "TMTextUnitStatistic.legacy.tmTextUnit")
    },
    subgraphs = {
      @NamedSubgraph(
          name = "TMTextUnitStatistic.legacy.tmTextUnit",
          attributeNodes = {
            @NamedAttributeNode(
                value = "asset",
                subgraph = "TMTextUnitStatistic.legacy.tmTextUnit.asset")
          }),
      @NamedSubgraph(
          name = "TMTextUnitStatistic.legacy.tmTextUnit.asset",
          attributeNodes = {@NamedAttributeNode(value = "repository")})
    })
public class TMTextUnitStatistic extends AuditableEntity {

  @OneToOne(fetch = FetchType.LAZY, optional = false)
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
