package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.okapi.FilterConfigIdOverride;
import com.box.l10n.mojito.okapi.ImportTranslationsFromLocalizedAssetStep;
import java.util.List;

public class ImportLocalizedAssetJobInput {

  Long assetId;
  Long localeId;
  String content;
  ImportTranslationsFromLocalizedAssetStep.StatusForEqualTarget statusForEqualtarget;
  FilterConfigIdOverride filterConfigIdOverride;
  List<String> filterOptions;

  public Long getAssetId() {
    return assetId;
  }

  public void setAssetId(Long assetId) {
    this.assetId = assetId;
  }

  public Long getLocaleId() {
    return localeId;
  }

  public void setLocaleId(Long localeId) {
    this.localeId = localeId;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public ImportTranslationsFromLocalizedAssetStep.StatusForEqualTarget getStatusForEqualtarget() {
    return statusForEqualtarget;
  }

  public void setStatusForEqualtarget(
      ImportTranslationsFromLocalizedAssetStep.StatusForEqualTarget statusForEqualtarget) {
    this.statusForEqualtarget = statusForEqualtarget;
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
