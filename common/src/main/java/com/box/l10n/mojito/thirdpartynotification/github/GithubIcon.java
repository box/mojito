package com.box.l10n.mojito.thirdpartynotification.github;

public enum GithubIcon {
  INFO(":information_source:"),
  WARNING(":warning:"),
  STOP(":stop_sign:");

  String str;

  GithubIcon(String str) {
    this.str = str;
  }

  public String getStr() {
    return str;
  }

  @Override
  public String toString() {
    return str;
  }
}
