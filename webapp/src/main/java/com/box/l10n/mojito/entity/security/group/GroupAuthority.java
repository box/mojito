package com.box.l10n.mojito.entity.security.group;

import com.box.l10n.mojito.entity.AuditableEntity;
import com.box.l10n.mojito.entity.BaseEntity;
import com.box.l10n.mojito.entity.security.user.User;
import org.springframework.data.annotation.CreatedBy;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * @author wyau
 */
@Entity
@Table(name = "group_authorities")
public class GroupAuthority extends AuditableEntity {

    @ManyToOne
    @JoinColumn(name = "group_id", foreignKey = @ForeignKey(name = "FK__GROUP_AUTHORITY__GROUP__ID"), nullable = false)
    Group group;

    @Column(name = "authority", nullable = false)
    String authority;

    @CreatedBy
    @ManyToOne
    @JoinColumn(name = BaseEntity.CreatedByUserColumnName, foreignKey = @ForeignKey(name = "FK__GROUP_AUTHORITY__USER__ID"))
    protected User createdByUser;

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }
}
