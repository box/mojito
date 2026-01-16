package com.box.l10n.mojito.entity.review;

import com.box.l10n.mojito.entity.AuditableEntity;
import com.box.l10n.mojito.entity.Locale;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "review_project_screenshot")
public class ReviewProjectScreenshot extends AuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "review_project_request_id",
      foreignKey = @ForeignKey(name = "FK__REVIEW_PROJECT_SCREENSHOT__REQUEST"))
  private ReviewProjectRequest reviewProjectRequest;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "review_project_id",
      foreignKey = @ForeignKey(name = "FK__REVIEW_PROJECT_SCREENSHOT__PROJECT"))
  private ReviewProject reviewProject;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "locale_id",
      foreignKey = @ForeignKey(name = "FK__REVIEW_PROJECT_SCREENSHOT__LOCALE"))
  private Locale locale;

  @Column(name = "image_key", length = 255, nullable = false)
  private String imageKey;

  public ReviewProjectRequest getReviewProjectRequest() {
    return reviewProjectRequest;
  }

  public void setReviewProjectRequest(ReviewProjectRequest reviewProjectRequest) {
    this.reviewProjectRequest = reviewProjectRequest;
  }

  public ReviewProject getReviewProject() {
    return reviewProject;
  }

  public void setReviewProject(ReviewProject reviewProject) {
    this.reviewProject = reviewProject;
  }

  public Locale getLocale() {
    return locale;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  public String getImageKey() {
    return imageKey;
  }

  public void setImageKey(String imageKey) {
    this.imageKey = imageKey;
  }
}
