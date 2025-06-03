package com.box.l10n.mojito.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Use run_name to manage multiple reviews of the same text units, it is denormalized but just want
 * something very simple for now. The review is stored a JSON blob defined in the {@link
 * com.box.l10n.mojito.service.oaireview.AiReviewService.AiReviewSingleTextUnitOutput} but that
 * format could change any time.
 */
@Entity
@Table(
    name = "ai_review_proto",
    indexes = {
      @Index(
          name = "UK__AI_REVIEW_PROTO__RUN_NAME__TM_TEXT_UNIT_VARIANT_ID",
          columnList = "run_name, tm_text_unit_variant_id",
          unique = true)
    })
public class AiReviewProto extends AuditableEntity {

  @Column(name = "run_name", length = 32)
  String runName;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "tm_text_unit_variant_id",
      foreignKey = @ForeignKey(name = "FK__AI_REVIEW_PROTO__TM_TEXT_UNIT_VARIANT__ID"))
  TMTextUnitVariant tmTextUnitVariant;

  @Column(name = "json_review", length = Integer.MAX_VALUE)
  String jsonReview;

  public String getRunName() {
    return runName;
  }

  public void setRunName(String runName) {
    this.runName = runName;
  }

  public TMTextUnitVariant getTmTextUnitVariant() {
    return tmTextUnitVariant;
  }

  public void setTmTextUnitVariant(TMTextUnitVariant tmTextUnitVariant) {
    this.tmTextUnitVariant = tmTextUnitVariant;
  }

  public String getJsonReview() {
    return jsonReview;
  }

  public void setJsonReview(String jsonReview) {
    this.jsonReview = jsonReview;
  }
}
