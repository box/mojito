package com.box.l10n.mojito.entity.review;

import com.box.l10n.mojito.entity.SettableAuditableEntity;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "review_project_text_unit",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "UK__REVIEW_PROJECT_TEXT_UNIT__PROJECT_VARIANT",
          columnNames = {"review_project_id", "tm_text_unit_variant_id"})
    })
public class ReviewProjectTextUnit extends SettableAuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "review_project_id",
      foreignKey = @ForeignKey(name = "FK__REVIEW_PROJECT_TEXT_UNIT__PROJECT"))
  private ReviewProject reviewProject;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "tm_text_unit_variant_id",
      foreignKey = @ForeignKey(name = "FK__REVIEW_PROJECT_TEXT_UNIT__VARIANT"))
  private TMTextUnitVariant tmTextUnitVariant;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "tm_text_unit_id",
      foreignKey = @ForeignKey(name = "FK__REVIEW_PROJECT_TEXT_UNIT__TM_TEXT_UNIT"))
  private TMTextUnit tmTextUnit;

  public ReviewProject getReviewProject() {
    return reviewProject;
  }

  public void setReviewProject(ReviewProject reviewProject) {
    this.reviewProject = reviewProject;
  }

  public TMTextUnitVariant getTmTextUnitVariant() {
    return tmTextUnitVariant;
  }

  public void setTmTextUnitVariant(TMTextUnitVariant tmTextUnitVariant) {
    this.tmTextUnitVariant = tmTextUnitVariant;
  }

  public TMTextUnit getTmTextUnit() {
    return tmTextUnit;
  }

  public void setTmTextUnit(TMTextUnit tmTextUnit) {
    this.tmTextUnit = tmTextUnit;
  }
}
