package com.box.l10n.mojito.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;

/**
 * Entity to keep track of which repository's statistics are outdated.
 *
 * @author jyi
 */
@Entity
@Table(
    name = "statistics_schedule",
    indexes = {
      @Index(
          name = "I__STATISTICS_SCHEDULE__REPOSITORY_ID",
          columnList = "repository_id",
          unique = false)
    })
public class StatisticsSchedule extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "repository_id",
      foreignKey = @ForeignKey(name = "FK__STATISTICS_SCHEDULE__REPOSITORY_ID"))
  private Repository repository;

  @Column(name = "time_to_update")
  protected ZonedDateTime timeToUpdate;

  public Repository getRepository() {
    return repository;
  }

  public void setRepository(Repository repository) {
    this.repository = repository;
  }

  public ZonedDateTime getTimeToUpdate() {
    return timeToUpdate;
  }

  public void setTimeToUpdate(ZonedDateTime timeToUpdate) {
    this.timeToUpdate = timeToUpdate;
  }
}
