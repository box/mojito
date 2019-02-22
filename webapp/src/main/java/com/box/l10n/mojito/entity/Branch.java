package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.springframework.data.annotation.CreatedBy;

import javax.persistence.*;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Entity to manage branches of an {@link Asset}.
 * <p>
 * The branch name can be {@code null}. This will be used when no branch is provided to the system. Note {@code null}
 * complicates query when searching by name and {@link com.box.l10n.mojito.service.branch.BranchService#findByNameAndRepository(String, Repository)}
 * should be used instead of {@link com.box.l10n.mojito.service.branch.BranchRepository#findByNameAndRepository(String, Repository)}
 *
 * @author jeanaurambault
 */
@Entity
@Table(
        name = "branch",
        indexes = {
                @Index(name = "UK__BRANCH__REPOSITORY_ID__PATH", columnList = "repository_id, name", unique = true),
        }
)
public class Branch extends SettableAuditableEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "repository_id", foreignKey = @ForeignKey(name = "FK__BRANCH__REPOSITORY__ID"))
    @JsonView(View.BranchSummary.class)
    Repository repository;

    @JsonView(View.BranchSummary.class)
    @Column(name = "name")
    String name;

    @JsonView(View.BranchSummary.class)
    @CreatedBy
    @ManyToOne
    @JoinColumn(name = BaseEntity.CreatedByUserColumnName, foreignKey = @ForeignKey(name = "FK__BRANCH__USER__ID"))
    protected User createdByUser;

    @JsonView(View.BranchSummary.class)
    @Column(name = "deleted", nullable = false)
    Boolean deleted = false;

    @JsonView(View.BranchSummary.class)
    @OneToMany(mappedBy = "branch", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JsonDeserialize(as = LinkedHashSet.class)
    @OrderBy("id")
    Set<Screenshot> screenshots = new HashSet<>();

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

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public Set<Screenshot> getScreenshots() {
        return screenshots;
    }

    public void setScreenshots(Set<Screenshot> screenshots) {
        this.screenshots = screenshots;
    }
}
