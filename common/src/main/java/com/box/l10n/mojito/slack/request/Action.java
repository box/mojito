package com.box.l10n.mojito.slack.request;

public class Action {

  public static final String ACTION_TYPE_BUTTON = "button";
  public static final String ACTION_STYLE_PRIMARY = "primary";

  String type;
  String text;
  String url;
  String style;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getStyle() {
    return style;
  }

  public void setStyle(String style) {
    this.style = style;
  }
}
