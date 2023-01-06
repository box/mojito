package com.box.l10n.mojito.service.branch.notification;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.branch-notification.notifiers")
public class BranchNotificationMessageSendersConfigurationProperties {

  Map<String, NoopConfigurationProperties> noop = new HashMap<>();

  public Map<String, NoopConfigurationProperties> getNoop() {
    return noop;
  }

  public void setNoop(Map<String, NoopConfigurationProperties> noop) {
    this.noop = noop;
  }

  static class NoopConfigurationProperties {

    boolean enabled;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }
}
