package com.box.l10n.mojito.entity.review;

import com.box.l10n.mojito.entity.BaseEntity;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.security.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;

@Entity
@Table(name = "review_project_text_unit_decision")
public class ReviewProjectTextUnitDecision extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "review_project_text_unit_id",
      foreignKey =
          @ForeignKey(
              name = "FK__REVIEW_PROJECT_TEXT_UNIT_DECISION__REVIEW_PROJECT_TEXT_UNIT"))
  private ReviewProjectTextUnit reviewProjectTextUnit;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "variant_id",
      foreignKey =
          @ForeignKey(
              name = "FK__REVIEW_PROJECT_TEXT_UNIT_DECISION__VARIANT"))
  private TMTextUnitVariant variant;

  @Column(name = "notes", length = 4000)
  private String notes;

  @Column(name = "recorded_at")
  private ZonedDateTime recordedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "recorded_by_user_id",
      foreignKey = @ForeignKey(name = "FK__REVIEW_PROJECT_TEXT_UNIT_DECISION__USER"))
  private User recordedBy;

  public ReviewProjectTextUnit getReviewProjectTextUnit() {
    return reviewProjectTextUnit;
  }

  public void setReviewProjectTextUnit(ReviewProjectTextUnit reviewProjectTextUnit) {
    this.reviewProjectTextUnit = reviewProjectTextUnit;
  }

  public TMTextUnitVariant getVariant() {
    return variant;
  }

  public void setVariant(TMTextUnitVariant variant) {
    this.variant = variant;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public ZonedDateTime getRecordedAt() {
    return recordedAt;
  }

  public void setRecordedAt(ZonedDateTime recordedAt) {
    this.recordedAt = recordedAt;
  }

  public User getRecordedBy() {
    return recordedBy;
  }

  public void setRecordedBy(User recordedBy) {
    this.recordedBy = recordedBy;
  }
}
