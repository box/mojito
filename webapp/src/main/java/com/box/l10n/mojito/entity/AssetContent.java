package com.box.l10n.mojito.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.Set;

/**
 * To save the content of an {@link Asset} for later processing. It is linked to a branch. Strictly
 * speaking we can have as many record as we want in the table but the job is cleaning old entries.
 *
 * <p>{@see com.box.l10n.mojito.service.assetExtraction.AssetExtractionCleanupJob}.
 *
 * @author jeanaurambault
 */
@Entity
@Table(name = "asset_content")
@NamedEntityGraph(
    name = "AssetContent.legacy",
    attributeNodes = {
      @NamedAttributeNode(value = "asset", subgraph = "AssetContent.legacy.asset"),
      @NamedAttributeNode("branch")
    },
    subgraphs = {
      @NamedSubgraph(
          name = "AssetContent.legacy.asset",
          attributeNodes = @NamedAttributeNode(value = "repository")),
    })
public class AssetContent extends AuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "asset_id", foreignKey = @ForeignKey(name = "FK__ASSET_CONTENT__ASSET__ID"))
  private Asset asset;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "branch_id", foreignKey = @ForeignKey(name = "FK__ASSET_CONTENT__BRANCH__ID"))
  private Branch branch;

  @Basic(optional = false)
  @Column(name = "content", length = Integer.MAX_VALUE)
  private String content;

  @Column(name = "content_md5", length = 32)
  private String contentMd5;

  @Column(name = "extracted_content")
  private boolean extractedContent = false;

  @JsonManagedReference("assetContent")
  @OneToMany(mappedBy = "assetContent")
  private Set<AssetExtraction> assetExtractions;

  public Asset getAsset() {
    return asset;
  }

  public void setAsset(Asset asset) {
    this.asset = asset;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getContentMd5() {
    return contentMd5;
  }

  public void setContentMd5(String contentMd5) {
    this.contentMd5 = contentMd5;
  }

  public boolean isExtractedContent() {
    return extractedContent;
  }

  public void setExtractedContent(boolean extractedContent) {
    this.extractedContent = extractedContent;
  }

  public Branch getBranch() {
    return branch;
  }

  public void setBranch(Branch branch) {
    this.branch = branch;
  }

  public Set<AssetExtraction> getAssetExtractions() {
    return assetExtractions;
  }

  public void setAssetExtractions(Set<AssetExtraction> assetExtractions) {
    this.assetExtractions = assetExtractions;
  }
}
