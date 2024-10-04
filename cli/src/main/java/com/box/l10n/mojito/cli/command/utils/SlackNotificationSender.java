package com.box.l10n.mojito.cli.command.utils;

import static com.box.l10n.mojito.slack.SlackClient.COLOR_WARNING;
import static com.box.l10n.mojito.slack.request.Attachment.MRKDWNIN_TEXT;

import com.box.l10n.mojito.slack.SlackClient;
import com.box.l10n.mojito.slack.SlackClientException;
import com.box.l10n.mojito.slack.request.Attachment;
import com.box.l10n.mojito.slack.request.Message;
import com.google.common.base.Strings;

public class SlackNotificationSender {
  private final SlackClient slackClient;

  public SlackNotificationSender(SlackClient slackClient) {
    this.slackClient = slackClient;
  }

  public void sendMessage(String channel, String message) {
    if (!Strings.isNullOrEmpty(message)) {
      try {
        Message slackMessage = new Message();
        if (Strings.isNullOrEmpty(channel)) {
          throw new SlackNotificationSenderException(
              "Channel cannot be empty for a Slack notification");
        }
        slackMessage.setChannel(channel);
        Attachment attachment = new Attachment();
        attachment.setText(message);
        attachment.setColor(COLOR_WARNING);
        attachment.getMrkdwnIn().add(MRKDWNIN_TEXT);
        slackMessage.getAttachments().add(attachment);

        this.slackClient.sendInstantMessage(slackMessage);
      } catch (SlackClientException e) {
        throw new SlackNotificationSenderException(
            "Error sending Slack notification: " + e.getMessage(), e);
      }
    }
  }
}
