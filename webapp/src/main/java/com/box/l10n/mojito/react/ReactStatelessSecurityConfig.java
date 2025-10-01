package com.box.l10n.mojito.react;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Minimal config for exposing stateless auth settings to the frontend.
 *
 * <p>Properties prefix: l10n.security.stateless
 *
 * <p>Notes on authority vs issuer-uri: - Backend resource server validation typically uses:
 * spring.security.oauth2.resourceserver.jwt.issuer-uri=https://login.microsoftonline.com/<tenantId>/v2.0
 * - MSAL authority for the SPA should NOT include "/v2.0". Use the base tenant authority:
 * l10n.security.stateless.msal.authority=https://login.microsoftonline.com/<tenantId> MSAL will
 * internally target the v2.0 authorize/token endpoints.
 *
 * <p>Example configuration: l10n.security.stateless.enabled=true l10n.security.stateless.type=MSAL
 * l10n.security.stateless.msal.authority=https://login.microsoftonline.com/<tenantId>
 * l10n.security.stateless.msal.client-id=<frontend-app-client-id>
 * l10n.security.stateless.msal.scope=api://<appId>/scope.read
 *
 * <p>Cloudflare Access example: l10n.security.stateless.enabled=true
 * l10n.security.stateless.type=CLOUDFLARE
 * l10n.security.stateless.cloudflare.local-jwt-assertion=<optional-jwt-for-local-testing>
 */
@Component
@ConfigurationProperties(prefix = "l10n.security.stateless")
public class ReactStatelessSecurityConfig {

  private boolean enabled;

  private AuthType type = AuthType.MSAL;

  private Msal msal = new Msal();

  private Cloudflare cloudflare = new Cloudflare();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public AuthType getType() {
    return type;
  }

  public void setType(AuthType type) {
    this.type = type;
  }

  public Msal getMsal() {
    return msal;
  }

  public void setMsal(Msal msal) {
    this.msal = msal;
  }

  public Cloudflare getCloudflare() {
    return cloudflare;
  }

  public void setCloudflare(Cloudflare cloudflare) {
    this.cloudflare = cloudflare;
  }

  public enum AuthType {
    MSAL,
    CLOUDFLARE
  }

  public static class Msal {
    private String authority;
    private String clientId;
    private String scope;

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

    public String getScope() {
      return scope;
    }

    public void setScope(String scope) {
      this.scope = scope;
    }
  }

  public static class Cloudflare {
    private String localJwtAssertion;

    public String getLocalJwtAssertion() {
      return localJwtAssertion;
    }

    public void setLocalJwtAssertion(String localJwtAssertion) {
      this.localJwtAssertion = localJwtAssertion;
    }
  }
}
