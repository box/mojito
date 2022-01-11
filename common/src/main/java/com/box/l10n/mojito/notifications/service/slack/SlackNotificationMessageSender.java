package com.box.l10n.mojito.notifications.service.slack;

import com.box.l10n.mojito.notifications.service.NotificationServiceException;
import com.box.l10n.mojito.notifications.service.ThirdPartyNotificationType;
import com.box.l10n.mojito.notifications.service.ThirdPartyNotificationMessageSender;
import com.box.l10n.mojito.slack.request.Attachment;
import com.box.l10n.mojito.slack.request.Message;
import com.box.l10n.mojito.slack.SlackClient;
import com.box.l10n.mojito.slack.SlackClientException;
import com.google.common.collect.ImmutableMap;
import com.ibm.icu.text.MessageFormat;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static com.box.l10n.mojito.notifications.service.slack.SlackUtils.getSlackChannel;
import static com.box.l10n.mojito.slack.SlackClient.COLOR_GOOD;
import static com.box.l10n.mojito.slack.request.Attachment.MRKDWNIN_TEXT;

/**
 * Sends notifications as Slack messages using {@link SlackClient}
 *
 * @author mallen
 */
@ConditionalOnProperty("l10n.notifications.slack.enabled")
@Component
public class SlackNotificationMessageSender extends ThirdPartyNotificationMessageSender {

    @Autowired
    SlackClient slackClient;

    @Value("${l10n.notifications.slack.userEmailPattern}")
    String userEmailPattern;

    @Value("${l10n.notifications.slack.useDirectMessage:false}")
    boolean useDirectMessage;

    @Override
    public void sendMessage(String message, ImmutableMap<String, String> serviceParameters) throws Exception {
        checkParameters(serviceParameters);
        Message slackMessage = buildSlackMessage(message, serviceParameters);
        slackClient.sendInstantMessage(slackMessage);
    }

    private void checkParameters(ImmutableMap<String, String> serviceParameters) throws NotificationServiceException {
        if (!serviceParameters.containsKey(SlackParameters.USERNAME.getParamKey())) {
            throw new NotificationServiceException("Slack username must be present in parameters map if sending Slack notifications.");
        }
    }

    private Message buildSlackMessage(String message, ImmutableMap<String, String> serviceParameters) throws Exception {
        Message slackMessage = new Message();
        slackMessage.setChannel(getSlackChannel(useDirectMessage, serviceParameters.get(SlackParameters.USERNAME.getParamKey()), userEmailPattern, slackClient));
        Attachment attachment = new Attachment();
        attachment.setText(message);
        attachment.setColor(serviceParameters.getOrDefault(SlackParameters.ATTACHMENT_COLOR.getParamKey(), COLOR_GOOD));
        attachment.getMrkdwnIn().add(MRKDWNIN_TEXT);
        slackMessage.getAttachments().add(attachment);
        return slackMessage;
    }

}
