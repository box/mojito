package com.box.l10n.mojito.react;

import com.box.l10n.mojito.rest.security.UserProfile;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class ReactAppConfig {

  @JsonUnwrapped ReactStaticAppConfig reactStaticAppConfig;

  UserProfile user;

  String locale;

  boolean ict;

  String csrfToken;

  String contextPath;

  public ReactAppConfig(ReactStaticAppConfig reactStaticAppConfig, UserProfile userProfile) {
    this.reactStaticAppConfig = reactStaticAppConfig;
    this.user = userProfile;
  }

  public ReactStaticAppConfig getReactStaticAppConfig() {
    return reactStaticAppConfig;
  }

  public UserProfile getUser() {
    return user;
  }

  public void setReactStaticAppConfig(ReactStaticAppConfig reactStaticAppConfig) {
    this.reactStaticAppConfig = reactStaticAppConfig;
  }

  public void setUser(UserProfile user) {
    this.user = user;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  public boolean isIct() {
    return ict;
  }

  public void setIct(boolean ict) {
    this.ict = ict;
  }

  public String getCsrfToken() {
    return csrfToken;
  }

  public void setCsrfToken(String csrfToken) {
    this.csrfToken = csrfToken;
  }

  public String getContextPath() {
    return contextPath;
  }

  public void setContextPath(String contextPath) {
    this.contextPath = contextPath;
  }
}
