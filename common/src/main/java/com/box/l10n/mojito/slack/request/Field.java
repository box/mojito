package com.box.l10n.mojito.slack.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Field {

  String title;

  String value;

  @JsonProperty("short")
  Boolean isShort;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public Boolean getShort() {
    return isShort;
  }

  public void setShort(Boolean aShort) {
    isShort = aShort;
  }
}
