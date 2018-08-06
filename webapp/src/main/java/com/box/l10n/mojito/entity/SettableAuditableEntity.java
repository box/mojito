package com.box.l10n.mojito.entity;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Similar to {@link AuditableEntity} but allows to override the attributes.
 * <p/>
 * Spring doesn't allow to set dates manually via the setter with the
 * annotations and the listener
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class SettableAuditableEntity extends BaseEntity {

    @Column(name = "created_date")
    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    protected DateTime createdDate;

    public DateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(DateTime createdDate) {
        this.createdDate = createdDate;
    }

    @PrePersist
    public void onPrePersist() {
        if (createdDate == null) {
            createdDate = new DateTime();
        }
    }

}
