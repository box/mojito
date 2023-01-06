package com.box.l10n.mojito.service.branch.notification.phabricator;

import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.phabricator.DifferentialRevision;
import com.box.l10n.mojito.phabricator.PhabricatorException;
import com.box.l10n.mojito.service.branch.notification.BranchNotificationMessageSender;
import com.box.l10n.mojito.service.branch.notification.BranchNotificationMessageSenderException;
import com.google.common.base.Preconditions;
import java.util.List;
import org.slf4j.Logger;

public class BranchNotificationMessageSenderPhabricator implements BranchNotificationMessageSender {

  /** logger */
  static Logger logger = getLogger(BranchNotificationMessageSenderPhabricator.class);

  String id;

  DifferentialRevision differentialRevision;

  /** This should be a phid */
  String reviewer;

  Boolean blockingReview;

  BranchNotificationMessageBuilderPhabricator branchNotificationMessageBuilderPhabricator;

  public BranchNotificationMessageSenderPhabricator(
      String id,
      DifferentialRevision differentialRevision,
      String reviewer,
      Boolean blockingReview,
      BranchNotificationMessageBuilderPhabricator branchNotificationMessageBuilderPhabricator) {
    this.id = id;
    this.differentialRevision = Preconditions.checkNotNull(differentialRevision);
    this.reviewer = Preconditions.checkNotNull(reviewer);
    this.blockingReview = Preconditions.checkNotNull(blockingReview);
    this.branchNotificationMessageBuilderPhabricator =
        Preconditions.checkNotNull(branchNotificationMessageBuilderPhabricator);
  }

  @Override
  public String sendNewMessage(String branchName, String username, List<String> sourceStrings)
      throws BranchNotificationMessageSenderException {
    logger.debug("sendNewMessage to: {}", username);

    try {
      differentialRevision.addComment(
          branchName,
          branchNotificationMessageBuilderPhabricator.getNewMessage(branchName, sourceStrings));
      differentialRevision.addReviewer(branchName, reviewer, blockingReview);
      return null;
    } catch (PhabricatorException e) {
      throw new BranchNotificationMessageSenderException(e);
    }
  }

  @Override
  public String sendUpdatedMessage(
      String branchName, String username, String messageId, List<String> sourceStrings)
      throws BranchNotificationMessageSenderException {
    logger.debug("sendUpdatedMessage to: {}", username);

    try {
      differentialRevision.addComment(
          branchName,
          branchNotificationMessageBuilderPhabricator.getUpdatedMessage(branchName, sourceStrings));
      differentialRevision.addReviewer(branchName, reviewer, blockingReview);
      return null;
    } catch (PhabricatorException e) {
      throw new BranchNotificationMessageSenderException(e);
    }
  }

  @Override
  public void sendTranslatedMessage(String branchName, String username, String messageId)
      throws BranchNotificationMessageSenderException {
    logger.debug("sendTranslatedMessage to: {}", username);

    try {
      differentialRevision.addComment(
          branchName, branchNotificationMessageBuilderPhabricator.getTranslatedMessage());
      differentialRevision.removeReviewer(branchName, reviewer, blockingReview);
    } catch (PhabricatorException e) {
      throw new BranchNotificationMessageSenderException(e);
    }
  }

  @Override
  public void sendScreenshotMissingMessage(String branchName, String messageId, String username)
      throws BranchNotificationMessageSenderException {
    logger.debug("sendScreenshotMissingMessage to: {}", username);

    try {
      differentialRevision.addComment(
          branchName, branchNotificationMessageBuilderPhabricator.getScreenshotMissingMessage());
    } catch (PhabricatorException e) {
      throw new BranchNotificationMessageSenderException(e);
    }
  }

  @Override
  public String getId() {
    return id;
  }
}
