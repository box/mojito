package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.entity.security.user.User;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import io.swagger.v3.oas.annotations.media.Schema;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.HashSet;
import java.util.Set;
import org.springframework.data.annotation.CreatedBy;

/**
 * Entity that contains information about the extraction of assets. Its purpose is to avoid race
 * condition issues: if two processes push the same asset at the same time, with different content,
 * the result will be unpredictable but we can easily change which one will win by moving the
 * currently used asset pointer in the Asset entity.
 *
 * @author aloison
 */
@Entity
@Table(name = "asset_extraction")
@NamedEntityGraph(
    name = "AssetExtraction.legacy",
    attributeNodes = {
      @NamedAttributeNode(value = "asset", subgraph = "AssetExtraction.legacy.asset"),
      @NamedAttributeNode("assetContent")
    },
    subgraphs = {
      @NamedSubgraph(
          name = "AssetExtraction.legacy.asset",
          attributeNodes = {
            @NamedAttributeNode(value = "repository"),
          })
    })
public class AssetExtraction extends AuditableEntity {

  @JsonBackReference("asset")
  @Schema(hidden = true)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "asset_id", foreignKey = @ForeignKey(name = "FK__ASSET_EXTRACTION__ASSET__ID"))
  private Asset asset;

  /**
   * With new implementation where the asset extraction is updated over time the asset content will
   * be null. (before we would create an asset extraction every single time and have a mapping
   * there)
   */
  @JsonBackReference("assetContent")
  @Schema(hidden = true)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "asset_content_id",
      foreignKey = @ForeignKey(name = "FK__ASSET_EXTRACTION__ASSET_CONTENT__ID"))
  private AssetContent assetContent;

  @Column(name = "content_md5", length = 32)
  private String contentMd5;

  @Column(name = "filter_options_md5", length = 32)
  private String filterOptionsMd5;

  @JsonManagedReference("assetTextUnits")
  @OneToMany(mappedBy = "assetExtraction")
  private Set<AssetTextUnit> assetTextUnits = new HashSet<>();

  @JsonManagedReference("assetExtractionByBranches")
  @OneToMany(mappedBy = "assetExtraction")
  private Set<AssetExtractionByBranch> assetExtractionByBranches = new HashSet<>();

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "pollable_task_id",
      foreignKey = @ForeignKey(name = "FK__ASSET_EXTRACTION__POLLABLE_TASK__ID"))
  private PollableTask pollableTask;

  @Column(name = "version")
  @Version
  private Long version = 0L;

  @CreatedBy
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = BaseEntity.CreatedByUserColumnName,
      foreignKey = @ForeignKey(name = "FK__ASSET_EXTRACTION__USER__ID"))
  private User createdByUser;

  public User getCreatedByUser() {
    return createdByUser;
  }

  public void setCreatedByUser(User createdByUser) {
    this.createdByUser = createdByUser;
  }

  public Asset getAsset() {
    return asset;
  }

  public void setAsset(Asset asset) {
    this.asset = asset;
  }

  public String getContentMd5() {
    return contentMd5;
  }

  public void setContentMd5(String contentMd5) {
    this.contentMd5 = contentMd5;
  }

  public String getFilterOptionsMd5() {
    return filterOptionsMd5;
  }

  public void setFilterOptionsMd5(String filterOptionsMd5) {
    this.filterOptionsMd5 = filterOptionsMd5;
  }

  public Set<AssetTextUnit> getAssetTextUnits() {
    return assetTextUnits;
  }

  public void setAssetTextUnits(Set<AssetTextUnit> assetTextUnits) {
    this.assetTextUnits = assetTextUnits;
  }

  public PollableTask getPollableTask() {
    return pollableTask;
  }

  public void setPollableTask(PollableTask pollableTask) {
    this.pollableTask = pollableTask;
  }

  public AssetContent getAssetContent() {
    return assetContent;
  }

  public void setAssetContent(AssetContent assetContent) {
    this.assetContent = assetContent;
  }

  public Set<AssetExtractionByBranch> getAssetExtractionByBranches() {
    return assetExtractionByBranches;
  }

  public void setAssetExtractionByBranches(Set<AssetExtractionByBranch> assetExtractionByBranches) {
    this.assetExtractionByBranches = assetExtractionByBranches;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }
}
