package com.box.l10n.mojito.smartling.response;

import java.util.Map;

public class Error {
  private String key;
  private String message;
  private Map<String, String> details;

  public void setKey(String key) {
    this.key = key;
  }

  public String getKey() {
    return this.key;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getMessage() {
    return this.message;
  }

  public void setDetails(Map<String, String> details) {
    this.details = details;
  }

  public Map<String, String> getDetails() {
    return this.details;
  }
}
