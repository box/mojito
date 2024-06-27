package com.box.l10n.mojito.service.ai.openai;

import static com.box.l10n.mojito.entity.PromptType.SOURCE_STRING_CHECKER;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.ChatMessage.messageBuilder;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.chatCompletionsRequest;
import static com.box.l10n.mojito.service.ai.openai.OpenAIPromptContextMessageType.SYSTEM;
import static com.box.l10n.mojito.service.ai.openai.OpenAIPromptContextMessageType.USER;

import com.box.l10n.mojito.entity.AIPrompt;
import com.box.l10n.mojito.entity.AIPromptContextMessage;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.box.l10n.mojito.openai.OpenAIClient;
import com.box.l10n.mojito.rest.ai.AICheckRequest;
import com.box.l10n.mojito.rest.ai.AICheckResponse;
import com.box.l10n.mojito.rest.ai.AICheckResult;
import com.box.l10n.mojito.rest.ai.AIException;
import com.box.l10n.mojito.service.ai.AIStringCheckRepository;
import com.box.l10n.mojito.service.ai.LLMService;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "l10n.ai.service.type", havingValue = "OpenAI")
public class OpenAILLMService implements LLMService {

  static Logger logger = LoggerFactory.getLogger(OpenAILLMService.class);

  @Autowired OpenAIClient openAIClient;

  @Autowired RepositoryRepository repositoryRepository;

  @Autowired AIStringCheckRepository aiStringCheckRepository;

  @Autowired ObjectMapper objectMapper;

  @Autowired LLMPromptService LLMPromptService;

  @Autowired MeterRegistry meterRegistry;

  @Value("${l10n.ai.checks.persistResults:true}")
  boolean persistResults;

  @Timed("OpenAILLMService.executeAIChecks")
  public AICheckResponse executeAIChecks(AICheckRequest aiCheckRequest) {

    logger.debug("Executing OpenAI string checks.");
    Repository repository = repositoryRepository.findByName(aiCheckRequest.getRepositoryName());

    if (repository == null) {
      logger.error("Repository not found: {}", aiCheckRequest.getRepositoryName());
      throw new AIException("Repository not found: " + aiCheckRequest.getRepositoryName());
    }

    List<AIPrompt> prompts =
        LLMPromptService.getPromptsByRepositoryAndPromptType(repository, SOURCE_STRING_CHECKER);

    Map<String, AssetExtractorTextUnit> textUnitsUniqueSource =
        aiCheckRequest.getTextUnits().stream()
            .collect(
                Collectors.toMap(
                    AssetExtractorTextUnit::getSource,
                    textUnit -> textUnit,
                    (existing, replacement) -> existing,
                    HashMap::new));
    Map<String, List<AICheckResult>> results = new HashMap<>();
    textUnitsUniqueSource
        .values()
        .forEach(
            textUnit -> {
              List<AICheckResult> aiCheckResults = checkString(textUnit, prompts, repository);
              results.put(textUnit.getSource(), aiCheckResults);
            });

    AICheckResponse aiCheckResponse = new AICheckResponse();
    aiCheckResponse.setResults(results);

    return aiCheckResponse;
  }

  @Timed("OpenAILLMService.checkString")
  private List<AICheckResult> checkString(
      AssetExtractorTextUnit textUnit, List<AIPrompt> prompts, Repository repository) {
    List<AICheckResult> results = new ArrayList<>();
    String sourceString = textUnit.getSource();
    String comment = textUnit.getComments() != null ? textUnit.getComments() : "";
    String[] nameSplit = textUnit.getName().split(" --- ");

    if (!prompts.isEmpty()) {
      executePromptChecks(textUnit, prompts, repository, sourceString, comment, nameSplit, results);
    } else {
      logger.warn("No prompts found for repository: {}", repository.getName());
      AICheckResult result = new AICheckResult();
      result.setSuccess(true);
      result.setSuggestedFix(
          "No prompts found for repository: " + repository.getName() + ", skipping check.");
      results.add(result);
    }

    return results;
  }

  private void executePromptChecks(
      AssetExtractorTextUnit textUnit,
      List<AIPrompt> prompts,
      Repository repository,
      String sourceString,
      String comment,
      String[] nameSplit,
      List<AICheckResult> results) {

    for (AIPrompt prompt : prompts) {
      if ((!Strings.isNullOrEmpty(prompt.getSystemPrompt())
              && !prompt.getSystemPrompt().contains(SOURCE_STRING_PLACEHOLDER))
          && (!Strings.isNullOrEmpty(prompt.getUserPrompt())
              && !prompt.getUserPrompt().contains(SOURCE_STRING_PLACEHOLDER))) {
        logger.error(
            "Source string placeholder is missing in both system and user prompts for prompt id {}. Skipping check.",
            prompt.getId());
        continue;
      }
      String systemPrompt = getFormattedPrompt(prompt.getSystemPrompt(), sourceString, comment);

      String userPrompt = getFormattedPrompt(prompt.getUserPrompt(), sourceString, comment);

      if (nameSplit.length > 1
          && (systemPrompt.contains(CONTEXT_STRING_PLACEHOLDER)
              || userPrompt.contains(CONTEXT_STRING_PLACEHOLDER))) {
        systemPrompt = systemPrompt.replace(CONTEXT_STRING_PLACEHOLDER, nameSplit[1]);
        userPrompt = userPrompt.replace(CONTEXT_STRING_PLACEHOLDER, nameSplit[1]);
      }

      OpenAIClient.ChatCompletionsRequest chatCompletionsRequest =
          buildChatCompletionsRequest(
              prompt, systemPrompt, userPrompt, prompt.getContextMessages(), true);

      OpenAIClient.ChatCompletionsResponse chatCompletionsResponse =
          openAIClient.getChatCompletions(chatCompletionsRequest).join();
      if (persistResults) {
        persistCheckResult(
            textUnit,
            repository,
            prompt,
            chatCompletionsResponse.choices().getFirst().message().content(),
            aiStringCheckRepository);
      }
      results.add(parseResponse(chatCompletionsResponse, repository));
    }
  }

  private AICheckResult parseResponse(
      OpenAIClient.ChatCompletionsResponse chatCompletionsResponse, Repository repository) {
    AICheckResult result;
    String response = chatCompletionsResponse.choices().getFirst().message().content();
    try {
      result = objectMapper.readValue(response, AICheckResult.class);
      meterRegistry
          .counter(
              "OpenAILLMService.checks.result",
              "success",
              Boolean.toString(result.isSuccess()),
              "repository",
              repository.getName())
          .increment();
    } catch (JsonProcessingException e) {
      logger.error("Error parsing AI check result from response: {}", response, e);
      // Nothing the user can do about this, so just skip the check
      result = new AICheckResult();
      result.setSuccess(true);
      result.setSuggestedFix("Check skipped as error parsing response from OpenAI.");
      meterRegistry
          .counter("OpenAILLMService.checks.parse.error", "repository", repository.getName())
          .increment();
    }
    return result;
  }

  private static String getFormattedPrompt(String prompt, String sourceString, String comment) {
    String systemPrompt = "";
    if (prompt != null) {
      systemPrompt =
          prompt
              .replace(SOURCE_STRING_PLACEHOLDER, sourceString)
              .replace(COMMENT_STRING_PLACEHOLDER, comment);
    }
    return systemPrompt;
  }

  private static OpenAIClient.ChatCompletionsRequest buildChatCompletionsRequest(
      AIPrompt prompt,
      String systemPrompt,
      String userPrompt,
      List<AIPromptContextMessage> contextMessages,
      boolean isJsonResponseType) {
    return chatCompletionsRequest()
        .temperature(prompt.getPromptTemperature())
        .model(prompt.getModelName())
        .messages(buildPromptMessages(systemPrompt, userPrompt, contextMessages))
        .jsonResponseType(isJsonResponseType)
        .build();
  }

  private static List<OpenAIClient.ChatCompletionsRequest.Message> buildPromptMessages(
      String systemPrompt, String userPrompt, List<AIPromptContextMessage> contextMessages) {
    List<OpenAIClient.ChatCompletionsRequest.Message> messages = new ArrayList<>();
    for (AIPromptContextMessage contextMessage : contextMessages) {
      messages.add(
          messageBuilder()
              .role(contextMessage.getMessageType())
              .content(contextMessage.getContent())
              .build());
    }

    if (!Strings.isNullOrEmpty(systemPrompt)) {
      messages.add(messageBuilder().role(SYSTEM.getType()).content(systemPrompt).build());
    }

    if (!Strings.isNullOrEmpty(userPrompt)) {
      messages.add(messageBuilder().role(USER.getType()).content(userPrompt).build());
    }

    return messages;
  }
}
