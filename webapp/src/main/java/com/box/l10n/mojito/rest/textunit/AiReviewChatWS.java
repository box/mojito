package com.box.l10n.mojito.rest.textunit;

import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.JsonFormat.JsonSchema.createJsonSchema;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.SystemMessage.systemMessageBuilder;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.UserMessage.userMessageBuilder;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.chatCompletionsRequest;
import static com.box.l10n.mojito.openai.OpenAIClient.TemperatureHelper.getTemperatureForReasoningModels;

import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.openai.OpenAIClient;
import com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest;
import com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsResponse;
import com.box.l10n.mojito.rest.textunit.AiReviewType.AiReviewTextUnitVariantOutput;
import com.box.l10n.mojito.service.oaireview.AiReviewConfigurationProperties;
import com.box.l10n.mojito.service.oaireview.AiReviewService.AiReviewTextUnitVariantInput;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiReviewChatWS {

  static Logger logger = LoggerFactory.getLogger(AiReviewChatWS.class);

  private final OpenAIClient openAIClient;
  private final AiReviewConfigurationProperties aiReviewConfigurationProperties;
  private final ObjectMapper objectMapper;

  public AiReviewChatWS(
      @Qualifier("openAIClientReview") @Nullable OpenAIClient openAIClient,
      AiReviewConfigurationProperties aiReviewConfigurationProperties,
      @Qualifier("objectMapperReview") ObjectMapper objectMapper) {
    this.openAIClient = openAIClient;
    this.aiReviewConfigurationProperties = Objects.requireNonNull(aiReviewConfigurationProperties);
    this.objectMapper = Objects.requireNonNull(objectMapper);
  }

  @PostMapping("/api/ai/review")
  @ResponseStatus(HttpStatus.OK)
  public AiReviewChatResponse chat(@RequestBody AiReviewChatRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    if (request.messages() == null || request.messages().isEmpty()) {
      throw new IllegalArgumentException("messages must not be empty");
    }

    String localeTag = StringUtils.hasText(request.localeTag()) ? request.localeTag() : "en";
    AiReviewChatMessage lastMessage = request.messages().get(request.messages().size() - 1);
    if (lastMessage == null || !StringUtils.hasText(lastMessage.content())) {
      throw new IllegalArgumentException("last message must contain text");
    }

    if (openAIClient == null) {
      throw new IllegalStateException("openAIClientReview bean must be configured");
    }

    AiReviewTextUnitVariantInput.ExistingTarget existingTarget = null;
    if (StringUtils.hasText(request.target())) {
      existingTarget = new AiReviewTextUnitVariantInput.ExistingTarget(request.target(), false);
    }

    AiReviewTextUnitVariantInput aiReviewInput =
        new AiReviewTextUnitVariantInput(
            localeTag, request.source(), lastMessage.content(), existingTarget);

    String inputPayload = objectMapper.writeValueAsStringUnchecked(aiReviewInput);

    List<ChatCompletionsRequest.Message> messages = new ArrayList<>();
    messages.add(systemMessageBuilder().content(AiReviewType.PROMPT_ALL).build());
    messages.add(userMessageBuilder().content(inputPayload).build());

    for (AiReviewChatMessage message : request.messages()) {
      if (message == null || !StringUtils.hasText(message.content())) {
        continue;
      }
      String role = StringUtils.hasText(message.role()) ? message.role() : "user";
      messages.add(userMessageBuilder().role(role).content(message.content()).build());
    }

    ChatCompletionsRequest chatCompletionsRequest =
        chatCompletionsRequest()
            .model(aiReviewConfigurationProperties.getModelName())
            .temperature(
                getTemperatureForReasoningModels(aiReviewConfigurationProperties.getModelName()))
            .maxCompletionTokens(null)
            .messages(messages)
            .responseFormat(
                new OpenAIClient.ChatCompletionsRequest.JsonFormat(
                    "json_schema",
                    new OpenAIClient.ChatCompletionsRequest.JsonFormat.JsonSchema(
                        true,
                        "request_json_format",
                        createJsonSchema(AiReviewTextUnitVariantOutput.class))))
            .build();

    logger.info(objectMapper.writeValueAsStringUnchecked(chatCompletionsRequest));

    ChatCompletionsResponse chatCompletionsResponse =
        openAIClient
            .getChatCompletions(chatCompletionsRequest, Duration.of(15, ChronoUnit.SECONDS))
            .join();

    logger.info(objectMapper.writeValueAsStringUnchecked(chatCompletionsResponse));

    String jsonResponse = chatCompletionsResponse.choices().getFirst().message().content();
    AiReviewTextUnitVariantOutput output =
        objectMapper.readValueUnchecked(jsonResponse, AiReviewTextUnitVariantOutput.class);

    String reply = output.target() != null ? output.target().explanation() : null;
    if (!StringUtils.hasText(reply) && output.reviewRequired() != null) {
      reply = output.reviewRequired().reason();
    }
    if (!StringUtils.hasText(reply)) {
      reply = "Here are the latest suggestions.";
    }

    List<AiReviewChatSuggestion> suggestions = new ArrayList<>();
    Set<String> seen = new HashSet<>();

    if (output.target() != null) {
      addSuggestion(
          suggestions,
          seen,
          output.target().content(),
          output.target().confidenceLevel(),
          output.target().explanation());
    }
    if (output.altTarget() != null) {
      addSuggestion(
          suggestions,
          seen,
          output.altTarget().content(),
          output.altTarget().confidenceLevel(),
          output.altTarget().explanation());
    }

    AiReviewChatReview review = null;
    if (output.existingTargetRating() != null) {
      review =
          new AiReviewChatReview(
              output.existingTargetRating().score(), output.existingTargetRating().explanation());
    }

    return new AiReviewChatResponse(
        new AiReviewChatMessage("assistant", reply), suggestions, review);
  }

  private void addSuggestion(
      List<AiReviewChatSuggestion> suggestions,
      Set<String> seen,
      String content,
      Integer confidenceLevel,
      String explanation) {
    if (!StringUtils.hasText(content)) {
      return;
    }
    if (seen.add(content)) {
      suggestions.add(new AiReviewChatSuggestion(content, confidenceLevel, explanation));
    }
  }

  public record AiReviewChatRequest(
      String source, String target, String localeTag, List<AiReviewChatMessage> messages) {}

  public record AiReviewChatMessage(String role, String content) {}

  public record AiReviewChatSuggestion(
      String content, Integer confidenceLevel, String explanation) {}

  public record AiReviewChatReview(int score, String explanation) {}

  public record AiReviewChatResponse(
      AiReviewChatMessage message,
      List<AiReviewChatSuggestion> suggestions,
      AiReviewChatReview review) {}
}
