package com.box.l10n.mojito.rest.resttemplate.stateless;

import com.box.l10n.mojito.rest.resttemplate.ResttemplateConfig;
import com.microsoft.aad.msal4j.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MsalDeviceCodeTokenSupplier implements TokenSupplier {

  static Logger logger = LoggerFactory.getLogger(MsalDeviceCodeTokenSupplier.class);

  final ResttemplateConfig config;

  volatile PublicClientApplication pca;

  public MsalDeviceCodeTokenSupplier(ResttemplateConfig config) {
    this.config = config;
  }

  @Override
  public String getAccessToken() {
    try {
      ensurePca();
      Set<String> scopes = parseScopes(config.getStateless().getMsal().getScopes());

      // Attempt silent from cache
      for (IAccount account : pca.getAccounts().join()) {
        try {
          IAuthenticationResult res =
              pca.acquireTokenSilently(
                      com.microsoft.aad.msal4j.SilentParameters.builder(scopes, account).build())
                  .get();
          if (res != null && res.accessToken() != null) {
            return res.accessToken();
          }
        } catch (Exception ignored) {
          // fall back to device code
        }
      }

      // Interactive device code
      DeviceCodeFlowParameters parameters =
          DeviceCodeFlowParameters.builder(
                  scopes,
                  (DeviceCode deviceCode) -> {
                    System.out.println(deviceCode.message());
                  })
              .build();
      IAuthenticationResult result = pca.acquireToken(parameters).get();
      return result.accessToken();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException("Failed to acquire token via device code flow", e);
    }
  }

  void ensurePca() {
    if (pca == null) {
      synchronized (this) {
        if (pca == null) {
          try {
            String authority = config.getStateless().getMsal().getAuthority();
            String clientId = config.getStateless().getMsal().getPublicClientId();
            if (clientId == null) {
              clientId = config.getStateless().getMsal().getClientId(); // fallback for backward compat
            }
            if (authority == null || clientId == null) {
              throw new IllegalStateException(
                  "Missing MSAL authority/clientId in restclient stateless configuration");
            }
            Path cachePath = expandHome(config.getStateless().getCachePath());
            ensureParentDirWithPerms(cachePath);
            ITokenCacheAccessAspect cache = new FileTokenCache(cachePath);
            pca =
                PublicClientApplication.builder(clientId)
                    .authority(authority)
                    .setTokenCacheAccessAspect(cache)
                    .build();
          } catch (Exception e) {
            throw new RuntimeException("Failed to initialize MSAL PublicClientApplication", e);
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

  Path expandHome(String path) {
    if (path == null)
      return Paths.get(System.getProperty("user.home"), ".mojito", "msal-token-cache.json");
    if (path.startsWith("~")) {
      return Paths.get(System.getProperty("user.home"), path.substring(1)).normalize();
    }
    return Paths.get(path);
  }

  void ensureParentDirWithPerms(Path filePath) throws IOException {
    Path dir = filePath.getParent();
    if (dir != null && !Files.exists(dir)) {
      Files.createDirectories(dir);
    }
    // Best-effort: set 700 on dir and 600 on file if created here. OS-dependent, ignore failures.
    try {
      if (dir != null) {
        new File(dir.toString()).setReadable(true, true);
        new File(dir.toString()).setWritable(true, true);
        new File(dir.toString()).setExecutable(true, true);
      }
      if (!Files.exists(filePath)) {
        Files.createFile(filePath);
        File f = filePath.toFile();
        f.setReadable(true, true);
        f.setWritable(true, true);
      }
    } catch (Exception ignored) {
    }
  }

  static class FileTokenCache implements ITokenCacheAccessAspect {

    final Path cacheFile;

    FileTokenCache(Path cacheFile) {
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
        // ignore and continue without cache
      }
    }

    @Override
    public void afterCacheAccess(ITokenCacheAccessContext context) {
      if (context.hasCacheChanged()) {
        try {
          String data = context.tokenCache().serialize();
          Files.writeString(cacheFile, data, StandardCharsets.UTF_8);
        } catch (IOException e) {
          // ignore failures to persist
        }
      }
    }
  }
}
