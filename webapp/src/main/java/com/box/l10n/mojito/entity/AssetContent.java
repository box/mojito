package com.box.l10n.mojito.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Set;

/**
 * To save the content of an {@link Asset} for later processing. It is linked to a branch. Strictly speaking we can have
 * as many record as we want in the table but the job is cleaning old entries.
 *
 * {@see com.box.l10n.mojito.service.assetExtraction.AssetExtractionCleanupJob}.
 *
 * @author jeanaurambault
 */
@Entity
@Table(name = "asset_content")
public class AssetContent extends AuditableEntity {

    @ManyToOne
    @JoinColumn(name = "asset_id", foreignKey = @ForeignKey(name = "FK__ASSET_CONTENT__ASSET__ID"))
    private Asset asset;

    @ManyToOne
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
