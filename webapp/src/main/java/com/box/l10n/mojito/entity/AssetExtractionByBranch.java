package com.box.l10n.mojito.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Tracks the current {@link AssetExtraction}s of an asset by {@link Branch}.
 *
 * Supports soft deletes.
 *
 * @author jeanaurambault
 */
@Entity
@Table(name = "asset_extraction_by_branch",
        indexes = {
                @Index(name = "UK__ASSET_EXTRACTION_BY_BRANCH__ASSET_ID__BRANCH", columnList = "asset_id, branch_id", unique = true)
        }
)
public class AssetExtractionByBranch extends AuditableEntity {

    @ManyToOne
    @JoinColumn(name = "asset_id", foreignKey = @ForeignKey(name = "FK__ASSET_EXTRACTION_BY_BRANCH__ASSET__ID"))
    private Asset asset;

    @ManyToOne
    @JoinColumn(name = "branch_id", foreignKey = @ForeignKey(name = "FK__ASSET_EXTRACTION_BY_BRANCH__BRANCH__ID"))
    private Branch branch;

    @JsonBackReference("assetExtractionByBranches")
    @ManyToOne
    @JoinColumn(name = "asset_extraction_id", foreignKey = @ForeignKey(name = "FK__ASSET_EXTRACTION_BY_BRANCH__ASSET_EXTRACTION__ID"))
    private AssetExtraction assetExtraction;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    public Asset getAsset() {
        return asset;
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public AssetExtraction getAssetExtraction() {
        return assetExtraction;
    }

    public void setAssetExtraction(AssetExtraction assetExtraction) {
        this.assetExtraction = assetExtraction;
    }

}
