package com.box.l10n.mojito.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.Table;

/**
 * Tracks the current {@link AssetExtraction}s of an asset by {@link Branch}.
 *
 * <p>Supports soft deletes.
 *
 * @author jeanaurambault
 */
@Entity
@Table(
    name = "asset_extraction_by_branch",
    indexes = {
      @Index(
          name = "UK__ASSET_EXTRACTION_BY_BRANCH__ASSET_ID__BRANCH",
          columnList = "asset_id, branch_id",
          unique = true)
    })
@NamedEntityGraph(
    name = "AssetExtractionByBranch.legacy",
    attributeNodes = {@NamedAttributeNode(value = "assetExtraction")})
public class AssetExtractionByBranch extends AuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "asset_id",
      foreignKey = @ForeignKey(name = "FK__ASSET_EXTRACTION_BY_BRANCH__ASSET__ID"))
  private Asset asset;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "branch_id",
      foreignKey = @ForeignKey(name = "FK__ASSET_EXTRACTION_BY_BRANCH__BRANCH__ID"))
  private Branch branch;

  @JsonBackReference("assetExtractionByBranches")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "asset_extraction_id",
      foreignKey = @ForeignKey(name = "FK__ASSET_EXTRACTION_BY_BRANCH__ASSET_EXTRACTION__ID"))
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
