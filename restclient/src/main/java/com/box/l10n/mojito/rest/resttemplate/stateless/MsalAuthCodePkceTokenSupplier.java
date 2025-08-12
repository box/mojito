package com.box.l10n.mojito.rest.resttemplate.stateless;

import static com.microsoft.aad.msal4j.SilentParameters.*;

import com.box.l10n.mojito.rest.resttemplate.ResttemplateConfig;
import com.microsoft.aad.msal4j.IAccount;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.InteractiveRequestParameters;
import com.microsoft.aad.msal4j.PublicClientApplication;
import java.io.IOException;
import java.net.URI;
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
    havingValue = "MSAL_BROWSER_CODE")
public class MsalAuthCodePkceTokenSupplier implements TokenSupplier {

  static Logger logger = LoggerFactory.getLogger(MsalAuthCodePkceTokenSupplier.class);

  final PublicClientApplication pca;
  final Set<String> scopes;

  public MsalAuthCodePkceTokenSupplier(ResttemplateConfig config) throws IOException {
    this.pca = MsalSupport.buildPublicClient(config);
    this.scopes = config.getStateless().getMsal().getScopes();
  }

  @Override
  public String getAccessToken() {
    try {
      logger.debug("Attempt silent from cache");
      for (IAccount account : pca.getAccounts().join()) {
        try {
          IAuthenticationResult res =
              pca.acquireTokenSilently(builder(scopes, account).build()).get();
          if (res != null && res.accessToken() != null) {
            return res.accessToken();
          }
        } catch (Exception e) {
          logger.debug("Fall back to interactive", e);
        }
      }

      logger.debug("Interactive with system browser and loopback redirect to localhost");
      URI redirectUri = URI.create("http://localhost");
      InteractiveRequestParameters params =
          InteractiveRequestParameters.builder(redirectUri).scopes(scopes).build();
      IAuthenticationResult result = pca.acquireToken(params).get();
      return result.accessToken();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException("Failed to acquire token via interactive browser flow", e);
    }
  }
}
