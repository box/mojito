package com.box.l10n.mojito.cli.command.extraction;

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
      ExtractionDiffNotificationSenderTest.class,
      ExtractionDiffNotificationSenderTest.ExtractionDiffNotificationSenderTestConfiguration.class
    })
public class ExtractionDiffNotificationSenderTest {
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

  ExtractionDiffNotificationSender extractionDiffNotificationSender;

  @Before
  public void setup() {
    this.extractionDiffNotificationSender = new ExtractionDiffNotificationSender(CHANNEL_ID);
    this.extractionDiffNotificationSender.slackClient = this.slackClientMock;
    Mockito.reset(this.slackClientMock);
  }

  @Test
  public void testSendMessageSuccess() throws SlackClientException {
    this.extractionDiffNotificationSender.sendMessage(MESSAGE);
    verify(this.slackClientMock, times(1)).sendInstantMessage(this.messageArgumentCaptor.capture());
    Message slackMessage = this.messageArgumentCaptor.getValue();
    Assert.assertEquals(CHANNEL_ID, slackMessage.getChannel());
    Assert.assertEquals(1, slackMessage.getAttachments().size());
    Attachment attachment = slackMessage.getAttachments().getFirst();
    Assert.assertEquals(COLOR_WARNING, attachment.getColor());
    Assert.assertTrue(attachment.getText().contains(MESSAGE));
  }

  @Test
  public void testSendMessageWithoutChannel() throws SlackClientException {
    this.extractionDiffNotificationSender.setChannel(null);
    Assert.assertThrows(
        ExtractionDiffNotificationSenderException.class,
        () -> this.extractionDiffNotificationSender.sendMessage(MESSAGE));
    this.extractionDiffNotificationSender.setChannel("");
    Assert.assertThrows(
        ExtractionDiffNotificationSenderException.class,
        () -> this.extractionDiffNotificationSender.sendMessage(MESSAGE));
  }

  @Test
  public void testSendMessageWithoutMessage() throws SlackClientException {
    this.extractionDiffNotificationSender.sendMessage(null);
    verify(this.slackClientMock, times(0)).sendInstantMessage(any());
    this.extractionDiffNotificationSender.sendMessage("");
    verify(this.slackClientMock, times(0)).sendInstantMessage(any());
  }
}
