package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;

/**
 * @author jeanaurambault
 */
@Entity
@Table(
    name = "branch_notification",
    indexes = {
      @Index(
          name = "UK__BRANCH_NOTIFICATION__BRANCH_ID__SENDER_TYPE",
          columnList = "branch_id, sender_type",
          unique = true),
    })
public class BranchNotification extends BaseEntity {

  @JsonView(View.BranchStatistic.class)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "branch_id",
      foreignKey = @ForeignKey(name = "FK__BRANCH_NOTIFICATION__BRANCH__ID"))
  private Branch branch;

  @Column(name = "new_msg_sent_at")
  ZonedDateTime newMsgSentAt;

  @Column(name = "updated_msg_sent_at")
  ZonedDateTime updatedMsgSentAt;

  @Column(name = "screenshot_missing_msg_sent_at")
  ZonedDateTime screenshotMissingMsgSentAt;

  @Column(name = "translated_msg_sent_at")
  ZonedDateTime translatedMsgSentAt;

  @Column(name = "content_md5")
  String contentMD5;

  @Column(name = "message_id")
  String messageId;

  @Column(name = "sender_type")
  String senderType;

  @Column(name = "notifier_id")
  String notifierId;

  public Branch getBranch() {
    return branch;
  }

  public void setBranch(Branch branch) {
    this.branch = branch;
  }

  public ZonedDateTime getNewMsgSentAt() {
    return newMsgSentAt;
  }

  public void setNewMsgSentAt(ZonedDateTime newMsgSentAt) {
    this.newMsgSentAt = newMsgSentAt;
  }

  public ZonedDateTime getUpdatedMsgSentAt() {
    return updatedMsgSentAt;
  }

  public void setUpdatedMsgSentAt(ZonedDateTime updatedMsgSentAt) {
    this.updatedMsgSentAt = updatedMsgSentAt;
  }

  public ZonedDateTime getScreenshotMissingMsgSentAt() {
    return screenshotMissingMsgSentAt;
  }

  public void setScreenshotMissingMsgSentAt(ZonedDateTime screenshotMissingMsgSentAt) {
    this.screenshotMissingMsgSentAt = screenshotMissingMsgSentAt;
  }

  public ZonedDateTime getTranslatedMsgSentAt() {
    return translatedMsgSentAt;
  }

  public void setTranslatedMsgSentAt(ZonedDateTime translatedMsgSentAt) {
    this.translatedMsgSentAt = translatedMsgSentAt;
  }

  public String getContentMD5() {
    return contentMD5;
  }

  public void setContentMD5(String contentMD5) {
    this.contentMD5 = contentMD5;
  }

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String messageId) {
    this.messageId = messageId;
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
