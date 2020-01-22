package com.box.l10n.mojito.service.tm.search;

import com.box.l10n.mojito.service.NormalizationUtils;
import org.joda.time.DateTime;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    String pluralFormOther;
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
    boolean untranslatedOrTranslationNeeded = false;
    boolean pluralFormsFiltered = true;
    boolean pluralFormsExcluded = false;
    Long pluralFormId;
    Boolean doNotTranslateFilter;
    DateTime tmTextUnitCreatedBefore;
    DateTime tmTextUnitCreatedAfter;
    Long branchId;

    public String getName() {
        return name;
    }

    public TextUnitSearcherParameters setName(String name) {
        this.name = name;
        return this;
    }

    public String getSource() {
        return source;
    }

    public TextUnitSearcherParameters setSource(String source) {
        this.source = NormalizationUtils.normalize(source);
        return this;
    }

    public SearchType getSearchType() {
        return searchType;
    }

    public TextUnitSearcherParameters setSearchType(SearchType searchType) {
        this.searchType = searchType;
        return this;
    }

    public String getTarget() {
        return target;
    }

    public TextUnitSearcherParameters setTarget(String target) {
        this.target = NormalizationUtils.normalize(target);
        return this;
    }

    public String getAssetPath() {
        return assetPath;
    }

    public TextUnitSearcherParameters setAssetPath(String assetPath) {
        this.assetPath = assetPath;
        return this;
    }

    public List<Long> getRepositoryIds() {
        return repositoryIds;
    }

    public TextUnitSearcherParameters setRepositoryIds(Long repositoryId) {
        this.repositoryIds = Arrays.asList(repositoryId);
        return this;
    }

    public TextUnitSearcherParameters setRepositoryIds(List<Long> repositoryIds) {
        this.repositoryIds = repositoryIds;
        return this;
    }

    public List<String> getLocaleTags() {
        return localeTags;
    }

    public TextUnitSearcherParameters setLocaleTags(List<String> localeTags) {
        this.localeTags = localeTags;
        return this;
    }

    public Long getLocaleId() {
        return localeId;
    }

    public TextUnitSearcherParameters setLocaleId(Long localeId) {
        this.localeId = localeId;
        return this;
    }

    public UsedFilter getUsedFilter() {
        return usedFilter;
    }

    public TextUnitSearcherParameters setUsedFilter(UsedFilter usedFilter) {
        this.usedFilter = usedFilter;
        return this;
    }

    public Integer getLimit() {
        return limit;
    }

    public TextUnitSearcherParameters setLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    public Integer getOffset() {
        return offset;
    }

    public TextUnitSearcherParameters setOffset(Integer offset) {
        this.offset = offset;
        return this;
    }

    public Long getAssetId() {
        return assetId;
    }

    public TextUnitSearcherParameters setAssetId(Long assetId) {
        this.assetId = assetId;
        return this;
    }

    public Long getTmId() {
        return tmId;
    }

    public TextUnitSearcherParameters setTmId(Long tmId) {
        this.tmId = tmId;
        return this;
    }

    public String getMd5() {
        return md5;
    }

    public TextUnitSearcherParameters setMd5(String md5) {
        this.md5 = md5;
        return this;
    }

    public boolean isRootLocaleExcluded() {
        return rootLocaleExcluded;
    }

    public TextUnitSearcherParameters setRootLocaleExcluded(boolean rootLocaleExcluded) {
        this.rootLocaleExcluded = rootLocaleExcluded;
        return this;
    }

    public boolean isForRootLocale() {
        return forRootLocale;
    }

    public TextUnitSearcherParameters setForRootLocale(boolean forRootLocale) {
        this.forRootLocale = forRootLocale;
        return this;
    }

    public Boolean getToBeFullyTranslatedFilter() {
        return toBeFullyTranslatedFilter;
    }

    public TextUnitSearcherParameters setToBeFullyTranslatedFilter(Boolean toBeFullyTranslatedFilter) {
        this.toBeFullyTranslatedFilter = toBeFullyTranslatedFilter;
        return this;
    }

    public StatusFilter getStatusFilter() {
        return statusFilter;
    }

    public TextUnitSearcherParameters setStatusFilter(StatusFilter statusFilter) {
        this.statusFilter = statusFilter;
        return this;
    }

    public String getPluralFormOther() {
        return pluralFormOther;
    }

    public TextUnitSearcherParameters setPluralFormOther(String pluralFormOther) {
        this.pluralFormOther = pluralFormOther;
        return this;
    }

    public List<String> getRepositoryNames() {
        return repositoryNames;
    }

    public TextUnitSearcherParameters setRepositoryNames(List<String> repositoryNames) {
        this.repositoryNames = repositoryNames;
        return this;
    }

    public boolean isPluralFormsFiltered() {
        return pluralFormsFiltered;
    }

    public TextUnitSearcherParameters setPluralFormsFiltered(boolean pluralFormsFiltered) {
        this.pluralFormsFiltered = pluralFormsFiltered;
        return this;
    }

    public Long getPluralFormId() {
        return pluralFormId;
    }

    public TextUnitSearcherParameters setPluralFormId(Long pluralFormId) {
        this.pluralFormId = pluralFormId;
        return this;
    }

    public boolean isPluralFormsExcluded() {
        return pluralFormsExcluded;
    }

    public TextUnitSearcherParameters setPluralFormsExcluded(boolean pluralFormsExcluded) {
        this.pluralFormsExcluded = pluralFormsExcluded;
        return this;
    }

    public Boolean getDoNotTranslateFilter() {
        return doNotTranslateFilter;
    }

    public TextUnitSearcherParameters setDoNotTranslateFilter(Boolean doNotTranslateFilter) {
        this.doNotTranslateFilter = doNotTranslateFilter;
        return this;
    }

    public DateTime getTmTextUnitCreatedBefore() {
        return tmTextUnitCreatedBefore;
    }

    public TextUnitSearcherParameters setTmTextUnitCreatedBefore(DateTime tmTextUnitCreatedBefore) {
        this.tmTextUnitCreatedBefore = tmTextUnitCreatedBefore;
        return this;
    }

    public DateTime getTmTextUnitCreatedAfter() {
        return tmTextUnitCreatedAfter;
    }

    public TextUnitSearcherParameters setTmTextUnitCreatedAfter(DateTime tmTextUnitCreatedAfter) {
        this.tmTextUnitCreatedAfter = tmTextUnitCreatedAfter;
        return this;
    }

    public Long getBranchId() {
        return branchId;
    }

    public TextUnitSearcherParameters setBranchId(Long branchId) {
        this.branchId = branchId;
        return this;
    }

    public List<Long> getTmTextUnitIds() {
        return tmTextUnitIds;
    }

    public TextUnitSearcherParameters setTmTextUnitIds(List<Long> tmTextUnitIds) {
        this.tmTextUnitIds = tmTextUnitIds;
        return this;
    }

    public TextUnitSearcherParameters setTmTextUnitIds(Long... tmTextUnitIds) {
        List<Long> filtered = Arrays.stream(tmTextUnitIds).filter(Objects::nonNull).collect(Collectors.toList());

        if (!filtered.isEmpty()) {
            this.tmTextUnitIds = filtered;
        }

        return this;
    }
}
