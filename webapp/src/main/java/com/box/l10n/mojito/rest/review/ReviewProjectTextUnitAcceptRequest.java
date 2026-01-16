package com.box.l10n.mojito.rest.review;

public class ReviewProjectTextUnitAcceptRequest {

  private String target;
  private Boolean includedInLocalizedFile;
  private Long expectedCurrentTmTextUnitVariantId;
  private Boolean overrideChangedCurrent;

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
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
}
