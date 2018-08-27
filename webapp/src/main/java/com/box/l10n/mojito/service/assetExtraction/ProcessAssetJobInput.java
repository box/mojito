package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.rest.asset.FilterConfigIdOverride;

public class ProcessAssetJobInput {
    Long assetId;
    FilterConfigIdOverride filterConfigIdOverride;

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public FilterConfigIdOverride getFilterConfigIdOverride() {
        return filterConfigIdOverride;
    }

    public void setFilterConfigIdOverride(FilterConfigIdOverride filterConfigIdOverride) {
        this.filterConfigIdOverride = filterConfigIdOverride;
    }
}
