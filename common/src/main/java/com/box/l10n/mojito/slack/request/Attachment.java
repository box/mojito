package com.box.l10n.mojito.slack.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class Attachment {

  public static final String MRKDWNIN_TEXT = "text";
  public static final String MRKDOWNIN_PRETEXT = "pretex";
  public static final String MRKDOWNIN_FILEDS = "fields";

  String title;

  String text;

  String fallback;

  @JsonProperty("mrkdwn_in")
  List<String> mrkdwnIn = new ArrayList<>();

  String color;

  List<Action> actions = new ArrayList<>();

  List<Field> fields = new ArrayList<>();

  public Attachment() {
    super();
  }

  public Attachment(String title, String text) {
    this.title = title;
    this.text = text;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getFallback() {
    return fallback;
  }

  public void setFallback(String fallback) {
    this.fallback = fallback;
  }

  public List<String> getMrkdwnIn() {
    return mrkdwnIn;
  }

  public void setMrkdwnIn(List<String> mrkdwnIn) {
    this.mrkdwnIn = mrkdwnIn;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public List<Action> getActions() {
    return actions;
  }

  public void setActions(List<Action> actions) {
    this.actions = actions;
  }

  public List<Field> getFields() {
    return fields;
  }

  public void setFields(List<Field> fields) {
    this.fields = fields;
  }
}
