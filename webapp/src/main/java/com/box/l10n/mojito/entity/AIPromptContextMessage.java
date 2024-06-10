package com.box.l10n.mojito.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

@Entity
@Table(name = "ai_prompt_context_message")
public class AIPromptContextMessage extends BaseEntity {

  @Column(name = "content")
  String content;

  @Column(name = "message_type")
  String messageType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ai_prompt_id")
  AIPrompt aiPrompt;

  @Column(name = "order_index")
  Integer orderIndex;

  @Column(name = "deleted")
  boolean deleted;

  @CreatedDate
  @Column(name = "created_date")
  ZonedDateTime createdDate;

  @LastModifiedDate
  @Column(name = "last_modified_date")
  ZonedDateTime lastModifiedDate;

  public AIPrompt getAiPrompt() {
    return aiPrompt;
  }

  public void setAiPrompt(AIPrompt aiPrompt) {
    this.aiPrompt = aiPrompt;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public ZonedDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(ZonedDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public String getMessageType() {
    return messageType;
  }

  public void setMessageType(String messageType) {
    this.messageType = messageType;
  }

  public Integer getOrderIndex() {
    return orderIndex;
  }

  public void setOrderIndex(Integer orderIndex) {
    this.orderIndex = orderIndex;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  public ZonedDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(ZonedDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }
}
