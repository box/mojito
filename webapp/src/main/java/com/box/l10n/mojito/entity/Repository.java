package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.rest.View;
import com.box.l10n.mojito.service.drop.exporter.DropExporterType;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.springframework.data.annotation.CreatedBy;

/**
 * Entity that describes a repository.
 *
 * @author aloison
 */
@Entity
@NamedEntityGraph(name = "Repository.statistics",
  attributeNodes = @NamedAttributeNode("repositoryStatistic"))
@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
@Table(name = "repository",
        indexes = {
            @Index(name = "UK__REPOSITORY__NAME", columnList = "name", unique = true)
        })
public class Repository extends AuditableEntity {

    public static final int NAME_MAX_LENGTH = 255;
    
    @Basic(optional = false)
    @Column(name = "name", length = NAME_MAX_LENGTH)
    @JsonView(View.IdAndName.class)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "drop_exporter_type")
    @Enumerated(EnumType.STRING)
    private DropExporterType dropExporterType;

    @JsonManagedReference
    @OneToMany(mappedBy = "repository", fetch = FetchType.EAGER)
    Set<RepositoryLocale> repositoryLocales = new HashSet<>();

    @OneToOne(fetch = FetchType.LAZY)
    @Basic(optional = false)
    @JoinColumn(name = "repository_statistic_id", foreignKey = @ForeignKey(name = "FK__REPOSITORY__REPOSITORY_STATISTIC__ID"))
    RepositoryStatistic repositoryStatistic;

    @JsonManagedReference
    @OneToMany(mappedBy = "repository", fetch = FetchType.EAGER)
    Set<AssetIntegrityChecker> assetIntegrityCheckers = new HashSet<>();

    @OneToOne
    @Basic(optional = false)
    @JoinColumn(name = "tm_id", foreignKey = @ForeignKey(name = "FK__REPOSITORY__TM__ID"))
    TM tm;

    @CreatedBy
    @ManyToOne
    @JoinColumn(name = BaseEntity.CreatedByUserColumnName, foreignKey = @ForeignKey(name = "FK__REPOSITORY__USER__ID"))
    protected User createdByUser;

    /**
     * To mark a Repository as deleted so it can be hidden in a Repositories
     * page.
     */
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * NOTE Be careful when using this method because this returns the entire
     * list of locales including the root locale. Use
     * {@link RepositoryService#getRepositoryLocalesWithoutRootLocale(Repository)}
     * to get the list without the root locale.
     *
     * @return
     */
    public Set<RepositoryLocale> getRepositoryLocales() {
        return repositoryLocales;
    }

    public void setRepositoryLocales(Set<RepositoryLocale> repositoryLocales) {
        this.repositoryLocales = repositoryLocales;
    }

    public TM getTm() {
        return tm;
    }

    public void setTm(TM tm) {
        this.tm = tm;
    }

    public DropExporterType getDropExporterType() {
        return dropExporterType;
    }

    public void setDropExporterType(DropExporterType exporterType) {
        this.dropExporterType = exporterType;
    }

    public RepositoryStatistic getRepositoryStatistic() {
        return repositoryStatistic;
    }

    public void setRepositoryStatistic(RepositoryStatistic repositoryStatistic) {
        this.repositoryStatistic = repositoryStatistic;
    }

    public Set<AssetIntegrityChecker> getAssetIntegrityCheckers() {
        return assetIntegrityCheckers;
    }

    public void setAssetIntegrityCheckers(Set<AssetIntegrityChecker> assetIntegrityCheckers) {
        this.assetIntegrityCheckers = assetIntegrityCheckers;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

}
