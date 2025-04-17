package com.box.l10n.mojito.service.branch.notification;

import java.util.List;

public interface BranchNotificationMessageSender {
  String sendNewMessage(String branchName, String username, List<String> sourceStrings)
      throws BranchNotificationMessageSenderException;

  String sendUpdatedMessage(
      String branchName, String username, String messageId, List<String> sourceStrings)
      throws BranchNotificationMessageSenderException;

  void sendTranslatedMessage(
      String branchName, String username, String messageId, String safeI18NCommit)
      throws BranchNotificationMessageSenderException;

  void sendScreenshotMissingMessage(String branchName, String messageId, String username)
      throws BranchNotificationMessageSenderException;

  String getId();

  boolean isUserAllowed(String username);
}
