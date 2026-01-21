package com.box.l10n.mojito.rest.review;

public class ReviewProjectTextUnitReviewRequest {

  private String reviewStatus;
  private String reviewTarget;
  private String reviewNotes;

  public String getReviewStatus() {
    return reviewStatus;
  }

  public void setReviewStatus(String reviewStatus) {
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
