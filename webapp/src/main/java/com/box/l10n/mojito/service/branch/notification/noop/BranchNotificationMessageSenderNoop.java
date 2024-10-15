package com.box.l10n.mojito.service.branch.notification.noop;

import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.service.branch.notification.BranchNotificationMessageSender;
import com.box.l10n.mojito.service.branch.notification.BranchNotificationMessageSenderException;
import java.util.List;
import org.slf4j.Logger;

public class BranchNotificationMessageSenderNoop implements BranchNotificationMessageSender {

  /** logger */
  static Logger logger = getLogger(BranchNotificationMessageSenderNoop.class);

  String id;

  public BranchNotificationMessageSenderNoop(String id) {
    this.id = id;
  }

  @Override
  public String sendNewMessage(String branchName, String username, List<String> sourceStrings)
      throws BranchNotificationMessageSenderException {
    logger.debug("noop sendNewMessage to: {}", username);
    return "noop-message-id";
  }

  @Override
  public String sendUpdatedMessage(
      String branchName, String username, String messageId, List<String> sourceStrings)
      throws BranchNotificationMessageSenderException {
    logger.debug("noop sendUpdatedMessage to: {}", username);
    return "noop-message-id";
  }

  @Override
  public void sendTranslatedMessage(String branchName, String username, String messageId)
      throws BranchNotificationMessageSenderException {
    logger.debug("noop sendTranslatedMessage to: {}", username);
  }

  @Override
  public void sendScreenshotMissingMessage(String branchName, String username, String messageId)
      throws BranchNotificationMessageSenderException {
    logger.debug("noop sendScreenshotMissingMessage to: {}", username);
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
