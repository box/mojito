package com.box.l10n.mojito.rest.review;

import com.box.l10n.mojito.entity.review.ReviewDecisionStatus;

public class ReviewProjectTextUnitReviewRequest {

  private ReviewDecisionStatus reviewStatus;
  private String reviewTarget;
  private String reviewNotes;

  public ReviewDecisionStatus getReviewStatus() {
    return reviewStatus;
  }

  public void setReviewStatus(ReviewDecisionStatus reviewStatus) {
    this.reviewStatus = reviewStatus;
  }

  public String getReviewTarget() {
    return reviewTarget;
  }

  public void setReviewTarget(String reviewTarget) {
    this.reviewTarget = reviewTarget;
  }

  public String getReviewNotes() {
    return reviewNotes;
  }

  public void setReviewNotes(String reviewNotes) {
    this.reviewNotes = reviewNotes;
  }
}
