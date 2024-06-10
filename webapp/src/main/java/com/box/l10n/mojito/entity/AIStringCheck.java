package com.box.l10n.mojito.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import org.springframework.data.annotation.CreatedDate;

@Entity
@Table(name = "ai_string_check")
public class AIStringCheck extends BaseEntity {

  @Column(name = "repository_id")
  private Long repositoryId;

  @Column(name = "prompt_id")
  private Long aiPromptId;

  @Column(name = "content")
  private String content;

  @Column(name = "comment")
  private String comment;

  @Column(name = "prompt_output")
  private String promptOutput;

  @CreatedDate
  @Column(name = "created_date")
  protected ZonedDateTime createdDate;

  @Column(name = "name")
  private String stringName;

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
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

  public String getPromptOutput() {
    return promptOutput;
  }

  public void setPromptOutput(String promptOutput) {
    this.promptOutput = promptOutput;
  }

  public long getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(long repositoryId) {
    this.repositoryId = repositoryId;
  }

  public void setAiPromptId(Long aiPromptId) {
    this.aiPromptId = aiPromptId;
  }

  public String getStringName() {
    return stringName;
  }

  public void setStringName(String stringName) {
    this.stringName = stringName;
  }

  public void setRepositoryId(Long repositoryId) {
    this.repositoryId = repositoryId;
  }
}
