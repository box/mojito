package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.entity.security.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;
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
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = BaseEntity.CreatedByUserColumnName,
      foreignKey = @ForeignKey(name = "FK__TM__USER__ID"))
  protected User createdByUser;

  @OneToMany(mappedBy = Repository_.TM)
  protected Set<Repository> repositories = new HashSet<>();

  public User getCreatedByUser() {
    return createdByUser;
  }

  public void setCreatedByUser(User createdByUser) {
    this.createdByUser = createdByUser;
  }
}
