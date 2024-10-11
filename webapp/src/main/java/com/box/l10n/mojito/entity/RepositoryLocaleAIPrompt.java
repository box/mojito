package com.box.l10n.mojito.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "repository_locale_ai_prompt",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "UK__REPOSITORY_LOCALE_AI_PROMPT__REPO_ID__LOCALE_ID__AI_PROMPT",
          columnNames = {"repository_id", "locale_id", "ai_prompt_id"})
    })
public class RepositoryLocaleAIPrompt extends BaseEntity {

  @ManyToOne
  @JoinColumn(name = "repository_id", nullable = false)
  private Repository repository;

  @ManyToOne
  @JoinColumn(name = "locale_id", nullable = true)
  private Locale locale;

  @ManyToOne
  @JoinColumn(name = "ai_prompt_id", nullable = false)
  private AIPrompt aiPrompt;

  @Column(name = "disabled")
  private boolean disabled;

  public AIPrompt getAiPrompt() {
    return aiPrompt;
  }

  public void setAiPrompt(AIPrompt aiPrompt) {
    this.aiPrompt = aiPrompt;
  }

  public Repository getRepository() {
    return repository;
  }

  public void setRepository(Repository repository) {
    this.repository = repository;
  }

  public Locale getLocale() {
    return locale;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  public boolean isDisabled() {
    return disabled;
  }

  public void setDisabled(boolean disabled) {
    this.disabled = disabled;
  }
}
