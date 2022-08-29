package com.box.l10n.mojito.slack;

import com.box.l10n.mojito.slack.request.Channel;
import com.box.l10n.mojito.slack.request.Message;
import com.box.l10n.mojito.slack.response.ChatPostMessageResponse;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {
      SlackClientTest.class,
      SlackClientConfiguration.class,
      SlackClientTest.TestConfig.class
    })
@EnableConfigurationProperties
public class SlackClientTest {

  @Autowired(required = false)
  SlackClient slackClient;

  @Autowired TestConfig testConfig;

  @Before
  public void assumeClient() {
    Assume.assumeNotNull(slackClient);
  }

  @Test
  public void testClient() throws SlackClientException {
    Channel instantMessageChannel =
        slackClient.getInstantMessageChannel(testConfig.emailDestination);

    Message message = new Message();
    message.setText("test");
    message.setChannel(instantMessageChannel.getId());
    ChatPostMessageResponse chatPostMessageResponse = slackClient.sendInstantMessage(message);

    Message reply = new Message();
    reply.setChannel(instantMessageChannel.getId());
    reply.setText("テスト");
    reply.setThreadTs(chatPostMessageResponse.getTs());

    slackClient.sendInstantMessage(reply);
  }

  @Configuration
  @ConfigurationProperties("test.l10n.slack")
  static class TestConfig {

    String emailDestination = "someemail@test.com";

    public String getEmailDestination() {
      return emailDestination;
    }

    public void setEmailDestination(String emailDestination) {
      this.emailDestination = emailDestination;
    }
  }
}
