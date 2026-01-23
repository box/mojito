package com.box.l10n.mojito.entity.review;

import com.box.l10n.mojito.entity.AuditableEntity;
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
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

@Entity
@Table(name = "review_project_text_unit_decision")
public class ReviewProjectTextUnitDecision extends AuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "review_project_text_unit_id",
      foreignKey =
          @ForeignKey(name = "FK__REVIEW_PROJECT_TEXT_UNIT_DECISION__REVIEW_PROJECT_TEXT_UNIT"))
  private ReviewProjectTextUnit reviewProjectTextUnit;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "variant_id",
      foreignKey = @ForeignKey(name = "FK__REVIEW_PROJECT_TEXT_UNIT_DECISION__VARIANT"))
  private TMTextUnitVariant decisionVariant;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "reviewed_variant_id",
      foreignKey =
          @ForeignKey(name = "FK__REVIEW_PROJECT_TEXT_UNIT_DECISION__REVIEWED_VARIANT"))
  private TMTextUnitVariant reviewedVariant;

  @Column(name = "notes", length = 4000)
  private String notes;

  @CreatedBy
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = BaseEntity.CreatedByUserColumnName,
      foreignKey = @ForeignKey(name = "FK__REVIEW_PROJECT_TEXT_UNIT_DECISION__CREATED_BY_USER"))
  private User createdByUser;

  @LastModifiedBy
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "last_modified_by_user_id",
      foreignKey =
          @ForeignKey(name = "FK__REVIEW_PROJECT_TEXT_UNIT_DECISION__LAST_MODIFIED_BY_USER"))
  private User lastModifiedByUser;

  public ReviewProjectTextUnit getReviewProjectTextUnit() {
    return reviewProjectTextUnit;
  }

  public void setReviewProjectTextUnit(ReviewProjectTextUnit reviewProjectTextUnit) {
    this.reviewProjectTextUnit = reviewProjectTextUnit;
  }

  public TMTextUnitVariant getVariant() {
    return decisionVariant;
  }

  public void setVariant(TMTextUnitVariant variant) {
    this.decisionVariant = variant;
  }

  public TMTextUnitVariant getDecisionVariant() {
    return decisionVariant;
  }

  public void setDecisionVariant(TMTextUnitVariant decisionVariant) {
    this.decisionVariant = decisionVariant;
  }

  public TMTextUnitVariant getAcceptedVariant() {
    return decisionVariant;
  }

  public void setAcceptedVariant(TMTextUnitVariant acceptedVariant) {
    this.decisionVariant = acceptedVariant;
  }

  public TMTextUnitVariant getReviewedVariant() {
    return reviewedVariant;
  }

  public void setReviewedVariant(TMTextUnitVariant reviewedVariant) {
    this.reviewedVariant = reviewedVariant;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public User getCreatedByUser() {
    return createdByUser;
  }

  public void setCreatedByUser(User createdByUser) {
    this.createdByUser = createdByUser;
  }

  public User getLastModifiedByUser() {
    return lastModifiedByUser;
  }

  public void setLastModifiedByUser(User lastModifiedByUser) {
    this.lastModifiedByUser = lastModifiedByUser;
  }
}
