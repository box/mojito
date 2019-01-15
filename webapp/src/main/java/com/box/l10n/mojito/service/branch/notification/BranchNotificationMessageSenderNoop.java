package com.box.l10n.mojito.service.branch.notification;

import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

@ConditionalOnProperty(value = "l10n.branchNotification.type", havingValue = "none", matchIfMissing = true)
@Component
public class BranchNotificationMessageSenderNoop implements BranchNotificationMessageSender {

    /**
     * logger
     */
    static Logger logger = getLogger(BranchNotificationMessageSenderNoop.class);

    @Override
    public String sendNewMessage(String branchName, String username, List<String> sourceStrings) throws BranchNotificationMessageSenderException {
        logger.debug("noop sendNewMessage to: {}", username);
        return "noop-message-id";
    }

    @Override
    public String sendUpdatedMessage(String branchName, String username, String messageId, List<String> sourceStrings) throws BranchNotificationMessageSenderException {
        logger.debug("noop sendUpdatedMessage to: {}", username);
        return "noop-message-id";
    }

    @Override
    public void sendTranslatedMessage(String username, String messageId) throws BranchNotificationMessageSenderException {
        logger.debug("noop sendTranslatedMessage to: {}", username);
    }

    @Override
    public void sendScreenshotMissingMessage(String username, String messageId) throws BranchNotificationMessageSenderException {
        logger.debug("noop sendScreenshotMissingMessage to: {}", username);
    }
}
