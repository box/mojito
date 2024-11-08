package com.box.l10n.mojito.service.ai.openai;

import static com.box.l10n.mojito.entity.PromptType.SOURCE_STRING_CHECKER;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.ChatMessage.messageBuilder;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.chatCompletionsRequest;
import static com.box.l10n.mojito.service.ai.openai.OpenAIPromptContextMessageType.SYSTEM;
import static com.box.l10n.mojito.service.ai.openai.OpenAIPromptContextMessageType.USER;

import com.box.l10n.mojito.entity.AIPrompt;
import com.box.l10n.mojito.entity.AIPromptContextMessage;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.box.l10n.mojito.openai.OpenAIClient;
import com.box.l10n.mojito.rest.ai.AICheckRequest;
import com.box.l10n.mojito.rest.ai.AICheckResponse;
import com.box.l10n.mojito.rest.ai.AICheckResult;
import com.box.l10n.mojito.rest.ai.AIException;
import com.box.l10n.mojito.service.ai.AIStringCheckRepository;
import com.box.l10n.mojito.service.ai.LLMPromptService;
import com.box.l10n.mojito.service.ai.LLMService;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

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

  @Value("${l10n.ai.translate.retry.maxAttempts:10}")
  int retryMaxAttempts;

  @Value("${l10n.ai.translate.retry.minDurationSeconds:5}")
  int retryMinDurationSeconds;

  @Value("${l10n.ai.translate.retry.maxBackoffDurationSeconds:60}")
  int retryMaxBackoffDurationSeconds;

  RetryBackoffSpec llmTranslateRetryConfig;

  Map<String, Pattern> patternCache = new HashMap<>();

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

    Map<String, CompletableFuture<List<AICheckResult>>> checkResultsBySource = new HashMap<>();
    textUnitsUniqueSource
        .values()
        .forEach(
            textUnit -> {
              CompletableFuture<List<AICheckResult>> checkResultFuture =
                  CompletableFuture.supplyAsync(() -> checkString(textUnit, prompts, repository));
              checkResultsBySource.put(textUnit.getSource(), checkResultFuture);
            });

    CompletableFuture<Void> combinedCheckResults =
        CompletableFuture.allOf(checkResultsBySource.values().toArray(new CompletableFuture[0]));

    CompletableFuture<Map<String, List<AICheckResult>>> checkResults =
        combinedCheckResults.thenApply(
            v -> {
              Map<String, List<AICheckResult>> allResultsList = new HashMap<>();
              for (Map.Entry<String, CompletableFuture<List<AICheckResult>>>
                  checkResultFutureBySource : checkResultsBySource.entrySet()) {
                try {
                  allResultsList.put(
                      checkResultFutureBySource.getKey(),
                      checkResultFutureBySource.getValue().get());
                } catch (InterruptedException | ExecutionException e) {
                  logger.error("Error while running a completable future", e);
                }
              }
              return allResultsList;
            });

    Map<String, List<AICheckResult>> results = new HashMap<>();
    checkResults.thenAccept(results::putAll);

    try {
      checkResults.get();
    } catch (InterruptedException | ExecutionException e) {
      logger.error("Error while running completable futures", e);
    }

    AICheckResponse aiCheckResponse = new AICheckResponse();
    aiCheckResponse.setResults(results);

    return aiCheckResponse;
  }

  @Override
  @Timed("OpenAILLMService.translate")
  public String translate(
      TMTextUnit tmTextUnit, String sourceBcp47Tag, String targetBcp47Tag, AIPrompt prompt) {
    logger.debug(
        "Translating text unit {} from {} to {} using prompt {}",
        tmTextUnit.getId(),
        sourceBcp47Tag,
        targetBcp47Tag,
        prompt.getId());
    String systemPrompt =
        getTranslationFormattedPrompt(
            prompt.getSystemPrompt(), tmTextUnit, sourceBcp47Tag, targetBcp47Tag);
    String userPrompt =
        getTranslationFormattedPrompt(
            prompt.getUserPrompt(), tmTextUnit, sourceBcp47Tag, targetBcp47Tag);

    OpenAIClient.ChatCompletionsRequest chatCompletionsRequest =
        buildChatCompletionsRequest(
            prompt, systemPrompt, userPrompt, prompt.getContextMessages(), prompt.isJsonResponse());

    return Mono.fromCallable(
            () -> {
              OpenAIClient.ChatCompletionsResponse chatCompletionsResponse =
                  openAIClient.getChatCompletions(chatCompletionsRequest).join();
              if (chatCompletionsResponse.choices().size() > 1) {
                logger.error(
                    "Multiple choices returned for text unit {}, expected only one",
                    tmTextUnit.getId());
                meterRegistry
                    .counter("OpenAILLMService.translate.error.multiChoiceResponse")
                    .increment();
                throw new AIException(
                    "Multiple response choices returned for text unit "
                        + tmTextUnit.getId()
                        + ", expected only one");
              }
              if (chatCompletionsResponse
                  .choices()
                  .getFirst()
                  .finishReason()
                  .equals(
                      OpenAIClient.ChatCompletionsStreamResponse.Choice.FinishReasons.STOP
                          .getValue())) {
                String response = chatCompletionsResponse.choices().getFirst().message().content();
                logger.debug(
                    "TmTextUnit id: {}, {} translation response: {}",
                    tmTextUnit.getId(),
                    targetBcp47Tag,
                    response);
                if (prompt.isJsonResponse()) {
                  try {
                    logger.debug("Parsing JSON response for key: {}", prompt.getJsonResponseKey());
                    response =
                        objectMapper.readTree(response).get(prompt.getJsonResponseKey()).asText();
                    logger.debug("Parsed translation: {}", response);
                  } catch (JsonProcessingException e) {
                    logger.error("Error parsing JSON response: {}", response, e);
                    throw new AIException("Error parsing JSON response: " + response);
                  }
                }
                meterRegistry
                    .counter("OpenAILLMService.translate.result", "success", "true")
                    .increment();
                return response;
              }
              String message =
                  String.format(
                      "Error translating text unit %d from %s to %s, response finish_reason: %s",
                      tmTextUnit.getId(),
                      sourceBcp47Tag,
                      targetBcp47Tag,
                      chatCompletionsResponse.choices().getFirst().finishReason());
              logger.error(message);
              throw new AIException(message);
            })
        .doOnError(
            e -> {
              logger.error("Error translating text unit {}", tmTextUnit.getId(), e);
              meterRegistry
                  .counter(
                      "OpenAILLMService.translate.result",
                      Tags.of("success", "false", "retryable", "true"))
                  .increment();
            })
        .retryWhen(llmTranslateRetryConfig)
        .doOnError(
            e -> {
              logger.error("Error translating text unit {}", tmTextUnit.getId(), e);
              meterRegistry
                  .counter(
                      "OpenAILLMService.translate.result",
                      Tags.of("success", "false", "retryable", "false"))
                  .increment();
            })
        .blockOptional()
        .orElseThrow(() -> new AIException("Error translating text unit " + tmTextUnit.getId()));
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
      String systemPrompt =
          getStringChecksFormattedPrompt(prompt.getSystemPrompt(), sourceString, comment);

      String userPrompt =
          getStringChecksFormattedPrompt(prompt.getUserPrompt(), sourceString, comment);

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
      results.add(parseAICheckPromptResponse(chatCompletionsResponse, repository));
    }
  }

  private AICheckResult parseAICheckPromptResponse(
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

  private static String getStringChecksFormattedPrompt(
      String prompt, String sourceString, String comment) {
    String formattedPrompt = "";
    if (prompt != null) {
      formattedPrompt =
          prompt
              .replace(SOURCE_STRING_PLACEHOLDER, sourceString)
              .replace(COMMENT_STRING_PLACEHOLDER, comment);
    }
    return formattedPrompt;
  }

  protected String getTranslationFormattedPrompt(
      String prompt, TMTextUnit tmTextUnit, String sourceBcp47Tag, String targetBcp47Tag) {
    String formattedPrompt = "";
    if (prompt != null) {
      formattedPrompt =
          prompt
              .replace(SOURCE_STRING_PLACEHOLDER, tmTextUnit.getContent())
              .replace(SOURCE_LOCALE_PLACEHOLDER, sourceBcp47Tag)
              .replace(TARGET_LOCALE_PLACEHOLDER, targetBcp47Tag);
      formattedPrompt =
          processOptionalPlaceholderText(
              formattedPrompt, COMMENT_STRING_PLACEHOLDER, tmTextUnit.getComment());
      formattedPrompt =
          processOptionalPlaceholderText(
              formattedPrompt,
              PLURAL_FORM_PLACEHOLDER,
              tmTextUnit.getPluralForm() != null ? tmTextUnit.getPluralForm().getName() : null);
      formattedPrompt =
          processOptionalPlaceholderText(
              formattedPrompt,
              CONTEXT_STRING_PLACEHOLDER,
              tmTextUnit.getName() != null && tmTextUnit.getName().split(" --- ").length > 1
                  ? tmTextUnit.getName().split(" --- ")[1]
                  : null);
    }
    return formattedPrompt.trim();
  }

  private String processOptionalPlaceholderText(
      String promptText, String placeholder, String placeholderValue) {
    if (placeholderValue != null && !placeholderValue.isEmpty()) {
      Pattern pattern = patternCache.get(placeholder);
      Matcher matcher = pattern.matcher(promptText);
      if (matcher.find()) {
        String optionalContent = matcher.group(1) + placeholderValue + matcher.group(2);
        if (matcher.groupCount() > 2) {
          optionalContent += matcher.group(3);
        }
        promptText = matcher.replaceFirst(optionalContent);
      }
    } else {
      // Remove the entire template block from the prompt if we have no value for the placeholder,
      // also removing new line characters if they exist immediately after the template ends
      String regex =
          "\\{\\{optional: [^\\{\\}]*"
              + Pattern.quote(placeholder)
              + "[^\\{\\}]*\\}\\}\\s*(?:\\r?\\n)?";
      promptText = promptText.replaceAll(regex, "");
    }
    return promptText;
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

  @PostConstruct
  public void init() {
    String commentPattern =
        "\\{\\{optional: ([^\\{\\}]*)"
            + Pattern.quote(COMMENT_STRING_PLACEHOLDER)
            + "([^\\{\\}]*)\\}\\}(\\s*(?:\\r?\\n)?)";
    String pluralFormPattern =
        "\\{\\{optional: ([^\\{\\}]*)"
            + Pattern.quote(PLURAL_FORM_PLACEHOLDER)
            + "([^\\{\\}]*)\\}\\}(\\s*(?:\\r?\\n)?)";
    String contextPattern =
        "\\{\\{optional: ([^\\{\\}]*)"
            + Pattern.quote(CONTEXT_STRING_PLACEHOLDER)
            + "([^\\{\\}]*)\\}\\}(\\s*(?:\\r?\\n)?)";
    patternCache.put(COMMENT_STRING_PLACEHOLDER, Pattern.compile(commentPattern));
    patternCache.put(PLURAL_FORM_PLACEHOLDER, Pattern.compile(pluralFormPattern));
    patternCache.put(CONTEXT_STRING_PLACEHOLDER, Pattern.compile(contextPattern));
    llmTranslateRetryConfig =
        Retry.backoff(retryMaxAttempts, Duration.ofSeconds(retryMinDurationSeconds))
            .maxBackoff(Duration.ofSeconds(retryMaxBackoffDurationSeconds))
            .onRetryExhaustedThrow(
                (retryBackoffSpec, retrySignal) -> {
                  Throwable error = retrySignal.failure();
                  logger.error(
                      "Retry exhausted after {} attempts: {}",
                      retryMaxAttempts,
                      error.getMessage(),
                      error);
                  return new AIException(
                      "Retry exhausted after "
                          + retryMaxAttempts
                          + " attempts: "
                          + error.getMessage(),
                      error);
                });
  }
}
