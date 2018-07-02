package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.annotations.VisibleForTesting;
import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;

/**
 * This class adds support for entity listener. It will track the creation date and modification date
 * of the entity extending this class.
 *
 * @author aloison
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity extends BaseEntity implements Serializable {

    @CreatedDate
    @Column(name = "created_date")
    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    @JsonView(View.IdAndNameAndCreated.class)
    protected DateTime createdDate;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    protected DateTime lastModifiedDate;

    public DateTime getCreatedDate() {
        return createdDate;
    }

    public DateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    @VisibleForTesting
    public void setCreatedDate(DateTime createdDate) {
        this.createdDate = createdDate;
    }

    public void setLastModifiedDate(DateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
}
