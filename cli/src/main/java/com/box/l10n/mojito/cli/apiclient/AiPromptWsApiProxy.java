package com.box.l10n.mojito.cli.apiclient;

import com.box.l10n.mojito.cli.model.AIPromptContextMessageCreateRequest;
import com.box.l10n.mojito.cli.model.AIPromptCreateRequest;
import com.box.l10n.mojito.cli.model.AITranslationLocalePromptOverridesRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AiPromptWsApiProxy {
  /** logger */
  static Logger logger = LoggerFactory.getLogger(AiPromptWsApiProxy.class);

  @Autowired private AiPromptWsApi aiPromptClient;

  public String deleteRepositoryLocalePromptOverrides(
      AITranslationLocalePromptOverridesRequest body) {
    logger.debug("Received request to delete repository locale prompt overrides");
    return this.aiPromptClient.deleteRepositoryLocalePromptOverrides(body);
  }

  public String createOrUpdateRepositoryLocalePromptOverrides(
      AITranslationLocalePromptOverridesRequest body) {
    logger.debug("Received request to create or update repository locale prompt overrides");
    return this.aiPromptClient.createOrUpdateRepositoryLocalePromptOverrides(body);
  }

  public void addPromptToRepository(Long promptId, String repositoryName, String promptType) {
    logger.debug("Received request to add prompt id {} to {} repository", promptId, repositoryName);
    this.aiPromptClient.addPromptToRepository(promptId, repositoryName, promptType);
  }

  public Long createPrompt(AIPromptCreateRequest body) {
    logger.debug("Received request to create prompt");
    return this.aiPromptClient.createPrompt(body);
  }

  public Long createPromptMessage(AIPromptContextMessageCreateRequest body) {
    logger.debug("Received request to create prompt context message");
    return this.aiPromptClient.createPromptMessage(body);
  }

  public void deletePrompt(Long promptId) {
    logger.debug("Received request to delete prompt id {}", promptId);
    this.aiPromptClient.deletePrompt(promptId);
  }

  public void deletePromptMessage(Long contextMessageId) {
    logger.debug("Received request to delete prompt message id {}", contextMessageId);
    this.aiPromptClient.deletePromptMessage(contextMessageId);
  }
}
