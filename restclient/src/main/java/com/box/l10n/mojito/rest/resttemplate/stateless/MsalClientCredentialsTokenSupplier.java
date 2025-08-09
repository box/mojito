package com.box.l10n.mojito.rest.resttemplate.stateless;

import com.box.l10n.mojito.rest.resttemplate.ResttemplateConfig;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Supplies access tokens using MSAL Client Credentials flow (application permissions).
 */
public class MsalClientCredentialsTokenSupplier implements TokenSupplier {

  static Logger logger = LoggerFactory.getLogger(MsalClientCredentialsTokenSupplier.class);

  final ResttemplateConfig config;
  volatile ConfidentialClientApplication cca;

  public MsalClientCredentialsTokenSupplier(ResttemplateConfig config) {
    this.config = config;
  }

  @Override
  public String getAccessToken() {
    try {
      ensureCca();
      Set<String> scopes = toDefaultScopes(parseScopes(config.getStateless().getMsal().getScopes()));
      if (scopes.isEmpty()) {
        throw new IllegalStateException("MSAL client-credentials requires scopes (resource/.default)");
      }
      ClientCredentialParameters params = ClientCredentialParameters.builder(scopes).build();
      IAuthenticationResult result = cca.acquireToken(params).get();
      return result.accessToken();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException("Failed to acquire token via client credentials flow", e);
    }
  }

  void ensureCca() {
    if (cca == null) {
      synchronized (this) {
        if (cca == null) {
          try {
            String authority = config.getStateless().getMsal().getAuthority();
            String clientId = config.getStateless().getMsal().getConfidentialClientId();
            if (clientId == null) {
              clientId = config.getStateless().getMsal().getClientId(); // fallback for backward compat
            }
            String clientSecret = config.getStateless().getMsal().getClientSecret();
            if (authority == null || clientId == null || clientSecret == null) {
              throw new IllegalStateException(
                  "Missing MSAL authority/clientId/clientSecret for client-credentials in restclient config");
            }
            // Guard against likely misconfiguration: secret ID used instead of secret VALUE
            if (clientSecret.trim().matches("(?i)^[0-9a-f-]{36}$")) {
              throw new IllegalStateException(
                  "Provided clientSecret looks like a Secret ID (GUID). Use the secret VALUE instead (copied at creation time)."
              );
            }
            cca =
                ConfidentialClientApplication.builder(
                        clientId, ClientCredentialFactory.createFromSecret(clientSecret))
                    .authority(authority)
                    .build();
          } catch (Exception e) {
            throw new RuntimeException("Failed to initialize MSAL ConfidentialClientApplication", e);
          }
        }
      }
    }
  }

  Set<String> parseScopes(String scopesString) {
    if (scopesString == null || scopesString.isBlank()) return Collections.emptySet();
    String[] s = scopesString.split("[\\s,]+");
    return new HashSet<>(Arrays.asList(s));
  }

  /**
   * Transform any "api://resource/scope" into "api://resource/.default" suitable for application permissions.
   */
  Set<String> toDefaultScopes(Set<String> scopes) {
    if (scopes == null || scopes.isEmpty()) return scopes;
    Set<String> res = new HashSet<>();
    for (String scope : scopes) {
      if (scope == null || scope.isBlank()) continue;
      String s = scope;
      int idx = s.indexOf("/");
      if (s.startsWith("api://") && idx > 0) {
        // keep scheme + host part (api://<aud>), drop remainder and use .default
        int secondSlash = s.indexOf('/', "api://".length());
        if (secondSlash > 0) {
          s = s.substring(0, secondSlash) + "/.default";
        }
      }
      res.add(s);
    }
    return res;
  }
}
