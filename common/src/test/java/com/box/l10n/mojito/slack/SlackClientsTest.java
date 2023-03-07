package com.box.l10n.mojito.slack;

import static org.junit.jupiter.api.Assertions.*;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {SlackClientsConfigurationProperties.class, SlackClients.class},
    properties = {
      "l10n.slack-clients.slackClientId1.token=token1",
      "l10n.slack-clients.slackClientId2.token=token2",
    })
@EnableConfigurationProperties
public class SlackClientsTest {

  @Autowired SlackClients slackClients;

  @Test
  public void createClientsFromConfigration() {
    Assertions.assertThat(slackClients.mapIdToClient)
        .containsOnlyKeys("slackClientId1", "slackClientId2");
    Assertions.assertThat(slackClients.getById("slackClientId1")).isNotNull();
    Assertions.assertThat(slackClients.getById("slackClientId1").authToken).isEqualTo("token1");
    Assertions.assertThat(slackClients.getById("slackClientId2")).isNotNull();
    Assertions.assertThat(slackClients.getById("slackClientId2").authToken).isEqualTo("token2");
  }
}
