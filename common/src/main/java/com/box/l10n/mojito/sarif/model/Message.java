package com.box.l10n.mojito.sarif.model;

public class Message {
  private String text;

  public Message(String text) {
    this.text = text;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }
}
