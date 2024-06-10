package com.box.l10n.mojito.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "repository_ai_prompt",
    indexes = {
      @Index(
          name = "I__REPOSITORY_AI_PROMPT__REPO_ID__TYPE_ID__PROMPT_ID",
          columnList = "repository_id, prompt_type_id, ai_prompt_id")
    })
public class RepositoryAIPrompt extends BaseEntity {

  @Column(name = "repository_id")
  private Long repositoryId;

  @Column(name = "ai_prompt_id")
  private Long aiPromptId;

  @Column(name = "prompt_type_id")
  private Long promptTypeId;

  public Long getAiPromptId() {
    return aiPromptId;
  }

  public void setAiPromptId(Long aiPromptId) {
    this.aiPromptId = aiPromptId;
  }

  public long getPromptTypeId() {
    return promptTypeId;
  }

  public void setPromptTypeId(Long promptTypeId) {
    this.promptTypeId = promptTypeId;
  }

  public long getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(long repositoryId) {
    this.repositoryId = repositoryId;
  }
}
