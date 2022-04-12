package com.box.l10n.mojito.entity;


import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.base.Objects;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Entity to manage the different database-backed application cache instances/types.
 * Application cache entries {@link ApplicationCache} are always created against a specific cache type.
 *
 * @author garion
 */
@Entity
@Table(name = "application_cache_type")
@BatchSize(size = 1000)
public class ApplicationCacheType {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonView(View.IdAndName.class)
    protected short id;

    @Column(name = "name")
    private String name;

    public ApplicationCacheType() {
    }

    public ApplicationCacheType(String name) {
        this.name = name;
    }

    public ApplicationCacheType(short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    public void setId(short id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApplicationCacheType that = (ApplicationCacheType) o;
        return id == that.id && Objects.equal(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, name);
    }
}
