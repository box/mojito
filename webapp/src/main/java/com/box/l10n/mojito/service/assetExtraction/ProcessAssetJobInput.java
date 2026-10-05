package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.okapi.FilterConfigIdOverride;
import com.box.l10n.mojito.rest.leveraging.CopyTmConfig.PreserveStatusMode;
import java.util.List;

public class ProcessAssetJobInput {
  Long assetContentId;
  Long pushRunId;
  FilterConfigIdOverride filterConfigIdOverride;
  List<String> filterOptions;
  PreserveStatusMode preserveStatusMode = PreserveStatusMode.PRECISION;

  public Long getAssetContentId() {
    return assetContentId;
  }

  public void setAssetContentId(Long assetContentId) {
    this.assetContentId = assetContentId;
  }

  public Long getPushRunId() {
    return pushRunId;
  }

  public void setPushRunId(Long pushRunId) {
    this.pushRunId = pushRunId;
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

  public PreserveStatusMode getPreserveStatusMode() {
    return preserveStatusMode;
  }

  public void setPreserveStatusMode(PreserveStatusMode preserveStatusMode) {
    this.preserveStatusMode = preserveStatusMode;
  }
}
