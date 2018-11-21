package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.rest.asset.FilterConfigIdOverride;

public class ProcessAssetJobInput {
    Long assetContentId;
    FilterConfigIdOverride filterConfigIdOverride;

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
