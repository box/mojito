package com.box.l10n.mojito.slack.response;

public class BaseResponse {
  Boolean ok;
  String error;

  public Boolean getOk() {
    return ok;
  }

  public void setOk(Boolean ok) {
    this.ok = ok;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }
}
