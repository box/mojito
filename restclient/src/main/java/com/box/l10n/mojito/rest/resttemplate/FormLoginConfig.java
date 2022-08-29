package com.box.l10n.mojito.rest.resttemplate;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration related to form login authentication
 *
 * @author jaurambault
 */
@Component
@ConfigurationProperties(prefix = "l10n.resttemplate.authentication.formlogin")
public class FormLoginConfig {

  String loginFormPath = "login";

  String loginPostPath = "login";

  String loginRedirectPath = "";

  String csrfTokenPath = "api/csrf-token";

  public String getLoginFormPath() {
    return loginFormPath;
  }

  public void setLoginFormPath(String loginFormPath) {
    this.loginFormPath = loginFormPath;
  }

  public String getLoginPostPath() {
    return loginPostPath;
  }

  public void setLoginPostPath(String loginPostPath) {
    this.loginPostPath = loginPostPath;
  }

  public String getLoginRedirectPath() {
    return loginRedirectPath;
  }

  public void setLoginRedirectPath(String loginRedirectPath) {
    this.loginRedirectPath = loginRedirectPath;
  }

  public String getCsrfTokenPath() {
    return csrfTokenPath;
  }

  public void setCsrfTokenPath(String csrfTokenPath) {
    this.csrfTokenPath = csrfTokenPath;
  }
}
