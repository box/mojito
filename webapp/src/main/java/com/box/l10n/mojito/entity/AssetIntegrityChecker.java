package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckerType;
import com.fasterxml.jackson.annotation.JsonBackReference;
import javax.persistence.Basic;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Entity that assign an integrity check for a type of asset.
 *
 * @author wyau
 */
@Entity
@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
@Table(
    name = "asset_integrity_checker",
    indexes = {
        @Index(name = "UK__ASSET_INTEGRITY_CHECKER__REPOSITORY_ID__ASSET_EXTENSION", columnList = "repository_id, asset_extension", unique = true)
    }
)
public class AssetIntegrityChecker extends BaseEntity {

    @ManyToOne
    @JsonBackReference
    @JoinColumn(name = "repository_id", foreignKey = @ForeignKey(name = "FK__ASSET_INTEGRITY_CHECKER__REPOSITORY__ID"), nullable = false)
    private Repository repository;

    @Basic(optional = false)
    @Column(name = "asset_extension")
    private String assetExtension;

    @Basic(optional = false)
    @Column(name = "integrity_checker_type")
    @Enumerated(EnumType.STRING)
    private IntegrityCheckerType integrityCheckerType;

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public String getAssetExtension() {
        return assetExtension;
    }

    public void setAssetExtension(String assetExtension) {
        this.assetExtension = assetExtension;
    }

    public IntegrityCheckerType getIntegrityCheckerType() {
        return integrityCheckerType;
    }

    public void setIntegrityCheckerType(IntegrityCheckerType integrityCheckerType) {
        this.integrityCheckerType = integrityCheckerType;
    }
}
