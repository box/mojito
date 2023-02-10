package com.box.l10n.mojito.cli.command.extractiondiffnotifier;

import static com.box.l10n.mojito.slack.SlackClient.COLOR_DANGER;
import static com.box.l10n.mojito.slack.SlackClient.COLOR_GOOD;
import static com.box.l10n.mojito.slack.SlackClient.COLOR_WARNING;
import static com.box.l10n.mojito.slack.request.Attachment.MRKDWNIN_TEXT;

import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffStatistics;
import com.box.l10n.mojito.slack.SlackClient;
import com.box.l10n.mojito.slack.SlackClientException;
import com.box.l10n.mojito.slack.request.Attachment;
import com.box.l10n.mojito.slack.request.Message;
import com.box.l10n.mojito.thirdpartynotification.slack.SlackChannels;
import com.google.common.base.Preconditions;

public class ExtractionDiffNotifierSlack implements ExtractionDiffNotifier {

  SlackClient slackClient;

  String userEmailPattern;

  boolean useDirectMessage = false;

  SlackChannels slackChannels;

  ExtractionDiffNotifierMessageBuilder extractionDiffNotifierMessageBuilder;

  String username;

  public ExtractionDiffNotifierSlack(
      SlackClient slackClient,
      String userEmailPattern,
      boolean useDirectMessage,
      ExtractionDiffNotifierMessageBuilder extractionDiffNotifierMessageBuilder,
      String username) {
    this.extractionDiffNotifierMessageBuilder =
        Preconditions.checkNotNull(extractionDiffNotifierMessageBuilder);
    this.slackClient = Preconditions.checkNotNull(slackClient);
    this.userEmailPattern = Preconditions.checkNotNull(userEmailPattern);
    this.useDirectMessage = useDirectMessage;
    this.slackChannels = new SlackChannels(slackClient);
    this.username = username;
  }

  @Override
  public String sendDiffStatistics(ExtractionDiffStatistics extractionDiffStatistics) {
    try {
      String message = extractionDiffNotifierMessageBuilder.getMessage(extractionDiffStatistics);

      Message slackMessage = new Message();
      slackMessage.setChannel(
          slackChannels.getSlackChannelForDirectOrBotMessage(
              useDirectMessage, username, userEmailPattern));
      Attachment attachment = new Attachment();
      attachment.setText(message);
      attachment.setColor(getAttachmentColor(extractionDiffStatistics));
      attachment.getMrkdwnIn().add(MRKDWNIN_TEXT);
      slackMessage.getAttachments().add(attachment);

      slackClient.sendInstantMessage(slackMessage);

      return message;
    } catch (SlackClientException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void sendNoChangesNotification() {
    // no-op
  }

  String getAttachmentColor(ExtractionDiffStatistics extractionDiffStatistics) {
    String attachmentColor;
    switch (extractionDiffNotifierMessageBuilder.getMessageSeverityLevel(
        extractionDiffStatistics)) {
      case WARNING:
        attachmentColor = COLOR_WARNING;
        break;
      case ERROR:
        attachmentColor = COLOR_DANGER;
        break;
      default:
        attachmentColor = COLOR_GOOD;
    }
    return attachmentColor;
  }
}
