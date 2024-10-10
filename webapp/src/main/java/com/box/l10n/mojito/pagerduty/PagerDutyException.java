package com.box.l10n.mojito.pagerduty;

public class PagerDutyException extends Exception {
  private int statusCode = 0;

  public PagerDutyException(int statusCode, String responseBody) {
    super(
        "PagerDuty request failed: Status Code: '"
            + statusCode
            + "', Response Body: '"
            + responseBody
            + "'");
    this.statusCode = statusCode;
  }

  public PagerDutyException(String message) {
    super(message);
  }

  public PagerDutyException(String message, Throwable cause) {
    super(message, cause);
  }

  public int getStatusCode() {
    return statusCode;
  }
}
