package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.okapi.FilterConfigIdOverride;

import java.util.List;

public class ProcessAssetJobInput {
    Long assetContentId;
    FilterConfigIdOverride filterConfigIdOverride;
    List<String> filterOptions;

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

    public List<String> getFilterOptions() {
        return filterOptions;
    }

    public void setFilterOptions(List<String> filterOptions) {
        this.filterOptions = filterOptions;
    }
}
