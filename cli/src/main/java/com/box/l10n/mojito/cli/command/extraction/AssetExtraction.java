package com.box.l10n.mojito.cli.command.extraction;

import com.box.l10n.mojito.okapi.FilterConfigIdOverride;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;

import java.util.List;

public class AssetExtraction {

    String name;

    List<AssetExtractorTextUnit> textunits;

    FilterConfigIdOverride filterConfigIdOverride;

    List<String> filterOptions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<AssetExtractorTextUnit> getTextunits() {
        return textunits;
    }

    public void setTextunits(List<AssetExtractorTextUnit> textunits) {
        this.textunits = textunits;
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
