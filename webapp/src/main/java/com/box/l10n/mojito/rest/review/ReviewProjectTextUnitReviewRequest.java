package com.box.l10n.mojito.rest.review;

public class ReviewProjectTextUnitReviewRequest {

  private String reviewStatus;
  private String reviewTarget;
  private String notes;

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

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }
}
