package com.box.l10n.mojito.rest.entity;

import java.util.Set;

public class AITranslationLocalePromptOverridesRequest {

  private String repositoryName;
  private Set<String> locales;
  private Long aiPromptId;
  private boolean disabled;

  public AITranslationLocalePromptOverridesRequest(
      String repositoryName, Set<String> locales, Long aiPromptId, boolean disabled) {
    this.repositoryName = repositoryName;
    this.locales = locales;
    this.aiPromptId = aiPromptId;
    this.disabled = disabled;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public void setRepositoryName(String repositoryName) {
    this.repositoryName = repositoryName;
  }

  public Set<String> getLocales() {
    return locales;
  }

  public void setLocales(Set<String> locales) {
    this.locales = locales;
  }

  public Long getAiPromptId() {
    return aiPromptId;
  }

  public void setAiPromptId(Long aiPromptId) {
    this.aiPromptId = aiPromptId;
  }

  public boolean isDisabled() {
    return disabled;
  }

  public void setDisabled(boolean disabled) {
    this.disabled = disabled;
  }
}
