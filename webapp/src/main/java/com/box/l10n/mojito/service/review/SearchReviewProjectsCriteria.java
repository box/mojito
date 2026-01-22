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
    String searchQuery) {

  public static final int DEFAULT_LIMIT = 500;
  public static final int MAX_LIMIT = 10_000;

  public SearchReviewProjectsCriteria {
    if (limit == null || limit <= 0) {
      limit = DEFAULT_LIMIT;
    } else if (limit > MAX_LIMIT) {
      throw new IllegalArgumentException("limit must be <= " + MAX_LIMIT);
    }
  }
}
