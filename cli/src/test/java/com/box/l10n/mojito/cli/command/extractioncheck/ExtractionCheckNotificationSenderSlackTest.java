package com.box.l10n.mojito.cli.command.extractioncheck;

import static com.box.l10n.mojito.cli.command.extractioncheck.ExtractionCheckNotificationSender.QUOTE_MARKER;
import static com.box.l10n.mojito.slack.SlackClient.COLOR_WARNING;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.cli.command.checks.CliCheckResult;
import com.box.l10n.mojito.slack.SlackClient;
import com.box.l10n.mojito.slack.SlackClientException;
import com.box.l10n.mojito.slack.request.Attachment;
import com.box.l10n.mojito.slack.request.Message;
import com.box.l10n.mojito.thirdpartynotification.slack.SlackChannels;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
    classes = {
      ExtractionCheckNotificationSenderSlackTest.class,
      ExtractionCheckNotificationSenderSlackTest
          .ExtractionCheckNotificationSenderSlackTestConfiguration.class
    })
public class ExtractionCheckNotificationSenderSlackTest {

  @TestConfiguration
  static class ExtractionCheckNotificationSenderSlackTestConfiguration {

    @Bean
    public SlackClient slackClient() {
      return Mockito.mock(SlackClient.class);
    }

    @Bean
    public SlackChannels slackChannels() {
      return Mockito.mock(SlackChannels.class);
    }
  }

  @Autowired SlackClient slackClientMock;

  @Autowired SlackChannels slackChannelsMock;

  @Captor ArgumentCaptor<Message> messageArgumentCaptor;

  ExtractionCheckNotificationSenderSlack extractionCheckNotificationSenderSlack;

  @Before
  public void setup() {
    extractionCheckNotificationSenderSlack =
        new ExtractionCheckNotificationSenderSlack(
            "user",
            "{0}@somewhere.com",
            "{baseMessage}",
            "This is a hard failure message.",
            "This is a checks skipped message.",
            false);
    extractionCheckNotificationSenderSlack.slackClient = slackClientMock;
    extractionCheckNotificationSenderSlack.slackChannels = slackChannelsMock;
    Mockito.reset(slackChannelsMock);
    Mockito.reset(slackClientMock);
  }

  @Test
  public void testSendFailureNotifications() throws SlackClientException {
    List<CliCheckResult> results = new ArrayList<>();
    CliCheckResult result = new CliCheckResult(false, false, "Test Check");
    result.setNotificationText("Some notification text");
    results.add(result);
    extractionCheckNotificationSenderSlack.sendFailureNotification(results, false);
    verify(slackClientMock, times(1)).sendInstantMessage(messageArgumentCaptor.capture());
    verify(slackChannelsMock, times(1))
        .getSlackChannelForDirectOrBotMessage(false, "user", "{0}@somewhere.com");
    Message slackMessage = messageArgumentCaptor.getValue();
    Assert.assertTrue(slackMessage.getAttachments().size() == 1);
    Attachment attachment = slackMessage.getAttachments().get(0);
    Assert.assertTrue(attachment.getColor().equals(COLOR_WARNING));
    Assert.assertTrue(attachment.getText().contains("*i18n source string checks failed*"));
    Assert.assertTrue(attachment.getText().contains("Test Check"));
    Assert.assertTrue(attachment.getText().contains("Some notification text"));
    Assert.assertTrue(attachment.getText().contains(":warning:"));
  }

  @Test
  public void testSendFailureNotificationsMultipleFailures() throws SlackClientException {
    List<CliCheckResult> results = new ArrayList<>();
    CliCheckResult result = new CliCheckResult(false, false, "Test Check");
    result.setNotificationText("Some notification text");
    CliCheckResult result2 = new CliCheckResult(false, false, "Other Check");
    result2.setNotificationText("Some other notification text");
    results.add(result);
    results.add(result2);
    extractionCheckNotificationSenderSlack.sendFailureNotification(results, false);
    verify(slackClientMock, times(1)).sendInstantMessage(messageArgumentCaptor.capture());
    verify(slackChannelsMock, times(1))
        .getSlackChannelForDirectOrBotMessage(false, "user", "{0}@somewhere.com");
    Message slackMessage = messageArgumentCaptor.getValue();
    Assert.assertTrue(slackMessage.getAttachments().size() == 1);
    Attachment attachment = slackMessage.getAttachments().get(0);
    Assert.assertTrue(attachment.getColor().equals(COLOR_WARNING));
    Assert.assertTrue(attachment.getText().contains("*i18n source string checks failed*"));
    Assert.assertTrue(attachment.getText().contains("Test Check"));
    Assert.assertTrue(attachment.getText().contains("Some notification text"));
    Assert.assertTrue(attachment.getText().contains("Other Check"));
    Assert.assertTrue(attachment.getText().contains("Some other notification text"));
  }

  @Test
  public void testSendFailureNotificationsHardFail() throws SlackClientException {
    List<CliCheckResult> results = new ArrayList<>();
    CliCheckResult result = new CliCheckResult(false, true, "Test Check");
    result.setNotificationText("Some notification text");
    results.add(result);
    extractionCheckNotificationSenderSlack.sendFailureNotification(results, true);
    verify(slackClientMock, times(1)).sendInstantMessage(messageArgumentCaptor.capture());
    verify(slackChannelsMock, times(1))
        .getSlackChannelForDirectOrBotMessage(false, "user", "{0}@somewhere.com");
    Message slackMessage = messageArgumentCaptor.getValue();
    Assert.assertTrue(slackMessage.getAttachments().size() == 1);
    Attachment attachment = slackMessage.getAttachments().get(0);
    Assert.assertTrue(attachment.getColor().equals(COLOR_WARNING));
    Assert.assertTrue(attachment.getText().contains("*i18n source string checks failed*"));
    Assert.assertTrue(attachment.getText().contains("Test Check"));
    Assert.assertTrue(attachment.getText().contains("Some notification text"));
    Assert.assertTrue(attachment.getText().contains("This is a hard failure message."));
    Assert.assertTrue(attachment.getText().contains(":stop:"));
  }

  @Test
  public void testNoNotificationsSentIfNoFailures() throws SlackClientException {
    List<CliCheckResult> results = new ArrayList<>();
    CliCheckResult result = new CliCheckResult(true, false, "Test Check");
    result.setNotificationText("Some notification text");
    results.add(result);
    extractionCheckNotificationSenderSlack.sendFailureNotification(results, false);
    verify(slackClientMock, times(0)).sendInstantMessage(isA(Message.class));
    verify(slackChannelsMock, times(0))
        .getSlackChannelForDirectOrBotMessage(
            isA(Boolean.class), isA(String.class), isA(String.class));
  }

  @Test
  public void testNoNotificationsSentIfResultsIsNull() throws SlackClientException {
    extractionCheckNotificationSenderSlack.sendFailureNotification(null, false);
    verify(slackClientMock, times(0)).sendInstantMessage(isA(Message.class));
    verify(slackChannelsMock, times(0))
        .getSlackChannelForDirectOrBotMessage(
            isA(Boolean.class), isA(String.class), isA(String.class));
  }

  @Test(expected = ExtractionCheckNotificationSenderException.class)
  public void testExceptionThrownIfSlackClientFails() throws SlackClientException {
    when(slackClientMock.sendInstantMessage(isA(Message.class)))
        .thenThrow(SlackClientException.class);
    List<CliCheckResult> results = new ArrayList<>();
    CliCheckResult result = new CliCheckResult(false, false, "Test Check");
    result.setNotificationText("Some notification text");
    results.add(result);
    extractionCheckNotificationSenderSlack.sendFailureNotification(results, false);
  }

  @Test(expected = ExtractionCheckNotificationSenderException.class)
  public void testExceptionThrownIfUsernameIsEmpty() {
    new ExtractionCheckNotificationSenderSlack(
        "", "emailPattern", "messageTemplate", "hardFail", "checksSkipped", false);
  }

  @Test
  public void testQuoteMarkersAreUpdated() throws SlackClientException {
    List<CliCheckResult> results = new ArrayList<>();
    CliCheckResult result = new CliCheckResult(false, true, "Test Check");
    result.setNotificationText(
        "Some notification text with " + QUOTE_MARKER + "some.text.id" + QUOTE_MARKER);
    results.add(result);
    extractionCheckNotificationSenderSlack.sendFailureNotification(results, true);
    verify(slackClientMock, times(1)).sendInstantMessage(messageArgumentCaptor.capture());
    verify(slackChannelsMock, times(1))
        .getSlackChannelForDirectOrBotMessage(false, "user", "{0}@somewhere.com");
    Message slackMessage = messageArgumentCaptor.getValue();
    Assert.assertTrue(slackMessage.getAttachments().size() == 1);
    Attachment attachment = slackMessage.getAttachments().get(0);
    Assert.assertTrue(attachment.getColor().equals(COLOR_WARNING));
    Assert.assertTrue(attachment.getText().contains("*i18n source string checks failed*"));
    Assert.assertTrue(attachment.getText().contains("Test Check"));
    Assert.assertTrue(attachment.getText().contains("Some notification text"));
    Assert.assertTrue(attachment.getText().contains("This is a hard failure message."));
    Assert.assertTrue(attachment.getText().contains("`some.text.id`"));
  }
}
