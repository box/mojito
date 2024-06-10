package com.box.l10n.mojito.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import java.util.List;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

@Entity
@Table(name = "ai_prompt")
public class AIPrompt extends BaseEntity {

  @Column(name = "system_prompt")
  private String systemPrompt;

  @Column(name = "user_prompt")
  private String userPrompt;

  @Column(name = "model_name")
  private String modelName;

  @Column(name = "prompt_temperature")
  private float promptTemperature;

  @Column(name = "deleted")
  private boolean deleted;

  @CreatedDate
  @Column(name = "created_date")
  private ZonedDateTime createdDate;

  @LastModifiedDate
  @Column(name = "last_modified_date")
  private ZonedDateTime lastModifiedDate;

  @OneToMany(fetch = FetchType.EAGER, mappedBy = "aiPrompt")
  @Where(clause = "deleted = false")
  @OrderBy("orderIndex ASC")
  List<AIPromptContextMessage> contextMessages;

  public String getModelName() {
    return modelName;
  }

  public void setModelName(String modelName) {
    this.modelName = modelName;
  }

  public String getSystemPrompt() {
    return systemPrompt;
  }

  public void setSystemPrompt(String systemPrompt) {
    this.systemPrompt = systemPrompt;
  }

  public String getUserPrompt() {
    return userPrompt;
  }

  public void setUserPrompt(String userPrompt) {
    this.userPrompt = userPrompt;
  }

  public float getPromptTemperature() {
    return promptTemperature;
  }

  public void setPromptTemperature(float promptTemperature) {
    this.promptTemperature = promptTemperature;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  public ZonedDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(ZonedDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public List<AIPromptContextMessage> getContextMessages() {
    return contextMessages;
  }

  public void setContextMessages(List<AIPromptContextMessage> contextMessages) {
    this.contextMessages = contextMessages;
  }

  public ZonedDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(ZonedDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }
}
