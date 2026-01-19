package com.box.l10n.mojito.entity.review;

/**
 * Reviewer-side decision state for a review project text unit. Separates review progress from TM
 * statuses.
 */
public enum ReviewDecisionStatus {
  PENDING,
  VIEWED,
  ACCEPTED_AS_IS,
  ACCEPTED_WITH_CHANGE,
  REJECTED,
  SKIPPED
}
