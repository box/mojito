package com.box.l10n.mojito.android.strings;

public abstract class AbstractAndroidString {

  private String comment;
  private String name;

  public AbstractAndroidString(String name, String comment) {
    this.comment = comment;
    this.name = name;
  }

  public abstract boolean isSingular();

  public boolean isPlural() {
    return !isSingular();
  }

  public String getComment() {
    return comment;
  }

  public String getName() {
    return name;
  }
}
