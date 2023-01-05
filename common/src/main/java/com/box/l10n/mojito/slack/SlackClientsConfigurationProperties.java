package com.box.l10n.mojito.slack;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("l10n")
public class SlackClientsConfigurationProperties {
  Map<String, SlackClientConfigurationProperties> slackClients = new HashMap<>();

  public Map<String, SlackClientConfigurationProperties> getSlackClients() {
    return slackClients;
  }

  public void setSlackClients(Map<String, SlackClientConfigurationProperties> slackClients) {
    this.slackClients = slackClients;
  }

  static class SlackClientConfigurationProperties {
    String token;

    public String getToken() {
      return token;
    }

    public void setToken(String token) {
      this.token = token;
    }
  }
}
