package com.box.l10n.mojito.common.notification;

import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "l10n.integrity-check-notifier.enabled", havingValue = "true")
@ConfigurationProperties("l10n.integrity-check-notifier")
public class IntegrityCheckNotifierConfiguration {
  private String slackClientId;
  private String slackChannel;
  private Map<String, WarningProperties> warnings;

  public void setSlackChannel(String slackChannel) {
    this.slackChannel = slackChannel;
  }

  public void setSlackClientId(String slackClientId) {
    this.slackClientId = slackClientId;
  }

  public String getSlackChannel() {
    return slackChannel;
  }

  public String getSlackClientId() {
    return slackClientId;
  }

  public Map<String, WarningProperties> getWarnings() {
    return warnings;
  }

  public void setWarnings(Map<String, WarningProperties> warnings) {
    this.warnings = warnings;
  }

  public static class WarningProperties {
    private String title;
    private String text;

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public String getText() {
      return text;
    }

    public void setText(String text) {
      this.text = text;
    }
  }
}
