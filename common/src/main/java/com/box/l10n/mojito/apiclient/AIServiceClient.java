package com.box.l10n.mojito.apiclient;

import com.box.l10n.mojito.apiclient.model.AICheckRequest;
import com.box.l10n.mojito.apiclient.model.AICheckResponse;
import com.box.l10n.mojito.apiclient.model.AIPromptContextMessageCreateRequest;
import com.box.l10n.mojito.apiclient.model.AIPromptCreateRequest;
import com.box.l10n.mojito.apiclient.model.AITranslationLocalePromptOverridesRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AIServiceClient {

  static Logger logger = LoggerFactory.getLogger(AIServiceClient.class);

  @Autowired private AiChecksWsApi aiChecksWsApi;

  @Autowired private AiPromptWsApi aiPromptWsApi;

  public AICheckResponse executeAIChecks(AICheckRequest body) {
    logger.debug("Received request to execute AI checks");
    return this.aiChecksWsApi.executeAIChecks(body);
  }

  public String deleteRepositoryLocalePromptOverrides(
      AITranslationLocalePromptOverridesRequest body) {
    logger.debug("Received request to delete repository locale prompt overrides");
    return this.aiPromptWsApi.deleteRepositoryLocalePromptOverrides(body);
  }

  public String createOrUpdateRepositoryLocalePromptOverrides(
      AITranslationLocalePromptOverridesRequest body) {
    logger.debug("Received request to create or update repository locale prompt overrides");
    return this.aiPromptWsApi.createOrUpdateRepositoryLocalePromptOverrides(body);
  }

  public void addPromptToRepository(Long promptId, String repositoryName, String promptType) {
    logger.debug("Received request to add prompt id {} to {} repository", promptId, repositoryName);
    this.aiPromptWsApi.addPromptToRepository(promptId, repositoryName, promptType);
  }

  public Long createPrompt(AIPromptCreateRequest body) {
    logger.debug("Received request to create prompt");
    return this.aiPromptWsApi.createPrompt(body);
  }

  public Long createPromptMessage(AIPromptContextMessageCreateRequest body) {
    logger.debug("Received request to create prompt context message");
    return this.aiPromptWsApi.createPromptMessage(body);
  }

  public void deletePrompt(Long promptId) {
    logger.debug("Received request to delete prompt id {}", promptId);
    this.aiPromptWsApi.deletePrompt(promptId);
  }

  public void deletePromptMessage(Long contextMessageId) {
    logger.debug("Received request to delete prompt message id {}", contextMessageId);
    this.aiPromptWsApi.deletePromptMessage(contextMessageId);
  }
}
