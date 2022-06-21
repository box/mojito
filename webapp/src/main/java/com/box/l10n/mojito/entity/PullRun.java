package com.box.l10n.mojito.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Set;

/**
 * @author garion
 */
@Entity
@Table(
        name = "pull_run",
        indexes = {
                @Index(name = "UK__PULL_RUN__NAME", columnList = "name", unique = true)
        }
)
public class PullRun extends SettableAuditableEntity {
    @ManyToOne
    @JoinColumn(name = "repository_id",
            foreignKey = @ForeignKey(name = "FK__PULL_RUN__REPOSITORY_ID"))
    private Repository repository;

    @Column(name = "name")
    private String name;

    @OneToMany(mappedBy = "pullRun")
    @JsonManagedReference
    private Set<PullRunAsset> pullRunAssets;

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<PullRunAsset> getPullRunAssets() {
        return pullRunAssets;
    }

    public void setPullRunAssets(Set<PullRunAsset> pullRunAssets) {
        this.pullRunAssets = pullRunAssets;
    }
}
