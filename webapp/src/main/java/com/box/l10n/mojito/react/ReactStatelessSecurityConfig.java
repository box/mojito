package com.box.l10n.mojito.react;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Minimal config for exposing MSAL (Azure AD) JWT auth settings to the frontend.
 *
 * <p>Properties prefix: l10n.security.stateless
 *
 * <p>Notes on authority vs issuer-uri: - Backend resource server validation typically uses:
 * spring.security.oauth2.resourceserver.jwt.issuer-uri=https://login.microsoftonline.com/<tenantId>/v2.0
 * - MSAL authority for the SPA should NOT include "/v2.0". Use the base tenant authority:
 * l10n.security.stateless.msal.authority=https://login.microsoftonline.com/<tenantId> MSAL will
 * internally target the v2.0 authorize/token endpoints.
 *
 * <p>Example configuration: l10n.security.stateless=true
 * l10n.security.stateless.msal.authority=https://login.microsoftonline.com/<tenantId>
 * l10n.security.stateless.msal.client-id=<frontend-app-client-id>
 * l10n.security.stateless.msal.scope=api://<appId>/scope.read
 */
@Component
@ConfigurationProperties(prefix = "l10n.security.stateless")
public class ReactStatelessSecurityConfig {

  private boolean enabled;

  private Msal msal = new Msal();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Msal getMsal() {
    return msal;
  }

  public void setMsal(Msal msal) {
    this.msal = msal;
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
}
