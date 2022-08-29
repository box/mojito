package com.box.l10n.mojito.service.branch.notification.slack;

import static com.box.l10n.mojito.slack.SlackClient.COLOR_GOOD;
import static com.box.l10n.mojito.slack.request.Action.ACTION_STYLE_PRIMARY;
import static com.box.l10n.mojito.slack.request.Action.ACTION_TYPE_BUTTON;
import static com.box.l10n.mojito.slack.request.Attachment.MRKDOWNIN_FILEDS;
import static com.box.l10n.mojito.slack.request.Attachment.MRKDOWNIN_PRETEXT;
import static com.box.l10n.mojito.slack.request.Attachment.MRKDWNIN_TEXT;

import com.box.l10n.mojito.service.branch.BranchUrlBuilder;
import com.box.l10n.mojito.slack.request.Action;
import com.box.l10n.mojito.slack.request.Attachment;
import com.box.l10n.mojito.slack.request.Field;
import com.box.l10n.mojito.slack.request.Message;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BranchNotificationMessageBuilderSlack {

  static final int STRING_IN_SUMMARY_COUNT = 20;
  static final int STRING_IN_SUMMARY_ABRREVIATE_LENGHT = 40;

  @Autowired BranchUrlBuilder branchUrlBuilder;

  @Value(
      "${l10n.branchNotification.slack.notification.message.new:We received your strings! "
          + "Please *add screenshots* as soon as possible and *wait for translations* before releasing.}")
  String newNotificationMsg;

  @Value(
      "${l10n.branchNotification.slack.notification.message.updated:Your branch was updated with new strings! "
          + "Please *add screenshots* as soon as possible and *wait for translations* before releasing.}")
  String updatedNotificationMsg;

  @Value(
      "${l10n.branchNotification.slack.notification.message.translationsReady:Translations are ready !! :party:}")
  String translationsReadyMsg;

  @Value(
      "${l10n.branchNotification.slack.notification.message.screenshotsMissing:"
          + ":warning: Please provide screenshots to help localization team :warning:}")
  String screenshotsMissingMsg;

  @Value(
      "${l10n.branchNotification.slack.notification.message.noMoreStrings:The branch was updated and there are no more strings to translate.}")
  String noMoreStringsMsg;

  public Message getNewMessage(String channel, String pr, List<String> sourceStrings) {
    Message message = getBaseMessage(channel, pr, sourceStrings, newNotificationMsg);
    return message;
  }

  public Message getUpdatedMessage(
      String channel, String threadTs, String pr, List<String> sourceStrings) {

    Message message = null;

    if (sourceStrings.isEmpty()) {
      message = new Message();
      message.setChannel(channel);
      message.setText(noMoreStringsMsg);
    } else {
      message = getBaseMessage(channel, pr, sourceStrings, updatedNotificationMsg);
    }

    message.setThreadTs(threadTs);
    return message;
  }

  public Message getTranslatedMessage(String channel, String threadTs) {
    Message message = new Message();
    message.setChannel(channel);
    message.setThreadTs(threadTs);
    message.setText(translationsReadyMsg);
    return message;
  }

  public Message getScreenshotMissingMessage(String channel, String threadTs) {
    Message message = new Message();
    message.setChannel(channel);
    message.setThreadTs(threadTs);
    message.setText(screenshotsMissingMsg);
    return message;
  }

  Message getBaseMessage(String channel, String pr, List<String> sourceStrings, String text) {

    Message message = new Message();

    message.setChannel(channel);

    Attachment attachment = new Attachment();
    message.getAttachments().add(attachment);

    attachment.setText(text);
    attachment.setColor(COLOR_GOOD);
    attachment.getMrkdwnIn().add(MRKDWNIN_TEXT);
    attachment.getMrkdwnIn().add(MRKDOWNIN_PRETEXT);
    attachment.getMrkdwnIn().add(MRKDOWNIN_FILEDS);

    Field field = new Field();
    field.setTitle("PR");
    field.setValue(pr);
    field.setShort(true);
    attachment.getFields().add(field);

    Field field2 = new Field();
    field2.setTitle("String number");
    field2.setValue(String.valueOf(sourceStrings.size()));
    field2.setShort(true);
    attachment.getFields().add(field2);

    Field field3 = new Field();
    field3.setTitle("Strings");
    field3.setValue(getSummaryString(sourceStrings));
    attachment.getFields().add(field3);

    Action actionScreenshot = new Action();
    actionScreenshot.setType(ACTION_TYPE_BUTTON);
    actionScreenshot.setText("Screenshots");
    actionScreenshot.setUrl(branchUrlBuilder.getBranchDashboardUrl(pr));
    actionScreenshot.setStyle(ACTION_STYLE_PRIMARY);
    attachment.getActions().add(actionScreenshot);

    return message;
  }

  String getSummaryString(List<String> strings) {
    return strings.stream()
        .limit(STRING_IN_SUMMARY_COUNT)
        .map(s -> StringUtils.abbreviate(s, STRING_IN_SUMMARY_ABRREVIATE_LENGHT))
        .collect(Collectors.joining(", "));
  }
}
