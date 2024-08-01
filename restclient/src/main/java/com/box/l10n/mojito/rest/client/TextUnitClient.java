package com.box.l10n.mojito.rest.client;

import com.box.l10n.mojito.rest.entity.PollableTask;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TextUnitClient extends BaseClient {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(TextUnitClient.class);

  @Override
  public String getEntityName() {
    return "textunits";
  }

  public List<TextUnit> searchTextUnits(TextUnitSearchBody textUnitSearchBody) {
    TextUnit[] search =
        authenticatedRestTemplate.postForObject(
            getBasePathForEntity() + "/search", textUnitSearchBody, TextUnit[].class);
    return Arrays.asList(search);
  }

  public PollableTask importTextUnitBatch(ImportTextUnitsBatch importTextUnitsBatch) {
    return authenticatedRestTemplate.postForObject(
        getBasePath() + "/textunitsBatch", importTextUnitsBatch, PollableTask.class);
  }

  public record ImportTextUnitsBatch(
      boolean integrityCheckSkipped,
      boolean integrityCheckKeepStatusIfFailedAndSameTarget,
      List<TextUnit> textUnits) {}

  public enum UsedFilter {
    USED,
    UNUSED
  }

  public enum SearchType {
    EXACT,
    CONTAINS,
    ILIKE
  }

  public enum StatusFilter {
    ALL,
    TRANSLATED,
    UNTRANSLATED,
    TRANSLATED_AND_NOT_REJECTED,
    APPROVED_OR_NEEDS_REVIEW_AND_NOT_REJECTED,
    APPROVED_AND_NOT_REJECTED,
    FOR_TRANSLATION,
    REVIEW_NEEDED,
    REVIEW_NOT_NEEDED,
    TRANSLATION_NEEDED,
    REJECTED,
    NOT_REJECTED,
  }

  public record TextUnit(
      @JsonProperty("tmTextUnitId") Long tmTextUnitId,
      @JsonProperty("tmTextUnitVariantId") Long tmTextUnitVariantId,
      @JsonProperty("localeId") Long localeId,
      @JsonProperty("name") String name,
      @JsonProperty("source") String source,
      @JsonProperty("comment") String comment,
      @JsonProperty("target") String target,
      @JsonProperty("targetLocale") String targetLocale,
      @JsonProperty("targetComment") String targetComment,
      @JsonProperty("assetId") Long assetId,
      @JsonProperty("lastSuccessfulAssetExtractionId") Long lastSuccessfulAssetExtractionId,
      @JsonProperty("assetExtractionId") Long assetExtractionId,
      @JsonProperty("tmTextUnitCurrentVariantId") Long tmTextUnitCurrentVariantId,
      @JsonProperty("status") Status status,
      @JsonProperty("includedInLocalizedFile") Boolean includedInLocalizedFile,
      @JsonProperty("createdDate") Long createdDate,
      @JsonProperty("assetDeleted") Boolean assetDeleted,
      @JsonProperty("pluralForm") String pluralForm,
      @JsonProperty("pluralFormOther") String pluralFormOther,
      @JsonProperty("repositoryName") String repositoryName,
      @JsonProperty("assetPath") String assetPath,
      @JsonProperty("assetTextUnitId") Long assetTextUnitId,
      @JsonProperty("tmTextUnitCreatedDate") Long tmTextUnitCreatedDate,
      @JsonProperty("doNotTranslate") Boolean doNotTranslate,
      @JsonProperty("translated") Boolean translated,
      @JsonProperty("used") Boolean used) {

    public TextUnit withTarget(String target, Status status) {
      return new TextUnit(
          tmTextUnitId,
          tmTextUnitVariantId,
          localeId,
          name,
          source,
          comment,
          target,
          targetLocale,
          targetComment,
          assetId,
          lastSuccessfulAssetExtractionId,
          assetExtractionId,
          tmTextUnitCurrentVariantId,
          status,
          includedInLocalizedFile,
          createdDate,
          assetDeleted,
          pluralForm,
          pluralFormOther,
          repositoryName,
          assetPath,
          assetTextUnitId,
          tmTextUnitCreatedDate,
          doNotTranslate,
          translated,
          used);
    }
  }

  public enum Status {
    TRANSLATION_NEEDED,
    REVIEW_NEEDED,
    APPROVED
  }

  public static class TextUnitSearchBody {
    List<Long> repositoryIds;
    List<String> repositoryNames;
    List<Long> tmTextUnitIds;
    String name;
    String source;
    String target;
    String assetPath;
    String pluralFormOther;
    boolean pluralFormFiltered = true;
    boolean pluralFormExcluded = false;
    SearchType searchType = SearchType.EXACT;
    List<String> localeTags;
    UsedFilter usedFilter;
    StatusFilter statusFilter;
    Boolean doNotTranslateFilter;
    ZonedDateTime tmTextUnitCreatedBefore;
    ZonedDateTime tmTextUnitCreatedAfter;
    Long branchId;
    Integer limit = 10;
    Integer offset = 0;

    public List<Long> getRepositoryIds() {
      return repositoryIds;
    }

    public void setRepositoryIds(List<Long> repositoryIds) {
      this.repositoryIds = repositoryIds;
    }

    public List<String> getRepositoryNames() {
      return repositoryNames;
    }

    public void setRepositoryNames(List<String> repositoryNames) {
      this.repositoryNames = repositoryNames;
    }

    public List<Long> getTmTextUnitIds() {
      return tmTextUnitIds;
    }

    public void setTmTextUnitIds(List<Long> tmTextUnitIds) {
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

    public List<String> getLocaleTags() {
      return localeTags;
    }

    public void setLocaleTags(List<String> localeTags) {
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
}
