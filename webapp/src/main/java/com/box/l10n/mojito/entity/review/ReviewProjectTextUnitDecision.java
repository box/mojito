package com.box.l10n.mojito.entity.review;

import com.box.l10n.mojito.entity.BaseEntity;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.security.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
      name = "decided_variant_id",
      foreignKey =
          @ForeignKey(
              name = "FK__REVIEW_PROJECT_TEXT_UNIT_DECISION__DECIDED_VARIANT"))
  private TMTextUnitVariant decidedVariant;

  @Enumerated(EnumType.STRING)
  @Column(name = "decision_status")
  private ReviewDecisionStatus decisionStatus = ReviewDecisionStatus.PENDING;

  @Column(name = "review_notes", length = 4000)
  private String reviewNotes;

  @Column(name = "decided_at")
  private ZonedDateTime decidedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "decided_by_user_id",
      foreignKey = @ForeignKey(name = "FK__REVIEW_PROJECT_TEXT_UNIT_DECISION__USER"))
  private User decidedBy;

  public ReviewProjectTextUnit getReviewProjectTextUnit() {
    return reviewProjectTextUnit;
  }

  public void setReviewProjectTextUnit(ReviewProjectTextUnit reviewProjectTextUnit) {
    this.reviewProjectTextUnit = reviewProjectTextUnit;
  }

  public TMTextUnitVariant getDecidedVariant() {
    return decidedVariant;
  }

  public void setDecidedVariant(TMTextUnitVariant decidedVariant) {
    this.decidedVariant = decidedVariant;
  }

  public ReviewDecisionStatus getDecisionStatus() {
    return decisionStatus;
  }

  public void setDecisionStatus(ReviewDecisionStatus decisionStatus) {
    this.decisionStatus = decisionStatus;
  }

  public String getReviewNotes() {
    return reviewNotes;
  }

  public void setReviewNotes(String reviewNotes) {
    this.reviewNotes = reviewNotes;
  }

  public ZonedDateTime getDecidedAt() {
    return decidedAt;
  }

  public void setDecidedAt(ZonedDateTime decidedAt) {
    this.decidedAt = decidedAt;
  }

  public User getDecidedBy() {
    return decidedBy;
  }

  public void setDecidedBy(User decidedBy) {
    this.decidedBy = decidedBy;
  }
}
