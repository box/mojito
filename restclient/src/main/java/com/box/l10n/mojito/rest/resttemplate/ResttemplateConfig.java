package com.box.l10n.mojito.rest.resttemplate;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the rest template
 *
 * @author jaurambault
 */
@Component
@ConfigurationProperties(prefix = "l10n.resttemplate")
public class ResttemplateConfig {

  String host = "localhost";
  Integer port = 8080;
  String scheme = "http";
  String contextPath = "";
  boolean usesLoginAuthentication = true;

  Authentication authentication = new Authentication();

  public static class Authentication {

    String username = "admin";
    String password = "ChangeMe";

    CredentialProvider credentialProvider = CredentialProvider.CONFIG;

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public CredentialProvider getCredentialProvider() {
      return credentialProvider;
    }

    public void setCredentialProvider(CredentialProvider credentialProvider) {
      this.credentialProvider = credentialProvider;
    }

    public enum CredentialProvider {
      CONSOLE,
      CONFIG
    }
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public String getScheme() {
    return scheme;
  }

  public void setScheme(String scheme) {
    this.scheme = scheme;
  }

  public Authentication getAuthentication() {
    return authentication;
  }

  public void setAuthentication(Authentication authentication) {
    this.authentication = authentication;
  }

  public String getContextPath() {
    return contextPath;
  }

  public void setContextPath(String contextPath) {
    this.contextPath = contextPath;
  }

  public boolean usesLoginAuthentication() {
    return usesLoginAuthentication;
  }

  public void setUsesLoginAuthentication(boolean usesLoginAuthentication) {
    this.usesLoginAuthentication = usesLoginAuthentication;
  }
}
