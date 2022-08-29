package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.entity.security.user.User;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.CreatedBy;

/**
 * Represents a TM (translation memory), basically contains a list of {@link TMTextUnit}
 *
 * @author jaurambault
 */
@Entity
@BatchSize(size = 1000)
public class TM extends AuditableEntity {

  @CreatedBy
  @ManyToOne
  @JoinColumn(
      name = BaseEntity.CreatedByUserColumnName,
      foreignKey = @ForeignKey(name = "FK__TM__USER__ID"))
  protected User createdByUser;

  @OneToMany(mappedBy = "tm")
  Set<Repository> repositories = new HashSet<>();

  public User getCreatedByUser() {
    return createdByUser;
  }

  public void setCreatedByUser(User createdByUser) {
    this.createdByUser = createdByUser;
  }
}
