package com.box.l10n.mojito.rest.entity;

import com.box.l10n.mojito.okapi.FilterConfigIdOverride;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MultiLocalizedAssetBody {

  public enum InheritanceMode {

    /** If there is no translation the text unit should be removed */
    REMOVE_UNTRANSLATED,
    /** Look for translations in parent locales, if none it will fallback to the source */
    USE_PARENT
  }

  public enum Status {
    ALL,
    ACCEPTED_OR_NEEDS_REVIEW,
    ACCEPTED
  }

  /** Asset id */
  Long assetId;

  List<LocaleInfo> localeInfos;

  /**
   * content to be localized (similar to the asset content stored in TMS) in the request and
   * localized asset in the response.
   */
  String sourceContent;

  /**
   * Optional, can be null. Allows to specify a specific Okapi filter to use to process the asset
   */
  FilterConfigIdOverride filterConfigIdOverride;

  /** Optional, can be null. To pass options to the Okapi filter */
  List<String> filterOptions;

  /**
   * Optional, can be null. If a name is provided, a pull run will be recored when the file is
   * generated
   */
  String pullRunName;

  LocalizedAssetBody.InheritanceMode inheritanceMode;

  LocalizedAssetBody.Status status = LocalizedAssetBody.Status.ALL;

  /** LocalizedAssetBody job ids */
  Map<String, Long> generateLocalizedAssetJobIds;

  public Long getAssetId() {
    return assetId;
  }

  public void setAssetId(Long assetId) {
    this.assetId = assetId;
  }

  public String getSourceContent() {
    return sourceContent;
  }

  public void setSourceContent(String sourceContent) {
    this.sourceContent = sourceContent;
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

  public String getPullRunName() {
    return pullRunName;
  }

  public void setPullRunName(String pullRunName) {
    this.pullRunName = pullRunName;
  }

  public LocalizedAssetBody.InheritanceMode getInheritanceMode() {
    return inheritanceMode;
  }

  public void setInheritanceMode(LocalizedAssetBody.InheritanceMode inheritanceMode) {
    this.inheritanceMode = inheritanceMode;
  }

  public LocalizedAssetBody.Status getStatus() {
    return status;
  }

  public void setStatus(LocalizedAssetBody.Status status) {
    this.status = status;
  }

  public Map<String, Long> getGenerateLocalizedAssetJobIds() {
    return generateLocalizedAssetJobIds;
  }

  public void setGenerateLocalizedAssetJobIds(Map<String, Long> generateLocalizedAssetJobIds) {
    this.generateLocalizedAssetJobIds = generateLocalizedAssetJobIds;
  }

  public List<LocaleInfo> getLocaleInfos() {
    return localeInfos;
  }

  public void setLocaleInfos(List<LocaleInfo> localeInfos) {
    this.localeInfos = localeInfos;
  }
}
