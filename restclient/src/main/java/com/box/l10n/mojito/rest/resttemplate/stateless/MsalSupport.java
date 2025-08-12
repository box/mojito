package com.box.l10n.mojito.rest.resttemplate.stateless;

import com.box.l10n.mojito.rest.resttemplate.ResttemplateConfig;
import com.microsoft.aad.msal4j.PublicClientApplication;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class MsalSupport {

  public static PublicClientApplication buildPublicClient(ResttemplateConfig config)
      throws IOException {
    String authority = config.getStateless().getMsal().getAuthority();
    String clientId = config.getStateless().getMsal().getClientId();
    if (authority == null || clientId == null) {
      throw new IllegalStateException(
          "Missing MSAL authority/clientId in restclient stateless configuration");
    }

    Path cachePath = Path.of(config.getStateless().getCachePath());
    Files.createDirectories(cachePath.getParent());
    if (Files.notExists(cachePath)) {
      Files.createFile(cachePath);
    }

    MsalFileTokenCache msalFileTokenCache = new MsalFileTokenCache(cachePath);

    return PublicClientApplication.builder(clientId)
        .authority(authority)
        .setTokenCacheAccessAspect(msalFileTokenCache)
        .build();
  }
}
