package com.box.l10n.mojito.service.branch.notification.github;

import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.github.GithubClient;
import com.box.l10n.mojito.service.branch.notification.BranchNotificationMessageSender;
import com.box.l10n.mojito.service.branch.notification.BranchNotificationMessageSenderException;
import java.util.List;
import org.slf4j.Logger;

public class BranchNotificationMessageSenderGithub implements BranchNotificationMessageSender {

  /** logger */
  static Logger logger = getLogger(BranchNotificationMessageSenderGithub.class);

  String id;

  GithubClient githubClient;

  public BranchNotificationMessageSenderGithub(String id, GithubClient githubClient) {
    this.id = id;
    this.githubClient = githubClient;
  }

  @Override
  public String sendNewMessage(String branchName, String username, List<String> sourceStrings)
      throws BranchNotificationMessageSenderException {
    logger.info("sendNewMessage");
    return null;
  }

  @Override
  public String sendUpdatedMessage(
      String branchName, String username, String messageId, List<String> sourceStrings)
      throws BranchNotificationMessageSenderException {
    logger.info("sendUpdatedMessage");
    return null;
  }

  @Override
  public void sendTranslatedMessage(String branchName, String username, String messageId)
      throws BranchNotificationMessageSenderException {
    logger.info("sendTranslatedMessage");
  }

  @Override
  public void sendScreenshotMissingMessage(String branchName, String messageId, String username)
      throws BranchNotificationMessageSenderException {
    logger.info("sendScreenshotMissingMessage");
  }

  @Override
  public String getId() {
    return id;
  }
}
