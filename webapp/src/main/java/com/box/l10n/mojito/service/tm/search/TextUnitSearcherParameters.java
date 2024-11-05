package com.box.l10n.mojito.service.tm.search;

import com.box.l10n.mojito.service.NormalizationUtils;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Parameters for {@link
 * TextUnitSearcher#search(com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters) }
 *
 * @author jaurambault
 */
public class TextUnitSearcherParameters {

  String name;
  String source;
  String target;
  String assetPath;
  String pluralFormOther;
  String locationUsage;
  SearchType searchType;
  List<Long> repositoryIds;
  List<String> repositoryNames;
  List<Long> tmTextUnitIds;
  List<String> localeTags;
  Long localeId;
  UsedFilter usedFilter;
  StatusFilter statusFilter;
  Integer offset;
  Integer limit;
  Long assetId;
  Long tmId;
  String md5;
  boolean forRootLocale = false;
  boolean rootLocaleExcluded = true;
  Boolean toBeFullyTranslatedFilter;
  boolean pluralFormsFiltered = true;
  boolean pluralFormsExcluded = false;

  boolean isOrderedByTextUnitID = false;
  Long pluralFormId;
  Boolean doNotTranslateFilter;
  ZonedDateTime tmTextUnitCreatedBefore;
  ZonedDateTime tmTextUnitCreatedAfter;
  Long branchId;
  String skipTextUnitWithPattern;
  String includeTextUnitsWithPattern;
  String skipAssetPathWithPattern;
  boolean isExcludeUnexpiredPendingMT = false;
  Duration aiTranslationExpiryDuration;
  boolean shouldRetrieveUploadedFileUri = false;

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
    this.source = NormalizationUtils.normalize(source);
  }

  public SearchType getSearchType() {
    return searchType;
  }

  public void setSearchType(SearchType searchType) {
    this.searchType = searchType;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = NormalizationUtils.normalize(target);
  }

  public String getAssetPath() {
    return assetPath;
  }

  public void setAssetPath(String assetPath) {
    this.assetPath = assetPath;
  }

  public List<Long> getRepositoryIds() {
    return repositoryIds;
  }

  public void setRepositoryIds(Long repositoryId) {
    this.repositoryIds = Arrays.asList(repositoryId);
  }

  public void setRepositoryIds(List<Long> repositoryIds) {
    this.repositoryIds = repositoryIds;
  }

  public List<String> getLocaleTags() {
    return localeTags;
  }

  public void setLocaleTags(List<String> localeTags) {
    this.localeTags = localeTags;
  }

  public Long getLocaleId() {
    return localeId;
  }

  public void setLocaleId(Long localeId) {
    this.localeId = localeId;
  }

  public UsedFilter getUsedFilter() {
    return usedFilter;
  }

  public void setUsedFilter(UsedFilter usedFilter) {
    this.usedFilter = usedFilter;
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

  public Long getAssetId() {
    return assetId;
  }

  public void setAssetId(Long assetId) {
    this.assetId = assetId;
  }

  public Long getTmId() {
    return tmId;
  }

  public void setTmId(Long tmId) {
    this.tmId = tmId;
  }

  public String getMd5() {
    return md5;
  }

  public void setMd5(String md5) {
    this.md5 = md5;
  }

  public boolean isRootLocaleExcluded() {
    return rootLocaleExcluded;
  }

  public void setRootLocaleExcluded(boolean rootLocaleExcluded) {
    this.rootLocaleExcluded = rootLocaleExcluded;
  }

  public boolean isForRootLocale() {
    return forRootLocale;
  }

  public void setForRootLocale(boolean forRootLocale) {
    this.forRootLocale = forRootLocale;
  }

  public Boolean getToBeFullyTranslatedFilter() {
    return toBeFullyTranslatedFilter;
  }

  public void setToBeFullyTranslatedFilter(Boolean toBeFullyTranslatedFilter) {
    this.toBeFullyTranslatedFilter = toBeFullyTranslatedFilter;
  }

  public StatusFilter getStatusFilter() {
    return statusFilter;
  }

  public void setStatusFilter(StatusFilter statusFilter) {
    this.statusFilter = statusFilter;
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

  public List<String> getRepositoryNames() {
    return repositoryNames;
  }

  public void setRepositoryNames(List<String> repositoryNames) {
    this.repositoryNames = repositoryNames;
  }

  public boolean isPluralFormsFiltered() {
    return pluralFormsFiltered;
  }

  public void setPluralFormsFiltered(boolean pluralFormsFiltered) {
    this.pluralFormsFiltered = pluralFormsFiltered;
  }

  public Long getPluralFormId() {
    return pluralFormId;
  }

  public void setPluralFormId(Long pluralFormId) {
    this.pluralFormId = pluralFormId;
  }

  public boolean isPluralFormsExcluded() {
    return pluralFormsExcluded;
  }

  public void setPluralFormsExcluded(boolean pluralFormsExcluded) {
    this.pluralFormsExcluded = pluralFormsExcluded;
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

  public List<Long> getTmTextUnitIds() {
    return tmTextUnitIds;
  }

  public void setTmTextUnitIds(List<Long> tmTextUnitIds) {
    this.tmTextUnitIds = tmTextUnitIds;
  }

  public void setTmTextUnitIds(Long... tmTextUnitIds) {
    List<Long> filtered =
        Arrays.stream(tmTextUnitIds).filter(Objects::nonNull).collect(Collectors.toList());

    if (!filtered.isEmpty()) {
      this.tmTextUnitIds = filtered;
    }
  }

  public String getSkipTextUnitWithPattern() {
    return skipTextUnitWithPattern;
  }

  public void setSkipTextUnitWithPattern(String skipTextUnitWithPattern) {
    this.skipTextUnitWithPattern = skipTextUnitWithPattern;
  }

  public String getIncludeTextUnitsWithPattern() {
    return includeTextUnitsWithPattern;
  }

  public void setIncludeTextUnitsWithPattern(String includeTextUnitsWithPattern) {
    this.includeTextUnitsWithPattern = includeTextUnitsWithPattern;
  }

  public String getSkipAssetPathWithPattern() {
    return skipAssetPathWithPattern;
  }

  public void setSkipAssetPathWithPattern(String skipAssetPathWithPattern) {
    this.skipAssetPathWithPattern = skipAssetPathWithPattern;
  }

  public boolean isOrderedByTextUnitID() {
    return isOrderedByTextUnitID;
  }

  public void setOrderByTextUnitID(boolean ordered) {
    isOrderedByTextUnitID = ordered;
  }

  public boolean isExcludeUnexpiredPendingMT() {
    return isExcludeUnexpiredPendingMT;
  }

  public void setExcludeUnexpiredPendingMT(boolean excludeUnexpiredPendingMT) {
    isExcludeUnexpiredPendingMT = excludeUnexpiredPendingMT;
  }

  public Duration getAiTranslationExpiryDuration() {
    return aiTranslationExpiryDuration;
  }

  public void setAiTranslationExpiryDuration(Duration aiTranslationExpiryDuration) {
    this.aiTranslationExpiryDuration = aiTranslationExpiryDuration;
  }

  public boolean shouldRetrieveUploadedFileUri() {
    return shouldRetrieveUploadedFileUri;
  }

  public void setIsRetrieveUploadedFileUri(boolean retrieveUploadedFileUri) {
    this.shouldRetrieveUploadedFileUri = retrieveUploadedFileUri;
  }

  public static class Builder {
    private String name;
    private String source;
    private String target;
    private String assetPath;
    private String pluralFormOther;
    private String locationUsage;
    private SearchType searchType;
    private List<Long> repositoryIds;
    private List<String> repositoryNames;
    private List<Long> tmTextUnitIds;
    private List<String> localeTags;
    private Long localeId;
    private UsedFilter usedFilter;
    private StatusFilter statusFilter;
    private Integer offset;
    private Integer limit;
    private Long assetId;
    private Long tmId;
    private String md5;
    private boolean forRootLocale;
    private boolean rootLocaleExcluded = true;
    private Boolean toBeFullyTranslatedFilter;
    private boolean pluralFormsFiltered = true;
    private boolean pluralFormsExcluded;
    private boolean isOrderedByTextUnitID;
    private Long pluralFormId;
    private Boolean doNotTranslateFilter;
    private ZonedDateTime tmTextUnitCreatedBefore;
    private ZonedDateTime tmTextUnitCreatedAfter;
    private Long branchId;
    private String skipTextUnitWithPattern;
    private String includeTextUnitsWithPattern;
    private String skipAssetPathWithPattern;
    private boolean isExcludeUnexpiredPendingMT;
    private Duration aiTranslationExpiryDuration;
    private boolean shouldRetrieveUploadedFileUri;

    public TextUnitSearcherParameters build() {
      TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
      textUnitSearcherParameters.name = this.name;
      textUnitSearcherParameters.source = this.source;
      textUnitSearcherParameters.target = this.target;
      textUnitSearcherParameters.assetPath = this.assetPath;
      textUnitSearcherParameters.pluralFormOther = this.pluralFormOther;
      textUnitSearcherParameters.locationUsage = this.locationUsage;
      textUnitSearcherParameters.searchType = this.searchType;
      textUnitSearcherParameters.repositoryIds = this.repositoryIds;
      textUnitSearcherParameters.repositoryNames = this.repositoryNames;
      textUnitSearcherParameters.tmTextUnitIds = this.tmTextUnitIds;
      textUnitSearcherParameters.localeTags = this.localeTags;
      textUnitSearcherParameters.localeId = this.localeId;
      textUnitSearcherParameters.usedFilter = this.usedFilter;
      textUnitSearcherParameters.statusFilter = this.statusFilter;
      textUnitSearcherParameters.offset = this.offset;
      textUnitSearcherParameters.limit = this.limit;
      textUnitSearcherParameters.assetId = this.assetId;
      textUnitSearcherParameters.tmId = this.tmId;
      textUnitSearcherParameters.md5 = this.md5;
      textUnitSearcherParameters.forRootLocale = this.forRootLocale;
      textUnitSearcherParameters.rootLocaleExcluded = this.rootLocaleExcluded;
      textUnitSearcherParameters.toBeFullyTranslatedFilter = this.toBeFullyTranslatedFilter;
      textUnitSearcherParameters.pluralFormsFiltered = this.pluralFormsFiltered;
      textUnitSearcherParameters.pluralFormsExcluded = this.pluralFormsExcluded;
      textUnitSearcherParameters.isOrderedByTextUnitID = this.isOrderedByTextUnitID;
      textUnitSearcherParameters.pluralFormId = this.pluralFormId;
      textUnitSearcherParameters.doNotTranslateFilter = this.doNotTranslateFilter;
      textUnitSearcherParameters.tmTextUnitCreatedBefore = this.tmTextUnitCreatedBefore;
      textUnitSearcherParameters.tmTextUnitCreatedAfter = this.tmTextUnitCreatedAfter;
      textUnitSearcherParameters.branchId = this.branchId;
      textUnitSearcherParameters.skipTextUnitWithPattern = this.skipTextUnitWithPattern;
      textUnitSearcherParameters.includeTextUnitsWithPattern = this.includeTextUnitsWithPattern;
      textUnitSearcherParameters.skipAssetPathWithPattern = this.skipAssetPathWithPattern;
      textUnitSearcherParameters.isExcludeUnexpiredPendingMT = this.isExcludeUnexpiredPendingMT;
      textUnitSearcherParameters.aiTranslationExpiryDuration = this.aiTranslationExpiryDuration;
      textUnitSearcherParameters.shouldRetrieveUploadedFileUri = this.shouldRetrieveUploadedFileUri;
      return textUnitSearcherParameters;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder source(String source) {
      this.source = NormalizationUtils.normalize(source);
      return this;
    }

    public Builder target(String target) {
      this.target = NormalizationUtils.normalize(target);
      return this;
    }

    public Builder assetPath(String assetPath) {
      this.assetPath = assetPath;
      return this;
    }

    public Builder pluralFormOther(String pluralFormOther) {
      this.pluralFormOther = pluralFormOther;
      return this;
    }

    public Builder locationUsage(String locationUsage) {
      this.locationUsage = locationUsage;
      return this;
    }

    public Builder searchType(SearchType searchType) {
      this.searchType = searchType;
      return this;
    }

    public Builder repositoryId(Long repositoryId) {
      this.repositoryIds = Collections.singletonList(repositoryId);
      return this;
    }

    public Builder repositoryIds(List<Long> repositoryIds) {
      this.repositoryIds = repositoryIds;
      return this;
    }

    public Builder repositoryNames(List<String> repositoryNames) {
      this.repositoryNames = repositoryNames;
      return this;
    }

    public Builder tmTextUnitIds(List<Long> tmTextUnitIds) {
      this.tmTextUnitIds = tmTextUnitIds;
      return this;
    }

    public Builder tmTextUnitIds(Long... tmTextUnitIds) {
      List<Long> filtered =
          Arrays.stream(tmTextUnitIds).filter(Objects::nonNull).collect(Collectors.toList());

      if (!filtered.isEmpty()) {
        this.tmTextUnitIds = filtered;
      }
      return this;
    }

    public Builder localeTags(List<String> localeTags) {
      this.localeTags = localeTags;
      return this;
    }

    public Builder localeId(Long localeId) {
      this.localeId = localeId;
      return this;
    }

    public Builder usedFilter(UsedFilter usedFilter) {
      this.usedFilter = usedFilter;
      return this;
    }

    public Builder statusFilter(StatusFilter statusFilter) {
      this.statusFilter = statusFilter;
      return this;
    }

    public Builder offset(Integer offset) {
      this.offset = offset;
      return this;
    }

    public Builder limit(Integer limit) {
      this.limit = limit;
      return this;
    }

    public Builder assetId(Long assetId) {
      this.assetId = assetId;
      return this;
    }

    public Builder tmId(Long tmId) {
      this.tmId = tmId;
      return this;
    }

    public Builder md5(String md5) {
      this.md5 = md5;
      return this;
    }

    public Builder forRootLocale(boolean forRootLocale) {
      this.forRootLocale = forRootLocale;
      return this;
    }

    public Builder rootLocaleExcluded(boolean rootLocaleExcluded) {
      this.rootLocaleExcluded = rootLocaleExcluded;
      return this;
    }

    public Builder toBeFullyTranslatedFilter(Boolean toBeFullyTranslatedFilter) {
      this.toBeFullyTranslatedFilter = toBeFullyTranslatedFilter;
      return this;
    }

    public Builder pluralFormsFiltered(boolean pluralFormsFiltered) {
      this.pluralFormsFiltered = pluralFormsFiltered;
      return this;
    }

    public Builder pluralFormsExcluded(boolean pluralFormsExcluded) {
      this.pluralFormsExcluded = pluralFormsExcluded;
      return this;
    }

    public Builder isOrderedByTextUnitID(boolean isOrderedByTextUnitID) {
      this.isOrderedByTextUnitID = isOrderedByTextUnitID;
      return this;
    }

    public Builder pluralFormId(Long pluralFormId) {
      this.pluralFormId = pluralFormId;
      return this;
    }

    public Builder doNotTranslateFilter(Boolean doNotTranslateFilter) {
      this.doNotTranslateFilter = doNotTranslateFilter;
      return this;
    }

    public Builder tmTextUnitCreatedBefore(ZonedDateTime tmTextUnitCreatedBefore) {
      this.tmTextUnitCreatedBefore = tmTextUnitCreatedBefore;
      return this;
    }

    public Builder tmTextUnitCreatedAfter(ZonedDateTime tmTextUnitCreatedAfter) {
      this.tmTextUnitCreatedAfter = tmTextUnitCreatedAfter;
      return this;
    }

    public Builder branchId(Long branchId) {
      this.branchId = branchId;
      return this;
    }

    public Builder skipTextUnitWithPattern(String skipTextUnitWithPattern) {
      this.skipTextUnitWithPattern = skipTextUnitWithPattern;
      return this;
    }

    public Builder includeTextUnitsWithPattern(String includeTextUnitsWithPattern) {
      this.includeTextUnitsWithPattern = includeTextUnitsWithPattern;
      return this;
    }

    public Builder skipAssetPathWithPattern(String skipAssetPathWithPattern) {
      this.skipAssetPathWithPattern = skipAssetPathWithPattern;
      return this;
    }

    public Builder isExcludeUnexpiredPendingMT(boolean isExcludeUnexpiredPendingMT) {
      this.isExcludeUnexpiredPendingMT = isExcludeUnexpiredPendingMT;
      return this;
    }

    public Builder aiTranslationExpiryDuration(Duration aiTranslationExpiryDuration) {
      this.aiTranslationExpiryDuration = aiTranslationExpiryDuration;
      return this;
    }

    public Builder shouldRetrieveUploadedFileUri(boolean shouldRetrieveUploadedFileUri) {
      this.shouldRetrieveUploadedFileUri = shouldRetrieveUploadedFileUri;
      return this;
    }
  }
}
