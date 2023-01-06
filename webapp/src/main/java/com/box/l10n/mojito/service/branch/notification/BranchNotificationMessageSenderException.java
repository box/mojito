package com.box.l10n.mojito.service.branch.notification;

public class BranchNotificationMessageSenderException extends Throwable {
  public BranchNotificationMessageSenderException(Exception e) {
    super(e);
  }

  public BranchNotificationMessageSenderException(String message) {
    super(message);
  }
}
