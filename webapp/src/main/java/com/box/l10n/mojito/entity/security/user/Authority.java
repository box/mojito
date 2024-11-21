package com.box.l10n.mojito.entity.security.user;

import com.box.l10n.mojito.entity.AuditableEntity;
import com.box.l10n.mojito.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import org.springframework.data.annotation.CreatedBy;

/**
 * @author wyau
 */
@Entity
@Table(
    name = "authority",
    indexes = {
      @Index(
          name = "UK__AUTHORITIES__USER_ID__AUTHORITY",
          columnList = "user_id, authority",
          unique = true)
    })
public class Authority extends AuditableEntity implements Serializable {

  @ManyToOne(fetch = FetchType.LAZY)
  @JsonBackReference
  @Schema(hidden = true)
  @JoinColumn(
      name = "user_id",
      foreignKey = @ForeignKey(name = "FK__AUTHORITY__USER__USER_ID"),
      nullable = false)
  User user;

  @Column(name = "authority")
  String authority;

  @CreatedBy
  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = BaseEntity.CreatedByUserColumnName,
      foreignKey = @ForeignKey(name = "FK__AUTHORITY__USER__ID"))
  protected User createdByUser;

  public User getCreatedByUser() {
    return createdByUser;
  }

  public void setCreatedByUser(User createdByUser) {
    this.createdByUser = createdByUser;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public String getAuthority() {
    return authority;
  }

  public void setAuthority(String authority) {
    this.authority = authority;
  }
}
