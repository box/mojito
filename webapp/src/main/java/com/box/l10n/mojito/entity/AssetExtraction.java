package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.entity.security.user.User;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import org.springframework.data.annotation.CreatedBy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity that contains information about the extraction of assets. Its purpose is to avoid race condition issues:
 * if two processes push the same asset at the same time, with different content, the result will be unpredictable
 * but we can easily change which one will win by moving the currently used asset pointer in the Asset entity.
 *
 * @author aloison
 */
@Entity
@Table(name = "asset_extraction")
public class AssetExtraction extends AuditableEntity {

    @JsonBackReference("asset")
    @ManyToOne
    @JoinColumn(name = "asset_id", foreignKey = @ForeignKey(name = "FK__ASSET_EXTRACTION__ASSET__ID"))
    private Asset asset;

    @JsonBackReference("assetContent")
    @ManyToOne
    @JoinColumn(name = "asset_content_id", foreignKey = @ForeignKey(name = "FK__ASSET_EXTRACTION__ASSET_CONTENT__ID"))
    private AssetContent assetContent;

    @Column(name = "content_md5", length = 32)
    private String contentMd5;

    @JsonManagedReference("assetTextUnits")
    @OneToMany(mappedBy = "assetExtraction")
    private Set<AssetTextUnit> assetTextUnits = new HashSet<>();

    @JsonManagedReference("assetExtractionByBranches")
    @OneToMany(mappedBy = "assetExtraction")
    private Set<AssetExtractionByBranch> assetExtractionByBranches = new HashSet<>();

    @OneToOne
    @JoinColumn(name = "pollable_task_id", foreignKey = @ForeignKey(name = "FK__ASSET_EXTRACTION__POLLABLE_TASK__ID"))
    private PollableTask pollableTask;

    @CreatedBy
    @ManyToOne
    @JoinColumn(name = BaseEntity.CreatedByUserColumnName, foreignKey = @ForeignKey(name = "FK__ASSET_EXTRACTION__USER__ID"))
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
}
