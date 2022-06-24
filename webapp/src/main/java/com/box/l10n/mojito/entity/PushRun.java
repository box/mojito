package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonView;
import org.joda.time.DateTime;

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
 * Represents a single instance/run of the push command against a repository.
 *
 * @author garion
 */
@Entity
@Table(
        name = "push_run",
        indexes = {
                @Index(name = "UK__PUSH_RUN__NAME", columnList = "name", unique = true)
        }
)
public class PushRun extends SettableAuditableEntity {
    @ManyToOne
    @JoinColumn(name = "repository_id",
            foreignKey = @ForeignKey(name = "FK__PUSH_RUN__REPOSITORY_ID"))
    private Repository repository;

    /**
     * A unique identifier that is provided for the push run, generally a UUID.
     */
    @JsonView(View.CommitDetailed.class)
    @Column(name = "name")
    private String name;

    @OneToMany(mappedBy = "pushRun")
    @JsonManagedReference
    private Set<PushRunAsset> pushRunAssets;

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

    public DateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(DateTime createdDate) {
        this.createdDate = createdDate;
    }

    public Set<PushRunAsset> getPushRunAssets() {
        return pushRunAssets;
    }

    public void setPushRunAssets(Set<PushRunAsset> pushRunAssetSet) {
        this.pushRunAssets = pushRunAssetSet;
    }
}
