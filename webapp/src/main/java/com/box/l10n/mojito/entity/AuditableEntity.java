package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.annotations.VisibleForTesting;
import java.io.Serializable;
import java.time.ZonedDateTime;
import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * This class adds support for entity listener. It will track the creation date and modification
 * date of the entity extending this class.
 *
 * @author aloison
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity extends BaseEntity implements Serializable {

  @CreatedDate
  @Column(name = "created_date")
  @JsonView(View.IdAndNameAndCreated.class)
  protected ZonedDateTime createdDate;

  @LastModifiedDate
  @Column(name = "last_modified_date")
  @JsonView(View.Modified.class)
  protected ZonedDateTime lastModifiedDate;

  public ZonedDateTime getCreatedDate() {
    return createdDate;
  }

  public ZonedDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  @VisibleForTesting
  public void setCreatedDate(ZonedDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public void setLastModifiedDate(ZonedDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }
}
