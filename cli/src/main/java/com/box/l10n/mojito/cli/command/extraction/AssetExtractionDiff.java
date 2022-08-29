package com.box.l10n.mojito.cli.command.extraction;

import com.box.l10n.mojito.okapi.FilterConfigIdOverride;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import java.util.ArrayList;
import java.util.List;

public class AssetExtractionDiff {

  FilterConfigIdOverride currentFilterConfigIdOverride;

  List<String> currentFilterOptions;

  List<AssetExtractorTextUnit> addedTextunits = new ArrayList<>();

  List<AssetExtractorTextUnit> removedTextunits = new ArrayList<>();

  public FilterConfigIdOverride getCurrentFilterConfigIdOverride() {
    return currentFilterConfigIdOverride;
  }

  public void setCurrentFilterConfigIdOverride(
      FilterConfigIdOverride currentFilterConfigIdOverride) {
    this.currentFilterConfigIdOverride = currentFilterConfigIdOverride;
  }

  public List<String> getCurrentFilterOptions() {
    return currentFilterOptions;
  }

  public void setCurrentFilterOptions(List<String> currentFilterOptions) {
    this.currentFilterOptions = currentFilterOptions;
  }

  public List<AssetExtractorTextUnit> getAddedTextunits() {
    return addedTextunits;
  }

  public void setAddedTextunits(List<AssetExtractorTextUnit> addedTextunits) {
    this.addedTextunits = addedTextunits;
  }

  public List<AssetExtractorTextUnit> getRemovedTextunits() {
    return removedTextunits;
  }

  public void setRemovedTextunits(List<AssetExtractorTextUnit> removedTextunits) {
    this.removedTextunits = removedTextunits;
  }
}
