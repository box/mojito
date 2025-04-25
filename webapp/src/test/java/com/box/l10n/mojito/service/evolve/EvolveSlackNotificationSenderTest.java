package com.box.l10n.mojito.service.evolve;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.slack.SlackClient;
import com.box.l10n.mojito.slack.SlackClientException;
import com.box.l10n.mojito.slack.request.Attachment;
import com.box.l10n.mojito.slack.request.Message;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import reactor.util.retry.Retry;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {EvolveSlackNotificationSenderTest.class})
public class EvolveSlackNotificationSenderTest {
  @Mock SlackClient slackClientMock;

  @Captor ArgumentCaptor<Message> messageCaptor;

  EvolveSlackNotificationSender evolveSlackNotificationSender;

  final int courseId = 1;

  final String channel = "@user";

  final String courseUrlTemplate = "https://www.test.com/courses/{courseId,number,#}/content";

  final Retry retry = Retry.backoff(2, Duration.ofSeconds(1)).maxBackoff(Duration.ofSeconds(1));

  @Test
  public void testNotifyFullyTranslatedCourse() throws SlackClientException, InterruptedException {
    this.evolveSlackNotificationSender = new EvolveSlackNotificationSender(null);

    CountDownLatch latch =
        this.evolveSlackNotificationSender.notifyFullyTranslatedCourse(
            this.courseId, this.channel, this.courseUrlTemplate, this.retry);
    assertNull(latch);

    this.evolveSlackNotificationSender = new EvolveSlackNotificationSender(this.slackClientMock);

    latch =
        this.evolveSlackNotificationSender.notifyFullyTranslatedCourse(
            this.courseId, this.channel, this.courseUrlTemplate, this.retry);
    latch.await();

    verify(this.slackClientMock, times(1)).sendInstantMessage(this.messageCaptor.capture());

    Message message = this.messageCaptor.getValue();
    assertEquals(1, message.getAttachments().size());
    Attachment attachment = message.getAttachments().getFirst();
    assertTrue(
        attachment
            .getText()
            .contains(String.format("https://www.test.com/courses/%d/content", this.courseId)));

    Mockito.reset(this.slackClientMock);
    when(this.slackClientMock.sendInstantMessage(any(Message.class)))
        .thenThrow(SlackClientException.class)
        .thenReturn(null);
    this.evolveSlackNotificationSender = new EvolveSlackNotificationSender(this.slackClientMock);

    latch =
        this.evolveSlackNotificationSender.notifyFullyTranslatedCourse(
            this.courseId, this.channel, this.courseUrlTemplate, this.retry);
    latch.await();

    verify(this.slackClientMock, times(2)).sendInstantMessage(this.messageCaptor.capture());

    Mockito.reset(this.slackClientMock);
    when(this.slackClientMock.sendInstantMessage(any(Message.class)))
        .thenThrow(SlackClientException.class);
    this.evolveSlackNotificationSender = new EvolveSlackNotificationSender(this.slackClientMock);

    latch =
        this.evolveSlackNotificationSender.notifyFullyTranslatedCourse(
            this.courseId, this.channel, this.courseUrlTemplate, this.retry);
    latch.await();

    verify(this.slackClientMock, times(3)).sendInstantMessage(this.messageCaptor.capture());
  }

  @Test
  public void testNotifyFullyTranslatedCourseWithInvalidParameters() {
    this.evolveSlackNotificationSender = new EvolveSlackNotificationSender(this.slackClientMock);

    assertThrows(
        NullPointerException.class,
        () ->
            this.evolveSlackNotificationSender.notifyFullyTranslatedCourse(
                this.courseId, null, this.courseUrlTemplate, this.retry));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            this.evolveSlackNotificationSender.notifyFullyTranslatedCourse(
                this.courseId, "", this.courseUrlTemplate, this.retry));

    assertThrows(
        NullPointerException.class,
        () ->
            this.evolveSlackNotificationSender.notifyFullyTranslatedCourse(
                this.courseId, this.channel, null, this.retry));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            this.evolveSlackNotificationSender.notifyFullyTranslatedCourse(
                this.courseId, this.channel, "", this.retry));

    assertThrows(
        NullPointerException.class,
        () ->
            this.evolveSlackNotificationSender.notifyFullyTranslatedCourse(
                this.courseId, this.channel, this.courseUrlTemplate, null));
  }
}
