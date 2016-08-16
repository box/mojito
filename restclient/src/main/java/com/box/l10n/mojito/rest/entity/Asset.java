package com.box.l10n.mojito.rest.entity;

/**
 * Entity that describes an asset. It contains the information about the
 * repository’s assets:
 *
 * This entity mirrors: com.box.l10n.mojito.entity.Asset
 *
 * @author wyau
 */
public class Asset {

    private Long id;

    private Repository repository;

    private String path;

    private String content;

    private String contentMd5;

    private Long lastSuccessfulAssetExtractionId;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLastSuccessfulAssetExtractionId() {
        return lastSuccessfulAssetExtractionId;
    }

    public void setLastSuccessfulAssetExtractionId(Long lastSuccessfulAssetExtractionId) {
        this.lastSuccessfulAssetExtractionId = lastSuccessfulAssetExtractionId;
    }
}
