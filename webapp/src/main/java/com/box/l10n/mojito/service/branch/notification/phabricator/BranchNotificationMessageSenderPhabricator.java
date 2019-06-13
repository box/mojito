package com.box.l10n.mojito.service.branch.notification.phabricator;

import com.box.l10n.mojito.phabricator.PhabricatorClient;
import com.box.l10n.mojito.phabricator.PhabricatorClientException;
import com.box.l10n.mojito.service.branch.BranchUrlBuilder;
import com.box.l10n.mojito.service.branch.notification.BranchNotificationMessageSender;
import com.box.l10n.mojito.service.branch.notification.BranchNotificationMessageSenderException;
import com.box.l10n.mojito.utils.ServerConfig;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

@ConditionalOnProperty(value = "l10n.branchNotification.phabricator.enabled", havingValue = "true")
@Component
public class BranchNotificationMessageSenderPhabricator implements BranchNotificationMessageSender {

    /**
     * logger
     */
    static Logger logger = getLogger(BranchNotificationMessageSenderPhabricator.class);

    @Autowired
    PhabricatorClient phabricatorClient;

    /**
     * This should be a phid
     */
    @Value("${l10n.branchNotification.phabricator.reviewer}")
    String reviewer;

    @Autowired
    BranchNotificationMessageBuilderPhabricator branchNotificationMessageBuilderPhabricator;

    @Override
    public String sendNewMessage(String branchName, String username, List<String> sourceStrings) throws BranchNotificationMessageSenderException {
        logger.debug("sendNewMessage to: {}", username);

        try {
            phabricatorClient.addComment(branchName, branchNotificationMessageBuilderPhabricator.getNewMessage(branchName, sourceStrings));
            phabricatorClient.addReviewer(branchName, reviewer, true);
            return null;
        } catch (PhabricatorClientException e) {
            throw new BranchNotificationMessageSenderException(e);
        }
    }

    @Override
    public String sendUpdatedMessage(String branchName, String username, String messageId, List<String> sourceStrings) throws BranchNotificationMessageSenderException {
        logger.debug("sendUpdatedMessage to: {}", username);

        try {
            phabricatorClient.addComment(branchName, branchNotificationMessageBuilderPhabricator.getUpdatedMessage(branchName, sourceStrings));
            phabricatorClient.addReviewer(branchName, reviewer, true);
            return null;
        } catch (PhabricatorClientException e) {
            throw new BranchNotificationMessageSenderException(e);
        }
    }

    @Override
    public void sendTranslatedMessage(String branchName, String username, String messageId) throws BranchNotificationMessageSenderException {
        logger.debug("sendTranslatedMessage to: {}", username);

        try {
            phabricatorClient.addComment(branchName, branchNotificationMessageBuilderPhabricator.getTranslatedMessage());
            phabricatorClient.removeReviewer(branchName, reviewer, true);
        } catch (PhabricatorClientException e) {
            throw new BranchNotificationMessageSenderException(e);
        }
    }

    @Override
    public void sendScreenshotMissingMessage(String branchName, String messageId, String username) throws BranchNotificationMessageSenderException {
        logger.debug("sendScreenshotMissingMessage to: {}", username);

        try {
            phabricatorClient.addComment(branchName, branchNotificationMessageBuilderPhabricator.getScreenshotMissingMessage());
        } catch (PhabricatorClientException e) {
            throw new BranchNotificationMessageSenderException(e);
        }
    }
}
