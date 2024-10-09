package com.box.l10n.mojito.rest.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;

public class UserLocale {
  @JsonBackReference private User user;

  private Locale locale;

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Locale getLocale() {
    return locale;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
  }
}
