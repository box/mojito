package com.box.l10n.mojito.entity;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;

/**
 * Similar to {@link AuditableEntity} but allows to override the attributes.
 * <p/>
 * Spring doesn't allow to set dates manually via the setter with the
 * annotations and the listener
 */
@MappedSuperclass
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
