package com.box.l10n.mojito.rest.textunit;

/**
 * @author wyau
 */
public class TMTextUnitIntegrityCheckResult {

  private Boolean checkResult;

  private String failureDetail;

  /**
   * @return True if the check passes. False if it fails.
   */
  public Boolean getCheckResult() {
    return checkResult;
  }

  /**
   * True if check passes. False if check fails.
   *
   * @param checkResult
   */
  public void setCheckResult(Boolean checkResult) {
    this.checkResult = checkResult;
  }

  /**
   * @return Detail description of the result
   */
  public String getFailureDetail() {
    return failureDetail;
  }

  /**
   * @param failureDetail
   */
  public void setFailureDetail(String failureDetail) {
    this.failureDetail = failureDetail;
  }
}
