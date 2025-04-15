package com.box.l10n.mojito.service.appender;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n")
public class AssetAppenderConfig {

  Map<String, AssetAppenderRepoConfig> assetAppender = new HashMap<>();

  public Map<String, AssetAppenderRepoConfig> getAssetAppender() {
    return assetAppender;
  }

  public void setAssetAppender(Map<String, AssetAppenderRepoConfig> assetAppender) {
    this.assetAppender = assetAppender;
  }
}
