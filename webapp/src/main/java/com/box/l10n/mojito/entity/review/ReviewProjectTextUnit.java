package com.box.l10n.mojito.entity.review;

import com.box.l10n.mojito.entity.SettableAuditableEntity;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "review_project_text_unit")
public class ReviewProjectTextUnit extends SettableAuditableEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "review_project_id",
      foreignKey = @ForeignKey(name = "FK__REVIEW_PROJECT_TEXT_UNIT__PROJECT"))
  private ReviewProject reviewProject;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "tm_text_unit_variant_id",
      foreignKey = @ForeignKey(name = "FK__REVIEW_PROJECT_TEXT_UNIT__VARIANT"))
  private TMTextUnitVariant tmTextUnitVariant;

  // TODO(ja) be more explicit: initialTmTextUnitVariant + acceptedTmTextUnitVariant +
  // currentTmTextUnitVariant (can be stale) + optional: editsTmTextUnitVariant (if the translator
  // do multiple changes we can save it there).
  // I'm wondering if we just update that table, OR if make this immutable content and then in the
  // other table, review_project_accepted_variant, we track mutable content?
  // please advise

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "tm_text_unit_id",
      foreignKey = @ForeignKey(name = "FK__REVIEW_PROJECT_TEXT_UNIT__TM_TEXT_UNIT"))
  private TMTextUnit tmTextUnit;

  @Column(name = "position")
  private Integer position;

  // TODO(ja) what's that?
  @Column(name = "selection_reason", length = 64)
  private String selectionReason;

  // TODO(ja) why do we care, we should keep the initial variant, that will come from there
  @Column(name = "initial_status", length = 32)
  private String initialStatus;

  // TODO(ja) same has will coment from the initial variant
  @Column(name = "initial_variant_hash", length = 32)
  private String initialVariantHash;

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

  public Integer getPosition() {
    return position;
  }

  public void setPosition(Integer position) {
    this.position = position;
  }

  public String getSelectionReason() {
    return selectionReason;
  }

  public void setSelectionReason(String selectionReason) {
    this.selectionReason = selectionReason;
  }

  public String getInitialStatus() {
    return initialStatus;
  }

  public void setInitialStatus(String initialStatus) {
    this.initialStatus = initialStatus;
  }

  public String getInitialVariantHash() {
    return initialVariantHash;
  }

  public void setInitialVariantHash(String initialVariantHash) {
    this.initialVariantHash = initialVariantHash;
  }
}
