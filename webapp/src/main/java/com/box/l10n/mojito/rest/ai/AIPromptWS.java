package com.box.l10n.mojito.rest.ai;

import com.box.l10n.mojito.service.ai.PromptService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(value = "l10n.ai.enabled", havingValue = "true")
public class AIPromptWS {

  static Logger logger = LoggerFactory.getLogger(AIPromptWS.class);

  @Autowired PromptService promptService;

  @Operation(summary = "Create a new Prompt")
  @RequestMapping(value = "/api/ai/prompts", method = RequestMethod.POST)
  @Timed("AIWS.createPrompt")
  public long createPrompt(@RequestBody AIPromptCreateRequest AIPromptCreateRequest) {
    logger.debug("Received request to create prompt");
    return promptService.createPrompt(AIPromptCreateRequest);
  }

  @Operation(summary = "Add a Prompt to a specific Repository and Prompt Type")
  @RequestMapping(
      value = "/api/ai/prompts/{prompt_id}/{repository_name}/{prompt_type}",
      method = RequestMethod.POST)
  @Timed("AIWS.addPromptToRepository")
  public void addPromptToRepository(
      @PathVariable("prompt_id") Long promptId,
      @PathVariable("repository_name") String repositoryName,
      @PathVariable("prompt_type") String promptType) {
    logger.debug("Received request to add prompt id {} to {} repository", promptId, repositoryName);
    promptService.addPromptToRepository(promptId, repositoryName, promptType);
  }

  @Operation(summary = "Delete a Prompt by ID")
  @RequestMapping(value = "/api/ai/prompts/{prompt_id}", method = RequestMethod.DELETE)
  @Timed("AIWS.deletePrompt")
  public void deletePrompt(@PathVariable("prompt_id") Long promptId) {
    logger.debug("Received request to delete prompt id {}", promptId);
    promptService.deletePrompt(promptId);
  }

  @Operation(summary = "Get a Prompt by ID")
  @RequestMapping(value = "/api/ai/prompts/{prompt_id}", method = RequestMethod.GET)
  @Timed("AIWS.getPrompt")
  public AIPrompt getPrompt(@PathVariable("prompt_id") Long promptId) {
    logger.debug("Received request to get prompt id {}", promptId);
    return buildOpenAIPromptDTO(promptService.getPrompt(promptId));
  }

  @Operation(summary = "Get all active Prompts")
  @RequestMapping(value = "/api/ai/prompts", method = RequestMethod.GET)
  @Timed("AIWS.getAllActivePrompts")
  public List<AIPrompt> getAllActivePrompts() {
    logger.debug("Received request to get all active prompts");
    return promptService.getAllActivePrompts().stream()
        .map(AIPromptWS::buildOpenAIPromptDTO)
        .collect(Collectors.toList());
  }

  @RequestMapping(value = "/api/ai/prompts/{repository_name}", method = RequestMethod.GET)
  @Timed("AIWS.getAllActivePrompts")
  public List<AIPrompt> getAllActivePromptsForRepository(
      @PathVariable("repository_name") String repositoryName) {
    logger.debug("Received request to get prompts all prompts for repository {}", repositoryName);
    return promptService.getAllActivePromptsForRepository(repositoryName).stream()
        .map(AIPromptWS::buildOpenAIPromptDTO)
        .collect(Collectors.toList());
  }

  @Operation(summary = "Create a Prompt Context Message")
  @RequestMapping(value = "/api/ai/prompts/contextMessage", method = RequestMethod.POST)
  @Timed("AIWS.createPromptMessage")
  public Long createPromptMessage(
      @RequestBody AIPromptContextMessageCreateRequest aiPromptContextMessageCreateRequest) {
    logger.debug("Received request to create prompt message");
    return promptService.createPromptContextMessage(aiPromptContextMessageCreateRequest);
  }

  @Operation(summary = "Delete a Prompt Context Message by ID")
  @RequestMapping(
      value = "/api/ai/prompts/contextMessage/{context_message_id}",
      method = RequestMethod.DELETE)
  @Timed("AIWS.deletePromptMessage")
  public void deletePromptMessage(@PathVariable("context_message_id") Long contextMessageId) {
    logger.debug("Received request to delete prompt message id {}", contextMessageId);
    promptService.deletePromptContextMessage(contextMessageId);
  }

  @Operation(summary = "Create or update a Repository Locale Prompt Overrides")
  @RequestMapping(
      value = "/api/ai/prompts/translation/locale/overrides",
      method = RequestMethod.POST)
  @Timed("AIWS.createOrUpdateRepositoryLocalePromptOverrides")
  public ResponseEntity<String> createOrUpdateRepositoryLocalePromptOverrides(
      @RequestBody AITranslationLocalePromptOverridesRequest request) {
    logger.debug("Received request to create or update repository locale prompt overrides");
    promptService.createOrUpdateRepositoryLocaleTranslationPromptOverrides(
        request.getRepositoryName(),
        request.getLocales(),
        request.getAiPromptId(),
        request.isDisabled());
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @Operation(summary = "Delete a Repository Locale Prompt Overrides for a given request")
  @RequestMapping(
      value = "/api/ai/prompts/translation/locale/overrides",
      method = RequestMethod.DELETE)
  @Timed("AIWS.deleteRepositoryLocalePromptOverrides")
  public ResponseEntity<String> deleteRepositoryLocalePromptOverrides(
      @RequestBody AITranslationLocalePromptOverridesRequest request) {
    logger.debug("Received request to delete repository locale prompt overrides");
    promptService.deleteRepositoryLocaleTranslationPromptOverride(
        request.getRepositoryName(), request.getLocales());
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  private static AIPrompt buildOpenAIPromptDTO(com.box.l10n.mojito.entity.AIPrompt prompt) {
    AIPrompt AIPrompt = new AIPrompt();
    AIPrompt.setSystemPrompt(prompt.getSystemPrompt());
    AIPrompt.setUserPrompt(prompt.getUserPrompt());
    AIPrompt.setModelName(prompt.getModelName());
    AIPrompt.setPromptTemperature(prompt.getPromptTemperature());
    AIPrompt.setDeleted(prompt.isDeleted());
    return AIPrompt;
  }
}
