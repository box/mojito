package com.box.l10n.mojito.rest.review;

import com.box.l10n.mojito.entity.review.ReviewProjectStatus;
import com.box.l10n.mojito.entity.review.ReviewProjectType;
import java.time.ZonedDateTime;
import java.util.List;

public record SearchReviewProjectsRequest(
    List<String> localeTags,
    List<ReviewProjectStatus> statuses,
    List<ReviewProjectType> types,
    ZonedDateTime createdAfter,
    ZonedDateTime createdBefore,
    ZonedDateTime dueAfter,
    ZonedDateTime dueBefore,
    Integer limit,
    String searchQuery,
    SearchField searchField,
    SearchMatchType searchMatchType) {

  public enum SearchField {
    NAME,
    ID,
    REQUEST_ID
  }

  public enum SearchMatchType {
    CONTAINS,
    EXACT,
    ILIKE
  }
}
