package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.rest.asset.FilterConfigIdOverride;

public class ProcessAssetJobInput {
    String username;
    Long assetContentId;
    FilterConfigIdOverride filterConfigIdOverride;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getAssetContentId() {
        return assetContentId;
    }

    public void setAssetContentId(Long assetContentId) {
        this.assetContentId = assetContentId;
    }

    public FilterConfigIdOverride getFilterConfigIdOverride() {
        return filterConfigIdOverride;
    }

    public void setFilterConfigIdOverride(FilterConfigIdOverride filterConfigIdOverride) {
        this.filterConfigIdOverride = filterConfigIdOverride;
    }
}
