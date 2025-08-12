package com.box.l10n.mojito.rest.resttemplate.stateless;

import com.box.l10n.mojito.rest.resttemplate.ResttemplateConfig;
import com.microsoft.aad.msal4j.DeviceCode;
import com.microsoft.aad.msal4j.DeviceCodeFlowParameters;
import com.microsoft.aad.msal4j.IAccount;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.PublicClientApplication;
import com.microsoft.aad.msal4j.SilentParameters;
import java.io.IOException;
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
    havingValue = "MSAL_DEVICE_CODE")
public class MsalDeviceCodeTokenSupplier implements TokenSupplier {

  static Logger logger = LoggerFactory.getLogger(MsalDeviceCodeTokenSupplier.class);

  final PublicClientApplication pca;
  final Set<String> scopes;

  public MsalDeviceCodeTokenSupplier(ResttemplateConfig config) throws IOException {
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
              pca.acquireTokenSilently(SilentParameters.builder(scopes, account).build()).get();
          if (res != null && res.accessToken() != null) {
            return res.accessToken();
          }
        } catch (Exception ignored) {
          logger.debug("Fall back to device code");
        }
      }

      logger.debug("Interactive device code");
      DeviceCodeFlowParameters parameters =
          DeviceCodeFlowParameters.builder(
                  scopes,
                  (DeviceCode deviceCode) -> {
                    // This should eventually use the CLI's console writer
                    System.out.println(deviceCode.message());
                  })
              .build();
      IAuthenticationResult result = pca.acquireToken(parameters).get();
      return result.accessToken();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException("Failed to acquire token via device code flow", e);
    }
  }
}
