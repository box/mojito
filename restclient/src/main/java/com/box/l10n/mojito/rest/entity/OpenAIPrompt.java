package com.box.l10n.mojito.rest.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAIPrompt {

  private String systemPrompt;
  private String userPrompt;
  private String modelName;
  private float promptTemperature;
  private boolean deleted;
  private List<String> repositoryName;
  private String promptType;

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

  public String getModelName() {
    return modelName;
  }

  public void setModelName(String modelName) {
    this.modelName = modelName;
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

  public String getPromptType() {
    return promptType;
  }

  public void setPromptType(String promptType) {
    this.promptType = promptType;
  }

  public List<String> getRepositoryName() {
    return repositoryName;
  }

  public void setRepositoryName(List<String> repositoryName) {
    this.repositoryName = repositoryName;
  }
}
