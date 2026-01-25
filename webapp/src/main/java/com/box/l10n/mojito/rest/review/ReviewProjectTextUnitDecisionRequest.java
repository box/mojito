package com.box.l10n.mojito.rest.review;

import com.box.l10n.mojito.entity.review.ReviewProjectTextUnitDecision.DecisionState;

public class ReviewProjectTextUnitDecisionRequest {

  private String target;
  private String comment;
  private String status;
  private Boolean includedInLocalizedFile;
  private Long expectedCurrentTmTextUnitVariantId;
  private Boolean overrideChangedCurrent;
  private String decisionNotes;
  private DecisionState decisionState;

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Boolean getIncludedInLocalizedFile() {
    return includedInLocalizedFile;
  }

  public void setIncludedInLocalizedFile(Boolean includedInLocalizedFile) {
    this.includedInLocalizedFile = includedInLocalizedFile;
  }

  public Long getExpectedCurrentTmTextUnitVariantId() {
    return expectedCurrentTmTextUnitVariantId;
  }

  public void setExpectedCurrentTmTextUnitVariantId(Long expectedCurrentTmTextUnitVariantId) {
    this.expectedCurrentTmTextUnitVariantId = expectedCurrentTmTextUnitVariantId;
  }

  public Boolean getOverrideChangedCurrent() {
    return overrideChangedCurrent;
  }

  public void setOverrideChangedCurrent(Boolean overrideChangedCurrent) {
    this.overrideChangedCurrent = overrideChangedCurrent;
  }

  public String getDecisionNotes() {
    return decisionNotes;
  }

  public void setDecisionNotes(String decisionNotes) {
    this.decisionNotes = decisionNotes;
  }

  public DecisionState getDecisionState() {
    return decisionState;
  }

  public void setDecisionState(DecisionState decisionState) {
    this.decisionState = decisionState;
  }
}
