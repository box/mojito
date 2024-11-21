package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.time.ZonedDateTime;

/**
 * Similar to {@link AuditableEntity} but allows to override the attributes.
 *
 * <p>Spring doesn't allow to set dates manually via the setter with the annotations and the
 * listener
 */
@MappedSuperclass
public abstract class SettableAuditableEntity extends BaseEntity {

  @Schema(type = "integer", format = "int64", example = "1715699917000")
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
