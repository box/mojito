package com.box.l10n.mojito.cli.command.extractioncheck;

import com.box.l10n.mojito.cli.command.checks.CliCheckResult;
import com.box.l10n.mojito.slack.SlackClient;
import com.box.l10n.mojito.slack.SlackClientException;
import com.box.l10n.mojito.slack.request.Attachment;
import com.box.l10n.mojito.slack.request.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

import static com.box.l10n.mojito.notifications.service.slack.SlackUtils.getSlackChannel;
import static com.box.l10n.mojito.slack.request.Attachment.MRKDWNIN_TEXT;

@Configurable
public class ExtractorCheckNotificationSenderSlack implements ExtractorCheckNotificationSender {

    @Autowired
    SlackClient slackClient;

    // TODO may want to pass those as CLI param, to avoid having to set envrionment varialbe, etc.
    @Value("${l10n.notifications.slack.userEmailPattern}")
    String userEmailPattern;

    @Value("${l10n.notifications.slack.useDirectMessage:false}")
    boolean useDirectMessage;

    String messageTemplate;
    String username;

    public ExtractorCheckNotificationSenderSlack(String messageTemplate, String username) {
        this.messageTemplate = messageTemplate;
        this.username = username;
    }

    @Override
    public void sendFailureNotification(List<CliCheckResult> failures, boolean hardFail) throws ExtractorCheckNotificationSenderException {
        try {
            //TODO The message builder can be it's own component or in a method here... but for the general idea
            Message slackMessage = buildSlackMessage(String.format(messageTemplate, "TODO"), username);
            slackClient.sendInstantMessage(slackMessage);
        } catch (SlackClientException e) {
            throw new ExtractorCheckNotificationSenderException();
        }
    }

    @Override
    public void sendChecksSkippedNotifications() {

    }

    private Message buildSlackMessage(String message, String username) throws SlackClientException {
        Message slackMessage = new Message();
        slackMessage.setChannel(getSlackChannel(useDirectMessage, username, userEmailPattern, slackClient));
        Attachment attachment = new Attachment();
        attachment.setText(message);
        // attachment.setColor(serviceParameters.getOrDefault(SlackParameters.ATTACHMENT_COLOR.getParamKey(), COLOR_GOOD));
        attachment.getMrkdwnIn().add(MRKDWNIN_TEXT);
        slackMessage.getAttachments().add(attachment);
        return slackMessage;
    }
}
