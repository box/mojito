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

// TODO(ja) do we need both vs mutating review_project_text_unit?

@Entity
@Table(name = "review_project_accepted_variant")
public class ReviewProjectAcceptedVariant extends BaseEntity {

  // TODO(ja) de-normalizing here? is it needed? i mean we have the relation via
  // review_project_text_unit_id
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "review_project_id",
      foreignKey = @ForeignKey(name = "FK__REVIEW_PROJECT_ACCEPTED_VARIANT__PROJECT"))
  private ReviewProject reviewProject;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "review_project_text_unit_id",
      foreignKey =
          @ForeignKey(name = "FK__REVIEW_PROJECT_ACCEPTED_VARIANT__REVIEW_PROJECT_TEXT_UNIT"))
  private ReviewProjectTextUnit reviewProjectTextUnit;

  // TODO(ja) I guess that is a duplicate of the other table info. anyway i also had the question
  // mark do we need 2 tables or not
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "tm_text_unit_variant_id",
      foreignKey = @ForeignKey(name = "FK__REVIEW_PROJECT_ACCEPTED_VARIANT__VARIANT"))
  private TMTextUnitVariant tmTextUnitVariant;

  // TODO(ja) i proposed better names for that I think
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "accepted_variant_id",
      foreignKey = @ForeignKey(name = "FK__REVIEW_PROJECT_ACCEPTED_VARIANT__ACCEPTED_VARIANT"))
  private TMTextUnitVariant acceptedVariant;

  // TODO(ja) yes, might be missing in the other table
  @Column(name = "accepted_at")
  private ZonedDateTime acceptedAt;

  // TODO(ja) yes, might be missing in the other table
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "accepted_by_user_id",
      foreignKey = @ForeignKey(name = "FK__REVIEW_PROJECT_ACCEPTED_VARIANT__USER"))
  private User acceptedBy;

  // TODO(ja) better just store the current and compare the values --- and be carefule of stale
  // data.
  @Column(name = "is_current")
  private Boolean current;

  public ReviewProject getReviewProject() {
    return reviewProject;
  }

  public void setReviewProject(ReviewProject reviewProject) {
    this.reviewProject = reviewProject;
  }

  public ReviewProjectTextUnit getReviewProjectTextUnit() {
    return reviewProjectTextUnit;
  }

  public void setReviewProjectTextUnit(ReviewProjectTextUnit reviewProjectTextUnit) {
    this.reviewProjectTextUnit = reviewProjectTextUnit;
  }

  public TMTextUnitVariant getTmTextUnitVariant() {
    return tmTextUnitVariant;
  }

  public void setTmTextUnitVariant(TMTextUnitVariant tmTextUnitVariant) {
    this.tmTextUnitVariant = tmTextUnitVariant;
  }

  public TMTextUnitVariant getAcceptedVariant() {
    return acceptedVariant;
  }

  public void setAcceptedVariant(TMTextUnitVariant acceptedVariant) {
    this.acceptedVariant = acceptedVariant;
  }

  public ZonedDateTime getAcceptedAt() {
    return acceptedAt;
  }

  public void setAcceptedAt(ZonedDateTime acceptedAt) {
    this.acceptedAt = acceptedAt;
  }

  public User getAcceptedBy() {
    return acceptedBy;
  }

  public void setAcceptedBy(User acceptedBy) {
    this.acceptedBy = acceptedBy;
  }

  public Boolean getCurrent() {
    return current;
  }

  public void setCurrent(Boolean current) {
    this.current = current;
  }
}
