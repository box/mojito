package com.box.l10n.mojito.service.evolve;

import static com.box.l10n.mojito.slack.SlackClient.COLOR_GOOD;
import static com.box.l10n.mojito.slack.request.Attachment.MRKDWNIN_TEXT;

import com.box.l10n.mojito.slack.SlackClient;
import com.box.l10n.mojito.slack.SlackClientException;
import com.box.l10n.mojito.slack.request.Attachment;
import com.box.l10n.mojito.slack.request.Message;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.ibm.icu.text.MessageFormat;
import java.util.concurrent.CountDownLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
public class EvolveSlackNotificationSender {
  private static final Logger LOG = LoggerFactory.getLogger(EvolveSlackNotificationSender.class);

  private final SlackClient slackClient;

  public EvolveSlackNotificationSender(@Autowired(required = false) SlackClient slackClient) {
    this.slackClient = slackClient;
  }

  public CountDownLatch notifyFullyTranslatedCourse(
      int courseId, String channel, String courseUrlTemplate, Retry retry) {
    Preconditions.checkNotNull(channel);
    Preconditions.checkArgument(!channel.isBlank());
    Preconditions.checkNotNull(courseUrlTemplate);
    Preconditions.checkArgument(!courseUrlTemplate.isBlank());
    Preconditions.checkNotNull(retry);

    if (this.slackClient != null) {
      CountDownLatch latch = new CountDownLatch(1);
      Message slackMessage = new Message();
      slackMessage.setChannel(channel);
      Attachment attachment = new Attachment();
      MessageFormat messageFormat = new MessageFormat(courseUrlTemplate);
      ImmutableMap<String, Integer> messageParamMap =
          new ImmutableMap.Builder<String, Integer>().put("courseId", courseId).build();
      attachment.setText(
          "This course has been fully translated: " + messageFormat.format(messageParamMap));
      attachment.setColor(COLOR_GOOD);
      attachment.getMrkdwnIn().add(MRKDWNIN_TEXT);
      slackMessage.getAttachments().add(attachment);
      Mono.fromRunnable(
              () -> {
                try {
                  this.slackClient.sendInstantMessage(slackMessage);
                } catch (SlackClientException e) {
                  LOG.error("Failed to send the Slack message for the course: " + courseId, e);
                  throw new EvolveSlackNotificationException(e.getMessage(), e);
                }
              })
          .retryWhen(retry)
          .doOnTerminate(latch::countDown)
          .subscribe();
      return latch;
    } else {
      LOG.error("Slack client is null");
    }
    return null;
  }
}
