package com.box.l10n.mojito.entity.security.group;

import com.box.l10n.mojito.entity.AuditableEntity;
import com.box.l10n.mojito.entity.BaseEntity;
import com.box.l10n.mojito.entity.security.user.User;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.springframework.data.annotation.CreatedBy;

/** @author wyau */
@Entity
@Table(name = "group_members")
public class GroupMember extends AuditableEntity {

  @ManyToOne
  @JoinColumn(
      name = "username",
      foreignKey = @ForeignKey(name = "FK__GROUP_MEMBER__USER__USERNAME"),
      nullable = false)
  User user;

  @ManyToOne
  @JoinColumn(
      name = "group_id",
      foreignKey = @ForeignKey(name = "FK__GROUP_MEMBER__GROUP__ID"),
      nullable = false)
  Group group;

  @CreatedBy
  @ManyToOne
  @JoinColumn(
      name = BaseEntity.CreatedByUserColumnName,
      foreignKey = @ForeignKey(name = "FK__GROUP_MEMBER__USER__ID"))
  protected User createdByUser;

  public User getCreatedByUser() {
    return createdByUser;
  }

  public void setCreatedByUser(User createdByUser) {
    this.createdByUser = createdByUser;
  }
}
