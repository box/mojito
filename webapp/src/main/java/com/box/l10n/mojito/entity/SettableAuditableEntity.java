package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonView;
import java.time.ZonedDateTime;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;

/**
 * Similar to {@link AuditableEntity} but allows to override the attributes.
 *
 * <p>Spring doesn't allow to set dates manually via the setter with the annotations and the
 * listener
 */
@MappedSuperclass
public abstract class SettableAuditableEntity extends BaseEntity {

  @Column(name = "created_date")
  @JsonView(View.IdAndNameAndCreated.class)
  protected ZonedDateTime createdDate;

  public ZonedDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(ZonedDateTime createdDate) {
    this.createdDate = createdDate;
  }

  @PrePersist
  public void onPrePersist() {
    if (createdDate == null) {
      createdDate = JSR310Migration.newDateTimeEmptyCtor();
    }
  }
}
