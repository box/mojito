package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonView;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;

/**
 * @author wyau
 */
@MappedSuperclass
public abstract class BaseEntity implements Serializable {

    protected static final String CreatedByUserColumnName = "created_by_user_id";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonView(View.IdAndName.class)
    protected Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
