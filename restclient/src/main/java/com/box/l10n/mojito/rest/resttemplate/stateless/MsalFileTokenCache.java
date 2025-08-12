package com.box.l10n.mojito.rest.resttemplate.stateless;

import com.microsoft.aad.msal4j.ITokenCacheAccessAspect;
import com.microsoft.aad.msal4j.ITokenCacheAccessContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Simple file-backed MSAL token cache to persist tokens between runs. */
class MsalFileTokenCache implements ITokenCacheAccessAspect {

  static Logger logger = LoggerFactory.getLogger(MsalFileTokenCache.class);

  private final Path cacheFile;

  MsalFileTokenCache(Path cacheFile) {
    this.cacheFile = cacheFile;
  }

  @Override
  public void beforeCacheAccess(ITokenCacheAccessContext context) {
    try {
      if (Files.exists(cacheFile)) {
        String data = Files.readString(cacheFile, StandardCharsets.UTF_8);
        context.tokenCache().deserialize(data);
      }
    } catch (IOException e) {
      logger.warn("Can't read cache file, ignore", e);
    }
  }

  @Override
  public void afterCacheAccess(ITokenCacheAccessContext context) {
    if (context.hasCacheChanged()) {
      try {
        String data = context.tokenCache().serialize();
        Files.writeString(cacheFile, data, StandardCharsets.UTF_8);
      } catch (IOException e) {
        logger.warn("Can't write cache file, ignore", e);
      }
    }
  }
}
