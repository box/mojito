package com.box.l10n.mojito.rest.textunit;

import java.time.ZonedDateTime;

public class ImportTextUnitStatisticsBody {
  private String name;

  private String content;

  private String comment;

  private Double lastDayEstimatedVolume;

  private Double lastPeriodEstimatedVolume;

  private ZonedDateTime lastSeenDate;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public Double getLastDayEstimatedVolume() {
    return lastDayEstimatedVolume;
  }

  public void setLastDayEstimatedVolume(Double lastDayEstimatedVolume) {
    this.lastDayEstimatedVolume = lastDayEstimatedVolume;
  }

  public Double getLastPeriodEstimatedVolume() {
    return lastPeriodEstimatedVolume;
  }

  public void setLastPeriodEstimatedVolume(Double lastPeriodEstimatedVolume) {
    this.lastPeriodEstimatedVolume = lastPeriodEstimatedVolume;
  }

  public ZonedDateTime getLastSeenDate() {
    return lastSeenDate;
  }

  public void setLastSeenDate(ZonedDateTime lastSeenDate) {
    this.lastSeenDate = lastSeenDate;
  }

  @Override
  public String toString() {
    return "ImportTextUnitStatisticsBody{"
        + "name='"
        + name
        + '\''
        + ", content='"
        + content
        + '\''
        + ", comment='"
        + comment
        + '\''
        + ", lastDayEstimatedVolume="
        + lastDayEstimatedVolume
        + ", lastPeriodEstimatedVolume="
        + lastPeriodEstimatedVolume
        + ", lastSeenDate="
        + lastSeenDate
        + '}';
  }
}
