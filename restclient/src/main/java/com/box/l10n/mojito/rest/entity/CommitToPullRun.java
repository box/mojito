package com.box.l10n.mojito.rest.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.joda.time.DateTime;

/**
 * Entity that describes a Commit entry. This entity mirrors:
 * com.box.l10n.mojito.entity.CommitToPullRun
 *
 * @author garion
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommitToPullRun {
  protected Long id;
  protected DateTime createdDate;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public DateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(DateTime createdDate) {
    this.createdDate = createdDate;
  }
}
