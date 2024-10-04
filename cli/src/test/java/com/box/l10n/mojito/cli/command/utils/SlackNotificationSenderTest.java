package com.box.l10n.mojito.cli.command.utils;

import static com.box.l10n.mojito.slack.SlackClient.COLOR_WARNING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.box.l10n.mojito.slack.SlackClient;
import com.box.l10n.mojito.slack.SlackClientException;
import com.box.l10n.mojito.slack.request.Attachment;
import com.box.l10n.mojito.slack.request.Message;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
    classes = {
      SlackNotificationSenderTest.class,
      SlackNotificationSenderTest.ExtractionDiffNotificationSenderTestConfiguration.class
    })
public class SlackNotificationSenderTest {
  @TestConfiguration
  static class ExtractionDiffNotificationSenderTestConfiguration {
    @Bean(name = "mockedSlackClient")
    public SlackClient slackClient() {
      return Mockito.mock(SlackClient.class);
    }
  }

  private static final String CHANNEL_ID = "channelID";

  private static final String MESSAGE = "Test message";

  @Qualifier("mockedSlackClient")
  @Autowired
  SlackClient slackClientMock;

  @Captor ArgumentCaptor<Message> messageArgumentCaptor;

  SlackNotificationSender slackNotificationSender;

  @Before
  public void setup() {
    this.slackNotificationSender = new SlackNotificationSender(this.slackClientMock);
    Mockito.reset(this.slackClientMock);
  }

  @Test
  public void testSendMessageSuccess() throws SlackClientException {
    this.slackNotificationSender.sendMessage(CHANNEL_ID, MESSAGE);
    verify(this.slackClientMock, times(1)).sendInstantMessage(this.messageArgumentCaptor.capture());
    Message slackMessage = this.messageArgumentCaptor.getValue();
    Assert.assertEquals(CHANNEL_ID, slackMessage.getChannel());
    Assert.assertEquals(1, slackMessage.getAttachments().size());
    Attachment attachment = slackMessage.getAttachments().getFirst();
    Assert.assertEquals(COLOR_WARNING, attachment.getColor());
    Assert.assertTrue(attachment.getText().contains(MESSAGE));
  }

  @Test
  public void testSendMessageWithoutChannel() {
    Assert.assertThrows(
        SlackNotificationSenderException.class,
        () -> this.slackNotificationSender.sendMessage(null, MESSAGE));
    Assert.assertThrows(
        SlackNotificationSenderException.class,
        () -> this.slackNotificationSender.sendMessage("", MESSAGE));
  }

  @Test
  public void testSendMessageWithoutMessage() throws SlackClientException {
    this.slackNotificationSender.sendMessage(CHANNEL_ID, null);
    verify(this.slackClientMock, times(0)).sendInstantMessage(any());
    this.slackNotificationSender.sendMessage(CHANNEL_ID, "");
    verify(this.slackClientMock, times(0)).sendInstantMessage(any());
  }
}
