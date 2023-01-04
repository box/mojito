package com.box.l10n.mojito.service.branch.notification.job;

public class BranchNotificationMissingScreenshotsJobInput {
  Long branchId;

  String senderType;
  String notifierId;

  public Long getBranchId() {
    return branchId;
  }

  public void setBranchId(Long branchId) {
    this.branchId = branchId;
  }

  public String getSenderType() {
    return senderType;
  }

  public void setSenderType(String senderType) {
    this.senderType = senderType;
  }

  public String getNotifierId() {
    return notifierId;
  }

  public void setNotifierId(String notifierId) {
    this.notifierId = notifierId;
  }
}
