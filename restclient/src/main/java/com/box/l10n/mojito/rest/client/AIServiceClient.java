package com.box.l10n.mojito.rest.client;

import com.box.l10n.mojito.rest.entity.AICheckRequest;
import com.box.l10n.mojito.rest.entity.AICheckResponse;
import com.box.l10n.mojito.rest.entity.AIPromptContextMessageCreateRequest;
import com.box.l10n.mojito.rest.entity.AIPromptCreateRequest;
import com.box.l10n.mojito.rest.entity.AITranslationLocalePromptOverridesRequest;
import com.box.l10n.mojito.rest.entity.OpenAIPrompt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class AIServiceClient extends BaseClient {

  static Logger logger = LoggerFactory.getLogger(AIServiceClient.class);

  @Override
  public String getEntityName() {
    return "ai";
  }

  public AICheckResponse executeAIChecks(AICheckRequest AICheckRequest) {
    logger.debug("Received request to execute AI checks");
    return authenticatedRestTemplate.postForObject(
        getBasePathForEntity() + "/checks", AICheckRequest, AICheckResponse.class);
  }

  public Long createPrompt(AIPromptCreateRequest AIPromptCreateRequest) {
    logger.debug("Received request to create prompt");
    return authenticatedRestTemplate.postForObject(
        getBasePathForEntity() + "/prompts", AIPromptCreateRequest, Long.class);
  }

  public void deletePrompt(Long promptId) {
    logger.debug("Received request to delete prompt id {}", promptId);
    authenticatedRestTemplate.delete(getBasePathForEntity() + "/prompts/" + promptId);
  }

  public OpenAIPrompt getPrompt(Long promptId) {
    logger.debug("Received request to get prompt id {}", promptId);
    return authenticatedRestTemplate.getForObject(
        getBasePathForEntity() + "/prompts/" + promptId, OpenAIPrompt.class);
  }

  public Long createPromptContextMessage(
      AIPromptContextMessageCreateRequest AIPromptContextMessageCreateRequest) {
    logger.debug("Received request to create prompt context message");
    return authenticatedRestTemplate.postForObject(
        getBasePathForEntity() + "/prompts/contextMessage",
        AIPromptContextMessageCreateRequest,
        Long.class);
  }

  public void deletePromptContextMessage(Long contextMessageId) {
    logger.debug("Received request to delete prompt message id {}", contextMessageId);
    authenticatedRestTemplate.delete(
        getBasePathForEntity() + "/prompts/contextMessage/" + contextMessageId);
  }

  public void addPromptToRepository(Long promptId, String repositoryName, String promptType) {
    logger.debug("Received request to add prompt id {} to {} repository", promptId, repositoryName);
    authenticatedRestTemplate.postForObject(
        getBasePathForEntity() + "/prompts/" + promptId + "/" + repositoryName + "/" + promptType,
        null,
        Void.class);
  }

  public void createOrUpdateRepositoryLocalePromptOverrides(
      AITranslationLocalePromptOverridesRequest aiTranslationLocalePromptOverridesRequest) {
    logger.debug("Received request to create or update repository locale prompt overrides");
    authenticatedRestTemplate.postForObject(
        getBasePathForEntity() + "/prompts/translation/locale/overrides",
        aiTranslationLocalePromptOverridesRequest,
        Void.class);
  }

  public void deleteRepositoryLocalePromptOverrides(
      AITranslationLocalePromptOverridesRequest aiTranslationLocalePromptOverridesRequest) {
    logger.debug("Received request to delete repository locale prompt overrides");
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<AITranslationLocalePromptOverridesRequest> entity =
        new HttpEntity<>(aiTranslationLocalePromptOverridesRequest, headers);
    authenticatedRestTemplate.deleteForObject(
        getBasePathForEntity() + "/prompts/translation/locale/overrides", entity, Void.class);
  }
}
