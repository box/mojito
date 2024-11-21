package com.box.l10n.mojito.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;

@Entity
@Table(
    name = "pagerduty_incidents",
    indexes = {
      @Index(
          name = "I__PAGERDUTY_INCIDENTS__CLIENT_NAME__DEDUP_KEY__RESOLVED_AT",
          columnList = "client_name, dedup_key, resolved_at")
    })
public class PagerDutyIncident extends BaseEntity {
  @Column(name = "client_name")
  String clientName;

  @Column(name = "dedup_key")
  String dedupKey;

  @Column(name = "triggered_at")
  private ZonedDateTime triggeredAt;

  @Column(name = "resolved_at")
  private ZonedDateTime resolvedAt;

  public String getClientName() {
    return clientName;
  }

  public void setClientName(String clientName) {
    this.clientName = clientName;
  }

  public String getDedupKey() {
    return dedupKey;
  }

  public void setDedupKey(String dedupKey) {
    this.dedupKey = dedupKey;
  }

  public ZonedDateTime getTriggeredAt() {
    return triggeredAt;
  }

  public void setTriggeredAt(ZonedDateTime triggeredAt) {
    this.triggeredAt = triggeredAt;
  }

  public ZonedDateTime getResolvedAt() {
    return resolvedAt;
  }

  public void setResolvedAt(ZonedDateTime resolvedAt) {
    this.resolvedAt = resolvedAt;
  }
}
