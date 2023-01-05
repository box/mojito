package com.box.l10n.mojito.slack;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Create Slack clients from configuration and expose them by client ids. This allows to configure
 * and communicate with different Slack instances.
 *
 * <p>The legacy that allows to configure a single client instance is in {@link
 * com.box.l10n.mojito.slack.SlackClientConfiguration}
 */
@Component
public class SlackClients {

  SlackClientsConfigurationProperties slackClientsConfigurationProperties;
  Map<String, SlackClient> mapIdToClient;

  public SlackClients(SlackClientsConfigurationProperties slackClientsConfigurationProperties) {
    this.slackClientsConfigurationProperties = slackClientsConfigurationProperties;
    createClientsFromConfigration();
  }

  public SlackClient getById(String id) {
    return mapIdToClient.get(id);
  }

  void createClientsFromConfigration() {
    mapIdToClient =
        slackClientsConfigurationProperties.getSlackClients().entrySet().stream()
            .map(
                e -> {
                  SlackClient slackClient = new SlackClient(e.getValue().getToken());
                  return new SimpleEntry<String, SlackClient>(e.getKey(), slackClient);
                })
            .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
  }
}
