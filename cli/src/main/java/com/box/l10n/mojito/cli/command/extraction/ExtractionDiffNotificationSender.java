package com.box.l10n.mojito.cli.command.extraction;

import static com.box.l10n.mojito.slack.SlackClient.COLOR_WARNING;
import static com.box.l10n.mojito.slack.request.Attachment.MRKDWNIN_TEXT;

import com.box.l10n.mojito.slack.SlackClient;
import com.box.l10n.mojito.slack.SlackClientException;
import com.box.l10n.mojito.slack.request.Attachment;
import com.box.l10n.mojito.slack.request.Message;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

@Configurable
public class ExtractionDiffNotificationSender {
  @Autowired SlackClient slackClient;

  private String channel;

  public ExtractionDiffNotificationSender(String channel) {
    this.channel = channel;
  }

  private Message getSlackMessage(String message) throws SlackClientException {
    Message slackMessage = new Message();
    if (Strings.isNullOrEmpty(this.channel)) {
      throw new ExtractionDiffNotificationSenderException(
          "Channel cannot be empty for a Slack notification");
    }
    slackMessage.setChannel(this.channel);
    Attachment attachment = new Attachment();
    attachment.setText(message);
    attachment.setColor(COLOR_WARNING);
    attachment.getMrkdwnIn().add(MRKDWNIN_TEXT);
    slackMessage.getAttachments().add(attachment);
    return slackMessage;
  }

  public void sendMessage(String message) {
    if (!Strings.isNullOrEmpty(message)) {
      try {
        this.slackClient.sendInstantMessage(this.getSlackMessage(message));
      } catch (SlackClientException e) {
        throw new ExtractionDiffNotificationSenderException(
            "Error sending Slack notification: " + e.getMessage(), e);
      }
    }
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }
}
