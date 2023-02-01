package com.box.l10n.mojito.service.branch.notification.github;

import static com.box.l10n.mojito.service.branch.notification.github.PRLabel.TRANSLATIONS_READY;
import static com.box.l10n.mojito.service.branch.notification.github.PRLabel.TRANSLATIONS_REQUIRED;
import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.github.GithubClient;
import com.box.l10n.mojito.github.GithubException;
import com.box.l10n.mojito.service.branch.notification.BranchNotificationMessageSender;
import com.box.l10n.mojito.service.branch.notification.BranchNotificationMessageSenderException;
import java.util.List;
import org.slf4j.Logger;

public class BranchNotificationMessageSenderGithub implements BranchNotificationMessageSender {

  /** logger */
  static Logger logger = getLogger(BranchNotificationMessageSenderGithub.class);

  String id;

  GithubClient githubClient;

  BranchNotificationMessageBuilderGithub branchNotificationMessageBuilderGithub;

  public BranchNotificationMessageSenderGithub(
      String id,
      GithubClient githubClient,
      BranchNotificationMessageBuilderGithub branchNotificationMessageBuilderGithub) {
    this.id = id;
    this.githubClient = githubClient;
    this.branchNotificationMessageBuilderGithub = branchNotificationMessageBuilderGithub;
  }

  @Override
  public String sendNewMessage(String branchName, String username, List<String> sourceStrings)
      throws BranchNotificationMessageSenderException {
    logger.debug("sendNewMessage to: {}", "https://github.com/" + branchName);

    try {
      GithubBranchDetails branchDetails = new GithubBranchDetails(branchName);
      githubClient.addCommentToPR(
          branchDetails.getRepository(),
          branchDetails.getPrNumber(),
          branchNotificationMessageBuilderGithub.getNewMessage(branchName, sourceStrings));
      updatePRLabel(githubClient, branchDetails, TRANSLATIONS_REQUIRED);
      return null;
    } catch (GithubException e) {
      throw new BranchNotificationMessageSenderException(e);
    }
  }

  @Override
  public String sendUpdatedMessage(
      String branchName, String username, String messageId, List<String> sourceStrings)
      throws BranchNotificationMessageSenderException {
    logger.debug("sendNewMessage to: {}", "https://github.com/" + branchName);
    try {
      GithubBranchDetails branchDetails = new GithubBranchDetails(branchName);
      githubClient.addCommentToPR(
          branchDetails.getRepository(),
          branchDetails.getPrNumber(),
          branchNotificationMessageBuilderGithub.getUpdatedMessage(branchName, sourceStrings));
      updatePRLabel(githubClient, branchDetails, TRANSLATIONS_REQUIRED);
      return null;
    } catch (GithubException e) {
      throw new BranchNotificationMessageSenderException(e);
    }
  }

  @Override
  public void sendTranslatedMessage(String branchName, String username, String messageId)
      throws BranchNotificationMessageSenderException {
    logger.debug("sendTranslatedMessage to: {}", "https://github.com/" + branchName);

    try {
      GithubBranchDetails branchDetails = new GithubBranchDetails(branchName);
      githubClient.addCommentToPR(
          branchDetails.getRepository(),
          branchDetails.getPrNumber(),
          branchNotificationMessageBuilderGithub.getTranslatedMessage());
      updatePRLabel(githubClient, branchDetails, TRANSLATIONS_READY);
    } catch (GithubException e) {
      throw new BranchNotificationMessageSenderException(e);
    }
  }

  @Override
  public void sendScreenshotMissingMessage(String branchName, String messageId, String username)
      throws BranchNotificationMessageSenderException {
    logger.debug("sendScreenshotMissingMessage to: {}", "https://github.com/" + branchName);

    try {
      GithubBranchDetails branchDetails = new GithubBranchDetails(branchName);
      githubClient.addCommentToPR(
          branchDetails.getRepository(),
          branchDetails.getPrNumber(),
          branchNotificationMessageBuilderGithub.getScreenshotMissingMessage());
    } catch (GithubException e) {
      throw new BranchNotificationMessageSenderException(e);
    }
  }

  @Override
  public String getId() {
    return id;
  }

  private void updatePRLabel(
      GithubClient githubClient, GithubBranchDetails branchDetails, PRLabel label) {
    String oppositeLabel =
        label == TRANSLATIONS_READY
            ? TRANSLATIONS_REQUIRED.toString()
            : TRANSLATIONS_READY.toString();
    if (githubClient.isLabelAppliedToPR(
        branchDetails.getRepository(), branchDetails.getPrNumber(), oppositeLabel)) {
      githubClient.removeLabelFromPR(
          branchDetails.getRepository(), branchDetails.getPrNumber(), oppositeLabel);
    }
    githubClient.addLabelToPR(
        branchDetails.getRepository(), branchDetails.getPrNumber(), label.toString());
  }
}
