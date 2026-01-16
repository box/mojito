package com.box.l10n.mojito.rest.review;

import com.box.l10n.mojito.entity.review.ReviewProjectStatus;
import com.box.l10n.mojito.entity.review.ReviewProjectType;
import java.time.ZonedDateTime;
import java.util.List;

public class ReviewProjectSearchRequest {

  public enum SearchField {
    NAME,
    ID
  }

  public enum SearchMatchType {
    CONTAINS,
    EXACT,
    ILIKE
  }

  private List<String> localeTags;
  private List<ReviewProjectStatus> statuses;
  private List<ReviewProjectType> types;
  private ZonedDateTime createdAfter;
  private ZonedDateTime createdBefore;
  private ZonedDateTime dueAfter;
  private ZonedDateTime dueBefore;
  private Integer limit;
  private String searchQuery;
  private SearchField searchField = SearchField.NAME;
  private SearchMatchType searchMatchType = SearchMatchType.CONTAINS;

  public List<String> getLocaleTags() {
    return localeTags;
  }

  public void setLocaleTags(List<String> localeTags) {
    this.localeTags = localeTags;
  }

  public List<ReviewProjectStatus> getStatuses() {
    return statuses;
  }

  public void setStatuses(List<ReviewProjectStatus> statuses) {
    this.statuses = statuses;
  }

  public List<ReviewProjectType> getTypes() {
    return types;
  }

  public void setTypes(List<ReviewProjectType> types) {
    this.types = types;
  }

  public ZonedDateTime getCreatedAfter() {
    return createdAfter;
  }

  public void setCreatedAfter(ZonedDateTime createdAfter) {
    this.createdAfter = createdAfter;
  }

  public ZonedDateTime getCreatedBefore() {
    return createdBefore;
  }

  public void setCreatedBefore(ZonedDateTime createdBefore) {
    this.createdBefore = createdBefore;
  }

  public ZonedDateTime getDueAfter() {
    return dueAfter;
  }

  public void setDueAfter(ZonedDateTime dueAfter) {
    this.dueAfter = dueAfter;
  }

  public ZonedDateTime getDueBefore() {
    return dueBefore;
  }

  public void setDueBefore(ZonedDateTime dueBefore) {
    this.dueBefore = dueBefore;
  }

  public Integer getLimit() {
    return limit;
  }

  public void setLimit(Integer limit) {
    this.limit = limit;
  }

  public String getSearchQuery() {
    return searchQuery;
  }

  public void setSearchQuery(String searchQuery) {
    this.searchQuery = searchQuery;
  }

  public SearchField getSearchField() {
    return searchField;
  }

  public void setSearchField(SearchField searchField) {
    this.searchField = searchField;
  }

  public SearchMatchType getSearchMatchType() {
    return searchMatchType;
  }

  public void setSearchMatchType(SearchMatchType searchMatchType) {
    this.searchMatchType = searchMatchType;
  }
}
