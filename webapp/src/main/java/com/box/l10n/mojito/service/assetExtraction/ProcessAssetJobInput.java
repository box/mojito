package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.rest.asset.FilterConfigIdOverride;

public class ProcessAssetJobInput {
    Long assetContentId;
    FilterConfigIdOverride filterConfigIdOverride;
    String filterOptions;

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

    public String getFilterOptions() {
        return filterOptions;
    }

    public void setFilterOptions(String filterOptions) {
        this.filterOptions = filterOptions;
    }
}
