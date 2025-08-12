package com.box.l10n.mojito.rest.resttemplate.stateless;

import com.box.l10n.mojito.rest.resttemplate.ResttemplateConfig;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import java.net.MalformedURLException;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "l10n.resttemplate.stateless",
    name = "provider",
    havingValue = "MSAL_CLIENT_CREDENTIALS")
/** Supplies access tokens using MSAL Client Credentials flow (application permissions). */
public class MsalClientCredentialsTokenSupplier implements TokenSupplier {

  static Logger logger = LoggerFactory.getLogger(MsalClientCredentialsTokenSupplier.class);

  final ConfidentialClientApplication cca;
  final Set<String> scopes;

  public MsalClientCredentialsTokenSupplier(ResttemplateConfig config)
      throws MalformedURLException {
    this.scopes = config.getStateless().getMsal().getScopes();

    String authority = config.getStateless().getMsal().getAuthority();
    String clientId = config.getStateless().getMsal().getClientId();
    String clientSecret = config.getStateless().getMsal().getClientSecret();
    if (authority == null || clientId == null || clientSecret == null) {
      throw new IllegalStateException(
          "Missing MSAL authority/clientId/clientSecret for client-credentials in restclient config");
    }
    if (clientSecret.trim().matches("(?i)^[0-9a-f-]{36}$")) {
      throw new IllegalStateException(
          "Provided clientSecret looks like a Secret ID (GUID). Use the secret VALUE instead (copied at creation time).");
    }

    this.cca =
        ConfidentialClientApplication.builder(
                clientId, ClientCredentialFactory.createFromSecret(clientSecret))
            .authority(authority)
            .build();
  }

  @Override
  public String getAccessToken() {
    try {
      if (scopes.isEmpty()) {
        throw new IllegalStateException(
            "MSAL client-credentials requires scopes (resource/.default)");
      }
      ClientCredentialParameters params = ClientCredentialParameters.builder(scopes).build();
      IAuthenticationResult result = cca.acquireToken(params).get();
      return result.accessToken();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException("Failed to acquire token via client credentials flow", e);
    }
  }
}
