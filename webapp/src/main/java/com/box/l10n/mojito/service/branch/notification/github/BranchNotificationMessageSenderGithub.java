package com.box.l10n.mojito.service.branch.notification.github;

import static com.box.l10n.mojito.github.PRLabel.TRANSLATIONS_READY;
import static com.box.l10n.mojito.github.PRLabel.TRANSLATIONS_REQUIRED;
import static com.box.l10n.mojito.github.PRLabel.updatePRLabel;
import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.github.GithubClient;
import com.box.l10n.mojito.github.GithubException;
import com.box.l10n.mojito.service.branch.notification.BranchNotificationMessageSender;
import com.box.l10n.mojito.service.branch.notification.BranchNotificationMessageSenderException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.kohsuke.github.GHIssueComment;
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
    logger.debug("sendNewMessage to: {}", githubClient.getEndpoint() + "/" + branchName);

    try {
      GithubBranchDetails branchDetails = new GithubBranchDetails(branchName);
      GHIssueComment addedComment =
          githubClient.addCommentToPR(
              branchDetails.getRepository(),
              branchDetails.getPrNumber(),
              branchNotificationMessageBuilderGithub.getNewMessage(branchName, sourceStrings));
      updatePRLabel(
          githubClient,
          branchDetails.getRepository(),
          branchDetails.getPrNumber(),
          TRANSLATIONS_REQUIRED);
      return Optional.ofNullable(addedComment)
          .map(GHIssueComment::getId)
          .map(Object::toString)
          .orElse("");
    } catch (GithubException e) {
      throw new BranchNotificationMessageSenderException(e);
    }
  }

  @Override
  public String sendUpdatedMessage(
      String branchName, String username, String messageId, List<String> sourceStrings)
      throws BranchNotificationMessageSenderException {
    logger.debug("sendNewMessage to: {}", githubClient.getEndpoint() + "/" + branchName);
    try {
      GithubBranchDetails branchDetails = new GithubBranchDetails(branchName);
      GHIssueComment addedComment =
          githubClient.updateOrAddCommentToPR(
              branchDetails.getRepository(),
              branchDetails.getPrNumber(),
              branchNotificationMessageBuilderGithub.getUpdatedMessage(branchName, sourceStrings),
              String.format(
                  "(%s|%s).*",
                  Pattern.quote(this.branchNotificationMessageBuilderGithub.getNewStringMsg()),
                  Pattern.quote(
                      this.branchNotificationMessageBuilderGithub.getUpdatedStringMsg())));
      updatePRLabel(
          githubClient,
          branchDetails.getRepository(),
          branchDetails.getPrNumber(),
          TRANSLATIONS_REQUIRED);
      return Optional.ofNullable(addedComment)
          .map(GHIssueComment::getId)
          .map(Object::toString)
          .orElse("");
    } catch (GithubException e) {
      throw new BranchNotificationMessageSenderException(e);
    }
  }

  @Override
  public void sendTranslatedMessage(String branchName, String username, String messageId)
      throws BranchNotificationMessageSenderException {
    logger.debug("sendTranslatedMessage to: {}", githubClient.getEndpoint() + "/" + branchName);

    try {
      GithubBranchDetails branchDetails = new GithubBranchDetails(branchName);
      githubClient.addCommentToPR(
          branchDetails.getRepository(),
          branchDetails.getPrNumber(),
          branchNotificationMessageBuilderGithub.getTranslatedMessage(branchName, branchDetails));
      updatePRLabel(
          githubClient,
          branchDetails.getRepository(),
          branchDetails.getPrNumber(),
          TRANSLATIONS_READY);
    } catch (GithubException e) {
      throw new BranchNotificationMessageSenderException(e);
    }
  }

  @Override
  public void sendScreenshotMissingMessage(String branchName, String messageId, String username)
      throws BranchNotificationMessageSenderException {
    logger.debug(
        "sendScreenshotMissingMessage to: {}", githubClient.getEndpoint() + "/" + branchName);

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

  @Override
  public boolean isUserAllowed(String username) {
    return true;
  }
}
