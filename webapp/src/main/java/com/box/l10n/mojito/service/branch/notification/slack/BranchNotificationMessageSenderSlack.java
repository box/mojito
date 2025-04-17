package com.box.l10n.mojito.service.branch.notification.slack;

import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.service.branch.notification.BranchNotificationMessageSender;
import com.box.l10n.mojito.service.branch.notification.BranchNotificationMessageSenderException;
import com.box.l10n.mojito.slack.SlackClient;
import com.box.l10n.mojito.slack.SlackClientException;
import com.box.l10n.mojito.slack.request.Message;
import com.box.l10n.mojito.slack.response.ChatPostMessageResponse;
import com.box.l10n.mojito.thirdpartynotification.slack.SlackChannels;
import com.google.common.base.Preconditions;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;

public class BranchNotificationMessageSenderSlack implements BranchNotificationMessageSender {

  /** logger */
  static Logger logger = getLogger(BranchNotificationMessageSenderSlack.class);

  String id;

  SlackClient slackClient;

  SlackChannels slackChannels;
  BranchNotificationMessageBuilderSlack branchNotificationMessageBuilderSlack;

  String userEmailPattern;
  boolean useDirectMessage;
  boolean githubPR;

  List<Pattern> blockedUserPatterns;

  public BranchNotificationMessageSenderSlack(
      String id,
      SlackClient slackClient,
      SlackChannels slackChannels,
      BranchNotificationMessageBuilderSlack branchNotificationMessageBuilderSlack,
      String userEmailPattern,
      boolean useDirectMessage,
      boolean isGithubPR,
      List<Pattern> blockedUserPatterns) {
    this.id = id;
    this.slackClient = Preconditions.checkNotNull(slackClient);
    this.slackChannels = Preconditions.checkNotNull(slackChannels);
    this.branchNotificationMessageBuilderSlack =
        Preconditions.checkNotNull(branchNotificationMessageBuilderSlack);
    this.userEmailPattern = Preconditions.checkNotNull(userEmailPattern);
    this.useDirectMessage = useDirectMessage;
    this.githubPR = isGithubPR;
    this.blockedUserPatterns = blockedUserPatterns;
  }

  @Override
  public String sendNewMessage(String branchName, String username, List<String> sourceStrings)
      throws BranchNotificationMessageSenderException {
    logger.debug("sendNewMessage to: {}", username);

    try {
      Message message =
          branchNotificationMessageBuilderSlack.getNewMessage(
              slackChannels.getSlackChannelForDirectOrBotMessage(
                  useDirectMessage, username, userEmailPattern),
              branchName,
              sourceStrings);

      ChatPostMessageResponse chatPostMessageResponse = slackClient.sendInstantMessage(message);
      return chatPostMessageResponse.getTs();
    } catch (SlackClientException sce) {
      throw new BranchNotificationMessageSenderException(sce);
    }
  }

  @Override
  public String sendUpdatedMessage(
      String branchName, String username, String messageId, List<String> sourceStrings)
      throws BranchNotificationMessageSenderException {
    logger.debug("sendUpdatedMessage to: {}", username);

    try {
      Message message =
          branchNotificationMessageBuilderSlack.getUpdatedMessage(
              slackChannels.getSlackChannelForDirectOrBotMessage(
                  useDirectMessage, username, userEmailPattern),
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
  public void sendTranslatedMessage(
      String branchName, String username, String messageId, String safeI18NCommit)
      throws BranchNotificationMessageSenderException {
    logger.debug("sendTranslatedMessage to: {}", username);

    try {
      Message message =
          branchNotificationMessageBuilderSlack.getTranslatedMessage(
              slackChannels.getSlackChannelForDirectOrBotMessage(
                  useDirectMessage, username, userEmailPattern),
              messageId,
              branchName,
              safeI18NCommit);

      ChatPostMessageResponse chatPostMessageResponse = slackClient.sendInstantMessage(message);
    } catch (SlackClientException sce) {
      throw new BranchNotificationMessageSenderException(sce);
    }
  }

  @Override
  public void sendScreenshotMissingMessage(String branchName, String messageId, String username)
      throws BranchNotificationMessageSenderException {
    logger.debug("sendScreenshotMissingMessage to: {}", username);

    try {
      Message message =
          branchNotificationMessageBuilderSlack.getScreenshotMissingMessage(
              slackChannels.getSlackChannelForDirectOrBotMessage(
                  useDirectMessage, username, userEmailPattern),
              messageId);

      ChatPostMessageResponse chatPostMessageResponse = slackClient.sendInstantMessage(message);
    } catch (SlackClientException sce) {
      throw new BranchNotificationMessageSenderException(sce);
    }
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public boolean isUserAllowed(String username) {
    for (Pattern blockedUserPattern : blockedUserPatterns) {
      if (blockedUserPattern.matcher(username).matches()) {
        return false;
      }
    }
    return true;
  }
}
