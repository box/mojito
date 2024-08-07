package com.box.l10n.mojito.rest.textunit;

import com.box.l10n.mojito.service.tm.search.SearchType;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import java.time.ZonedDateTime;
import java.util.ArrayList;

class TextUnitSearchBody {
  ArrayList<Long> repositoryIds;
  ArrayList<String> repositoryNames;
  ArrayList<Long> tmTextUnitIds;
  String name;
  String source;
  String target;
  String assetPath;
  String pluralFormOther;
  String locationUsage;
  boolean pluralFormFiltered = true;
  boolean pluralFormExcluded = false;
  SearchType searchType = SearchType.EXACT;
  ArrayList<String> localeTags;
  UsedFilter usedFilter;
  StatusFilter statusFilter;
  Boolean doNotTranslateFilter;
  ZonedDateTime tmTextUnitCreatedBefore;
  ZonedDateTime tmTextUnitCreatedAfter;
  Long branchId;
  Integer limit = 10;
  Integer offset = 0;

  public ArrayList<Long> getRepositoryIds() {
    return repositoryIds;
  }

  public void setRepositoryIds(ArrayList<Long> repositoryIds) {
    this.repositoryIds = repositoryIds;
  }

  public ArrayList<String> getRepositoryNames() {
    return repositoryNames;
  }

  public void setRepositoryNames(ArrayList<String> repositoryNames) {
    this.repositoryNames = repositoryNames;
  }

  public ArrayList<Long> getTmTextUnitIds() {
    return tmTextUnitIds;
  }

  public void setTmTextUnitIds(ArrayList<Long> tmTextUnitIds) {
    this.tmTextUnitIds = tmTextUnitIds;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public String getAssetPath() {
    return assetPath;
  }

  public void setAssetPath(String assetPath) {
    this.assetPath = assetPath;
  }

  public String getPluralFormOther() {
    return pluralFormOther;
  }

  public void setPluralFormOther(String pluralFormOther) {
    this.pluralFormOther = pluralFormOther;
  }

  public String getLocationUsage() {
    return this.locationUsage;
  }

  public void setLocationUsage(String locationUsage) {
    this.locationUsage = locationUsage;
  }

  public boolean isPluralFormFiltered() {
    return pluralFormFiltered;
  }

  public void setPluralFormFiltered(boolean pluralFormFiltered) {
    this.pluralFormFiltered = pluralFormFiltered;
  }

  public boolean isPluralFormExcluded() {
    return pluralFormExcluded;
  }

  public void setPluralFormExcluded(boolean pluralFormExcluded) {
    this.pluralFormExcluded = pluralFormExcluded;
  }

  public SearchType getSearchType() {
    return searchType;
  }

  public void setSearchType(SearchType searchType) {
    this.searchType = searchType;
  }

  public ArrayList<String> getLocaleTags() {
    return localeTags;
  }

  public void setLocaleTags(ArrayList<String> localeTags) {
    this.localeTags = localeTags;
  }

  public UsedFilter getUsedFilter() {
    return usedFilter;
  }

  public void setUsedFilter(UsedFilter usedFilter) {
    this.usedFilter = usedFilter;
  }

  public StatusFilter getStatusFilter() {
    return statusFilter;
  }

  public void setStatusFilter(StatusFilter statusFilter) {
    this.statusFilter = statusFilter;
  }

  public Boolean getDoNotTranslateFilter() {
    return doNotTranslateFilter;
  }

  public void setDoNotTranslateFilter(Boolean doNotTranslateFilter) {
    this.doNotTranslateFilter = doNotTranslateFilter;
  }

  public ZonedDateTime getTmTextUnitCreatedBefore() {
    return tmTextUnitCreatedBefore;
  }

  public void setTmTextUnitCreatedBefore(ZonedDateTime tmTextUnitCreatedBefore) {
    this.tmTextUnitCreatedBefore = tmTextUnitCreatedBefore;
  }

  public ZonedDateTime getTmTextUnitCreatedAfter() {
    return tmTextUnitCreatedAfter;
  }

  public void setTmTextUnitCreatedAfter(ZonedDateTime tmTextUnitCreatedAfter) {
    this.tmTextUnitCreatedAfter = tmTextUnitCreatedAfter;
  }

  public Long getBranchId() {
    return branchId;
  }

  public void setBranchId(Long branchId) {
    this.branchId = branchId;
  }

  public Integer getLimit() {
    return limit;
  }

  public void setLimit(Integer limit) {
    this.limit = limit;
  }

  public Integer getOffset() {
    return offset;
  }

  public void setOffset(Integer offset) {
    this.offset = offset;
  }
}
