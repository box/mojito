package com.box.l10n.mojito.entity.review;

import com.box.l10n.mojito.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "review_project_screenshot")
public class ReviewProjectRequestScreenshot extends AuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "review_project_request_id",
      foreignKey = @ForeignKey(name = "FK__REVIEW_PROJECT_SCREENSHOT__REQUEST"))
  private ReviewProjectRequest reviewProjectRequest;

  @Column(name = "image_name", length = 255, nullable = false)
  private String imageName;

  public ReviewProjectRequest getReviewProjectRequest() {
    return reviewProjectRequest;
  }

  public void setReviewProjectRequest(ReviewProjectRequest reviewProjectRequest) {
    this.reviewProjectRequest = reviewProjectRequest;
  }

  public String getImageName() {
    return imageName;
  }

  public void setImageName(String imageKey) {
    this.imageName = imageKey;
  }
}
