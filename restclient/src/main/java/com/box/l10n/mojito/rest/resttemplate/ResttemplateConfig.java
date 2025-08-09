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

  Authentication authentication = new Authentication();
  StatelessAuthentication stateless = new StatelessAuthentication();

  AuthenticationMode authenticationMode = AuthenticationMode.STATEFUL;

  public static class Authentication {

    String username = "admin";
    String password = "ChangeMe";

    CredentialProvider credentialProvider = CredentialProvider.CONFIG;

    // v0 stateless switch also supported here for backward-compatible property name:
    // l10n.resttemplate.authentication.mode=STATELESS
    AuthenticationMode mode = AuthenticationMode.STATEFUL;

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

    public AuthenticationMode getMode() {
      return mode;
    }

    public void setMode(AuthenticationMode mode) {
      this.mode = mode;
    }

    public enum CredentialProvider {
      CONSOLE,
      CONFIG
    }
  }

  public enum AuthenticationMode {
    STATEFUL,
    STATELESS
  }

  public static class StatelessAuthentication {

    Provider provider = Provider.MSAL_DEVICE_CODE;

    Msal msal = new Msal();

    String cachePath = System.getProperty("user.home") + "/.mojito/msal-token-cache.json";

    public Provider getProvider() {
      return provider;
    }

    public void setProvider(Provider provider) {
      this.provider = provider;
    }

    public Msal getMsal() {
      return msal;
    }

    public void setMsal(Msal msal) {
      this.msal = msal;
    }

    public String getCachePath() {
      return cachePath;
    }

    public void setCachePath(String cachePath) {
      this.cachePath = cachePath;
    }

    public static class Msal {
      String authority;
      String clientId;
      String scopes; // space- or comma-separated

      public String getAuthority() {
        return authority;
      }

      public void setAuthority(String authority) {
        this.authority = authority;
      }

      public String getClientId() {
        return clientId;
      }

      public void setClientId(String clientId) {
        this.clientId = clientId;
      }

      public String getScopes() {
        return scopes;
      }

      public void setScopes(String scopes) {
        this.scopes = scopes;
      }
    }

    public enum Provider {
      MSAL_DEVICE_CODE,
      MSAL_BROWSER_CODE
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

  public StatelessAuthentication getStateless() {
    return stateless;
  }

  public void setStateless(StatelessAuthentication stateless) {
    this.stateless = stateless;
  }

  public AuthenticationMode getAuthenticationMode() {
    return authenticationMode;
  }

  public void setAuthenticationMode(AuthenticationMode authenticationMode) {
    this.authenticationMode = authenticationMode;
  }

  public String getContextPath() {
    return contextPath;
  }

  public void setContextPath(String contextPath) {
    this.contextPath = contextPath;
  }
}
