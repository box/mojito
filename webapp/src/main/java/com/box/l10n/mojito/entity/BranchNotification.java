package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.rest.View;
import com.fasterxml.jackson.annotation.JsonView;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * @author jeanaurambault
 */
@Entity
@Table(
        name = "branch_notification",
        indexes = {
                @Index(name = "UK__BRANCH_NOTIFICATION__BRANCH_ID__SENDER_TYPE", columnList = "branch_id, sender_type", unique = true),
        }
)
public class BranchNotification extends BaseEntity {

    @JsonView(View.BranchStatistic.class)
    @ManyToOne(optional = false)
    @JoinColumn(name = "branch_id", foreignKey = @ForeignKey(name = "FK__BRANCH_NOTIFICATION__BRANCH__ID"))
    private Branch branch;

    @Column(name = "new_msg_sent_at")
    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    DateTime newMsgSentAt;

    @Column(name = "updated_msg_sent_at")
    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    DateTime updatedMsgSentAt;

    @Column(name = "screenshot_missing_msg_sent_at")
    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    DateTime screenshotMissingMsgSentAt;

    @Column(name = "translated_msg_sent_at")
    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    DateTime translatedMsgSentAt;

    @Column(name = "content_md5")
    String contentMD5;

    @Column(name = "message_id")
    String messageId;

    @Column(name = "sender_type")
    String senderType;

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public DateTime getNewMsgSentAt() {
        return newMsgSentAt;
    }

    public void setNewMsgSentAt(DateTime newMsgSentAt) {
        this.newMsgSentAt = newMsgSentAt;
    }

    public DateTime getUpdatedMsgSentAt() {
        return updatedMsgSentAt;
    }

    public void setUpdatedMsgSentAt(DateTime updatedMsgSentAt) {
        this.updatedMsgSentAt = updatedMsgSentAt;
    }

    public DateTime getScreenshotMissingMsgSentAt() {
        return screenshotMissingMsgSentAt;
    }

    public void setScreenshotMissingMsgSentAt(DateTime screenshotMissingMsgSentAt) {
        this.screenshotMissingMsgSentAt = screenshotMissingMsgSentAt;
    }

    public DateTime getTranslatedMsgSentAt() {
        return translatedMsgSentAt;
    }

    public void setTranslatedMsgSentAt(DateTime translatedMsgSentAt) {
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
}
