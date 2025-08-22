package com.box.l10n.mojito.react;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.screenshots-ui")
public class ScreenshotsUiConfig {

  boolean legacyEnabled = true;

  public boolean isLegacyEnabled() {
    return legacyEnabled;
  }

  public void setLegacyEnabled(boolean legacyEnabled) {
    this.legacyEnabled = legacyEnabled;
  }
}
