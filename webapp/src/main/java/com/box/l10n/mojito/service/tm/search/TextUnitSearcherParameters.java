package com.box.l10n.mojito.service.tm.search;

import com.box.l10n.mojito.service.NormalizationUtils;
import java.util.Arrays;
import java.util.List;

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
    SearchType searchType;
    List<Long> repositoryIds;
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
    boolean rootLocaleExcluded = true;
    boolean untranslatedOrTranslationNeeded = false;

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

    public String getTarget() {
        return target;
    }

    public SearchType getSearchType() {
        return searchType;
    }

    public void setSearchType(SearchType searchType) {
        this.searchType = searchType;
    }

    public void setTarget(String target) {
        this.target = NormalizationUtils.normalize(target);
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

    public StatusFilter getStatusFilter() {
        return statusFilter;
    }

    public void setStatusFilter(StatusFilter statusFilter) {
        this.statusFilter = statusFilter;
    }

}
