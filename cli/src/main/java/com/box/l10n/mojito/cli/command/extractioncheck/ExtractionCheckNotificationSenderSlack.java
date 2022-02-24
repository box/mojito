package com.box.l10n.mojito.cli.command.extractioncheck;

import com.box.l10n.mojito.cli.command.checks.CliCheckResult;
import com.box.l10n.mojito.slack.SlackClient;
import com.box.l10n.mojito.slack.SlackClientException;
import com.box.l10n.mojito.slack.request.Attachment;
import com.box.l10n.mojito.slack.request.Message;
import com.box.l10n.mojito.thirdpartynotification.slack.SlackChannels;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.List;
import java.util.stream.Collectors;

import static com.box.l10n.mojito.slack.SlackClient.COLOR_WARNING;
import static com.box.l10n.mojito.slack.request.Attachment.MRKDWNIN_TEXT;

@Configurable
public class ExtractionCheckNotificationSenderSlack extends ExtractionCheckNotificationSender {

    @Autowired
    SlackClient slackClient;

    @Autowired
    SlackChannels slackChannels;

    private final String username;
    private final String userEmailPattern;
    private final boolean useDirectMessage;

    public ExtractionCheckNotificationSenderSlack(String username, String userEmailPattern, String messageTemplate, String hardFailureMessage,
                                                  String checksSkippedMessage, boolean useDirectMessage) {
        super(messageTemplate, hardFailureMessage, checksSkippedMessage);
        if (Strings.isNullOrEmpty(username)) {
            throw new ExtractionCheckNotificationSenderException("Username must be provided when using Slack notifications.");
        }
        this.username = username;
        this.userEmailPattern = userEmailPattern;
        this.useDirectMessage = useDirectMessage;
    }

    @Override
    public void sendFailureNotification(List<CliCheckResult> results, boolean hardFail) {
        if (!isNullOrEmpty(results) && results.stream().anyMatch(result -> !result.isSuccessful())) {
            try {
                Message slackMessage = buildSlackMessage(getMessageText(results, hardFail));
                slackClient.sendInstantMessage(slackMessage);
            } catch (SlackClientException e) {
                throw new ExtractionCheckNotificationSenderException("Error sending Slack notification: " + e.getMessage(), e);
            }
        }
    }

    private String getMessageText(List<CliCheckResult> failures, boolean hardFail) {
        StringBuilder sb = new StringBuilder();
        sb.append("*i18n source string checks failed*" + getDoubleNewLines());
        if (hardFail) {
            sb.append("The following checks had hard failures:" + System.lineSeparator() +
                    getCheckerHardFailures(failures).map(failure -> "*" + failure.getCheckName() + "*").collect(Collectors.joining(System.lineSeparator())));
        }
        sb.append(getDoubleNewLines());
        sb.append("*" + "Failed checks:" + "*" + getDoubleNewLines());
        sb.append(failures.stream().map(check -> "*" + check.getCheckName() + "*" + getDoubleNewLines() + check.getNotificationText() + getDoubleNewLines()).collect(Collectors.joining(System.lineSeparator())));
        sb.append(getDoubleNewLines() + "*" + "Please correct the above issues in a new commit." + "*");
        String message = getFormattedNotificationMessage(messageTemplate, "baseMessage", replaceQuoteMarkers(appendHardFailureMessage(hardFail, sb)));
        return message;
    }

    @Override
    public void sendChecksSkippedNotification() {
        if (!Strings.isNullOrEmpty(checksSkippedMessage)) {
            try {
                slackClient.sendInstantMessage(buildSlackMessage(checksSkippedMessage));
            } catch (SlackClientException e) {
                throw new ExtractionCheckNotificationSenderException("Error sending Slack notification: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public String replaceQuoteMarkers(String message) {
        return message.replaceAll(QUOTE_MARKER, "`");
    }

    private Message buildSlackMessage(String message) throws SlackClientException {
        Message slackMessage = new Message();
        if (Strings.isNullOrEmpty(username)) {
            throw new ExtractionCheckNotificationSenderException("Username cannot be empty for a Slack notification");
        }
        slackMessage.setChannel(slackChannels.getSlackChannelForDirectOrBotMessage(useDirectMessage, username, userEmailPattern));
        Attachment attachment = new Attachment();
        attachment.setText(message);
        attachment.setColor(COLOR_WARNING);
        attachment.getMrkdwnIn().add(MRKDWNIN_TEXT);
        slackMessage.getAttachments().add(attachment);
        return slackMessage;
    }
}
