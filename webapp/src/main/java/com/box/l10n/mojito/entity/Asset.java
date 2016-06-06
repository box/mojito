package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.entity.security.user.User;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.springframework.data.annotation.CreatedBy;

/**
 * Entity that describes an asset. It contains the information about the
 * repository’s assets:
 *
 * <li> location of the resource bundles or images to localize in the remote
 * codebase
 * <li> pointer to the current extraction of the asset
 *
 * @author aloison
 */
@Entity
@Table(
        name = "asset",
        indexes = {
            @Index(name = "UK__ASSET__REPOSITORY_ID__PATH", columnList = "repository_id, path", unique = true),
            @Index(name = "I__ASSET__CONTENT_MD5", columnList = "content_md5", unique = false)
        }
)
public class Asset extends AuditableEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "repository_id", foreignKey = @ForeignKey(name = "FK__ASSET__REPOSITORY__ID"))
    private Repository repository;

    @Basic(optional = false)
    @Column(name = "path")
    private String path;

    @Basic(optional = false)
    @Column(name = "content", length = Integer.MAX_VALUE)
    private String content;

    @Column(name = "content_md5", length = 32)
    private String contentMd5;

    @OneToOne
    @JoinColumn(name = "last_successful_asset_extraction_id", foreignKey = @ForeignKey(name = "FK__ASSET__ASSET_EXTRACTION__ID"))
    private AssetExtraction lastSuccessfulAssetExtraction;

    @CreatedBy
    @ManyToOne
    @JoinColumn(name = BaseEntity.CreatedByUserColumnName, foreignKey = @ForeignKey(name = "FK__ASSET__USER__ID"))
    protected User createdByUser;

    /**
     * To mark an Asset as deleted so it does not get processed anymore.
     */
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;
    
    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
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

    public AssetExtraction getLastSuccessfulAssetExtraction() {
        return lastSuccessfulAssetExtraction;
    }

    public void setLastSuccessfulAssetExtraction(AssetExtraction lastSuccessfulAssetExtraction) {
        this.lastSuccessfulAssetExtraction = lastSuccessfulAssetExtraction;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
    
}
