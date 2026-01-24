package com.box.l10n.mojito.rest.review;

public class ReviewProjectTextUnitDecisionStateRequest {

  private String decisionState;
  private Long expectedCurrentTmTextUnitVariantId;
  private Boolean overrideChangedCurrent;

  public String getDecisionState() {
    return decisionState;
  }

  public void setDecisionState(String decisionState) {
    this.decisionState = decisionState;
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
}
