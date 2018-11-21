package com.box.l10n.mojito.service.tm.search;

import com.box.l10n.mojito.service.NormalizationUtils;
import java.util.Arrays;
import java.util.List;
import org.joda.time.DateTime;

/**
 * Parameters for {@link TextUnitSearcher#search(com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters)
 * }
 *
 * @author jaurambault
 */
public class TextUnitSearcherParameters {

    String name;
    String source;
    String target;
    String assetPath;
    String pullRequestId;
    String authorName;
    String pluralFormOther;
    SearchType searchType;
    List<Long> repositoryIds;
    List<String> repositoryNames;
    List<String> localeTags;
    Long localeId;
    UsedFilter usedFilter;
    StatusFilter statusFilter;
    Integer offset;
    Integer limit;
    Long assetId;
    Long tmTextUnitId;
    Long tmId;
    String md5;
    boolean forRootLocale = false;
    boolean rootLocaleExcluded = true;
    Boolean toBeFullyTranslatedFilter;
    boolean untranslatedOrTranslationNeeded = false;
    boolean pluralFormsFiltered = true;
    Long pluralFormId;
    Boolean doNotTranslateFilter;
    DateTime tmTextUnitCreatedBefore;
    DateTime tmTextUnitCreatedAfter;

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

    public String getPullRequestId() {
        return pullRequestId;
    }

    public void setPullRequestId(String pullRequestId) {
        this.pullRequestId = pullRequestId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
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

    public Long getTmTextUnitId() {
        return tmTextUnitId;
    }

    public void setTmTextUnitId(Long tmTextUnitId) {
        this.tmTextUnitId = tmTextUnitId;
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

    public Boolean getDoNotTranslateFilter() {
        return doNotTranslateFilter;
    }

    public void setDoNotTranslateFilter(Boolean doNotTranslateFilter) {
        this.doNotTranslateFilter = doNotTranslateFilter;
    }

    public DateTime getTmTextUnitCreatedBefore() {
        return tmTextUnitCreatedBefore;
    }

    public void setTmTextUnitCreatedBefore(DateTime tmTextUnitCreatedBefore) {
        this.tmTextUnitCreatedBefore = tmTextUnitCreatedBefore;
    }

    public DateTime getTmTextUnitCreatedAfter() {
        return tmTextUnitCreatedAfter;
    }

    public void setTmTextUnitCreatedAfter(DateTime tmTextUnitCreatedAfter) {
        this.tmTextUnitCreatedAfter = tmTextUnitCreatedAfter;
    }
}
