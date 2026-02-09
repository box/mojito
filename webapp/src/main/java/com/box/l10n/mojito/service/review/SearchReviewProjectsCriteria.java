package com.box.l10n.mojito.service.review;

import com.box.l10n.mojito.entity.review.ReviewProjectStatus;
import com.box.l10n.mojito.entity.review.ReviewProjectType;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Service-layer search parameters for review projects (kept independent from REST request types).
 */
public record SearchReviewProjectsCriteria(
    List<ReviewProjectStatus> statuses,
    List<ReviewProjectType> types,
    List<String> localeTags,
    ZonedDateTime createdAfter,
    ZonedDateTime createdBefore,
    ZonedDateTime dueAfter,
    ZonedDateTime dueBefore,
    Integer limit,
    String searchQuery,
    SearchField searchField,
    SearchMatchType searchMatchType) {

  public static final int DEFAULT_LIMIT = 500;
  public static final int MAX_LIMIT = 10_000;

  public SearchReviewProjectsCriteria {
    if (limit == null || limit <= 0) {
      limit = DEFAULT_LIMIT;
    } else if (limit > MAX_LIMIT) {
      throw new IllegalArgumentException("limit must be <= " + MAX_LIMIT);
    }

    if (searchQuery != null) {
      searchQuery = searchQuery.trim();
      if (searchQuery.isEmpty()) {
        searchQuery = null;
      }
    }

    if (searchField == null) {
      searchField = SearchField.NAME;
    }

    if (searchMatchType == null) {
      searchMatchType = SearchMatchType.CONTAINS;
    }
  }

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
