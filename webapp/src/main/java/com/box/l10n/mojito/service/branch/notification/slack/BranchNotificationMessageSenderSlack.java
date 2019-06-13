package com.box.l10n.mojito.service.branch.notification.slack;

import com.box.l10n.mojito.service.branch.notification.BranchNotificationMessageSender;
import com.box.l10n.mojito.service.branch.notification.BranchNotificationMessageSenderException;
import com.box.l10n.mojito.slack.SlackClient;
import com.box.l10n.mojito.slack.SlackClientException;
import com.box.l10n.mojito.slack.request.Message;
import com.box.l10n.mojito.slack.response.ChatPostMessageResponse;
import com.ibm.icu.text.MessageFormat;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

@ConditionalOnProperty(value = "l10n.branchNotification.slack.enabled", havingValue = "true")
@Component
public class BranchNotificationMessageSenderSlack implements BranchNotificationMessageSender {

    /**
     * logger
     */
    static Logger logger = getLogger(BranchNotificationMessageSenderSlack.class);

    @Autowired
    SlackClient slackClient;

    @Autowired
    BranchNotificationMessageBuilderSlack branchNotificationMessageBuilderSlack;

    @Value("${l10n.branchNotification.slack.userEmailPattern}")
    String userEmailPattern;

    @Override
    public String sendNewMessage(String branchName, String username, List<String> sourceStrings) throws BranchNotificationMessageSenderException {
        logger.debug("sendNewMessage to: {}", username);

        try {
            Message message = branchNotificationMessageBuilderSlack.getNewMessage(
                    getSlackChannelForBranch(username),
                    branchName,
                    sourceStrings);

            ChatPostMessageResponse chatPostMessageResponse = slackClient.sendInstantMessage(message);
            return chatPostMessageResponse.getTs();
        } catch (SlackClientException sce) {
            throw new BranchNotificationMessageSenderException(sce);
        }
    }

    @Override
    public String sendUpdatedMessage(String branchName, String username, String messageId, List<String> sourceStrings) throws BranchNotificationMessageSenderException {
        logger.debug("sendUpdatedMessage to: {}", username);

        try {
            Message message = branchNotificationMessageBuilderSlack.getUpdatedMessage(
                    getSlackChannelForBranch(username),
                    messageId,
                    branchName,
                    sourceStrings);

            ChatPostMessageResponse chatPostMessageResponse = slackClient.sendInstantMessage(message);
            return chatPostMessageResponse.getTs();
        } catch (SlackClientException sce) {
            throw new BranchNotificationMessageSenderException(sce);
        }
    }

    @Override
    public void sendTranslatedMessage(String branchName, String username, String messageId) throws BranchNotificationMessageSenderException {
        logger.debug("sendTranslatedMessage to: {}", username);

        try {
            Message message = branchNotificationMessageBuilderSlack.getTranslatedMessage(getSlackChannelForBranch(username), messageId);

            ChatPostMessageResponse chatPostMessageResponse = slackClient.sendInstantMessage(message);
        } catch (SlackClientException sce) {
            throw new BranchNotificationMessageSenderException(sce);
        }
    }

    @Override
    public void sendScreenshotMissingMessage(String branchName, String messageId, String username) throws BranchNotificationMessageSenderException {
        logger.debug("sendScreenshotMissingMessage to: {}", username);

        try {
            Message message = branchNotificationMessageBuilderSlack.getScreenshotMissingMessage(getSlackChannelForBranch(username), messageId);

            ChatPostMessageResponse chatPostMessageResponse = slackClient.sendInstantMessage(message);
        } catch (SlackClientException sce) {
            throw new BranchNotificationMessageSenderException(sce);
        }
    }

    String getSlackChannelForBranch(String username) throws SlackClientException {
        return slackClient.getInstantMessageChannel(getEmail(username)).getId();
    }

    String getEmail(String username) {
        return MessageFormat.format(userEmailPattern, username);
    }
}
