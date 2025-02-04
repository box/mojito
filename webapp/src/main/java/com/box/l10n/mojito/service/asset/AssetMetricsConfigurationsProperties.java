package com.box.l10n.mojito.service.asset;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("l10n")
public class AssetMetricsConfigurationsProperties {
  Map<String, AssetMetricsConfigurationProperties> assetMetrics = new HashMap<>();

  public Map<String, AssetMetricsConfigurationProperties> getAssetMetrics() {
    return assetMetrics;
  }

  public void setAssetMetrics(Map<String, AssetMetricsConfigurationProperties> assetMetrics) {
    this.assetMetrics = assetMetrics;
  }
}
