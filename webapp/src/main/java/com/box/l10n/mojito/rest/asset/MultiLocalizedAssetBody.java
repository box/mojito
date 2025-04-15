package com.box.l10n.mojito.rest.asset;

import com.box.l10n.mojito.okapi.FilterConfigIdOverride;
import com.box.l10n.mojito.okapi.InheritanceMode;
import com.box.l10n.mojito.okapi.Status;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiLocalizedAssetBody {

  /** Asset id */
  Long assetId;

  /** Locale infos */
  List<LocaleInfo> localeInfos;

  /** LocalizedAssetBody outputs */
  Map<String, Long> generateLocalizedAssetJobIds = new HashMap<>();

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

  InheritanceMode inheritanceMode = InheritanceMode.USE_PARENT;

  Status status = Status.ALL;

  String schedulerName;

  String appendTextUnitsId;

  public Long getAssetId() {
    return assetId;
  }

  public void setAssetId(Long assetId) {
    this.assetId = assetId;
  }

  public List<LocaleInfo> getLocaleInfos() {
    return localeInfos;
  }

  public void setLocaleInfos(List<LocaleInfo> localeInfos) {
    this.localeInfos = localeInfos;
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

  public InheritanceMode getInheritanceMode() {
    return inheritanceMode;
  }

  public void setInheritanceMode(InheritanceMode inheritanceMode) {
    this.inheritanceMode = inheritanceMode;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public Map<String, Long> getGenerateLocalizedAssetJobIds() {
    return generateLocalizedAssetJobIds;
  }

  public void addGenerateLocalizedAddedJobIdToMap(
      String bcp47Tag, Long generateLocalizedAssetJobId) {
    this.generateLocalizedAssetJobIds.put(bcp47Tag, generateLocalizedAssetJobId);
  }

  public String getSchedulerName() {
    return schedulerName;
  }

  public void setSchedulerName(String schedulerName) {
    this.schedulerName = schedulerName;
  }

  public String getAppendTextUnitsId() {
    return appendTextUnitsId;
  }

  public void setAppendTextUnitsId(String appendTextUnitsId) {
    this.appendTextUnitsId = appendTextUnitsId;
  }
}
