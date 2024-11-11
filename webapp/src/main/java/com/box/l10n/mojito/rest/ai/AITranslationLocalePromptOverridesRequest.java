package com.box.l10n.mojito.rest.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AITranslationLocalePromptOverridesRequest {

  private String repositoryName;
  private Set<String> locales;
  private Long aiPromptId;
  private boolean disabled;

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
