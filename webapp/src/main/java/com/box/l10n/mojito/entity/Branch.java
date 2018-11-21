package com.box.l10n.mojito.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Entity to manage branches of an {@link Asset}.
 *
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
public class Branch extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "repository_id", foreignKey = @ForeignKey(name = "FK__BRANCH__REPOSITORY__ID"))
    @JsonBackReference
    Repository repository;

    @Column(name = "name")
    String name;

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
}
