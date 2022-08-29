package com.box.l10n.mojito.rest.entity;

/** @author jaurambault */
public class ErrorMessage {

  String type;
  String message;
  boolean expected;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public boolean isExpected() {
    return expected;
  }

  public void setExpected(boolean expected) {
    this.expected = expected;
  }
}
