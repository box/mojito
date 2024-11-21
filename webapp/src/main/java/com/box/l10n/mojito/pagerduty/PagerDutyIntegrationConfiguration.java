package com.box.l10n.mojito.pagerduty;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.pagerduty")
public class PagerDutyIntegrationConfiguration {
  private String defaultIntegration;

  private Map<String, String> integrations = new HashMap<>();

  public Map<String, String> getIntegrations() {
    return integrations;
  }

  public void setIntegrations(Map<String, String> integrations) {
    this.integrations = integrations;
  }

  public String getDefaultIntegration() {
    return defaultIntegration;
  }

  public void setDefaultIntegration(String defaultIntegration) {
    this.defaultIntegration = defaultIntegration;
  }
}
