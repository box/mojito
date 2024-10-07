package com.box.l10n.mojito.rest.textunit;

import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.JsonFormat.JsonSchema.createJsonSchema;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.SystemMessage.systemMessageBuilder;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.UserMessage.userMessageBuilder;
import static com.box.l10n.mojito.openai.OpenAIClient.ChatCompletionsRequest.chatCompletionsRequest;

import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.openai.OpenAIClient;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiReviewWS {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(AiReviewWS.class);

  TextUnitSearcher textUnitSearcher;

  AiReviewConfigurationProperties aiReviewConfigurationProperties;

  public AiReviewWS(
      TextUnitSearcher textUnitSearcher,
      AiReviewConfigurationProperties aiReviewConfigurationProperties) {
    this.textUnitSearcher = textUnitSearcher;
    this.aiReviewConfigurationProperties = aiReviewConfigurationProperties;
  }

  @RequestMapping(method = RequestMethod.GET, value = "/api/proto-ai-review")
  @ResponseStatus(HttpStatus.OK)
  public ProtoAiReviewResponse getTextUnitsWithGet(ProtoAiReviewRequest protoAiReviewRequest) {

    TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
    textUnitSearcherParameters.setTmTextUnitVariantId(protoAiReviewRequest.tmTextUnitVariantId);

    List<TextUnitDTO> search = textUnitSearcher.search(textUnitSearcherParameters);
    if (search.isEmpty()) {
      throw new RuntimeException("Wrong tmTextUnitVariantId");
    }

    TextUnitDTO textUnit = search.getFirst();

    AiReviewInput input =
        new AiReviewInput(
            textUnit.getTargetLocale(),
            textUnit.getSource(),
            textUnit.getComment(),
            new AiReviewInput.ExistingTarget(
                textUnit.getTarget(), !textUnit.isIncludedInLocalizedFile()));

    ObjectMapper objectMapper = ObjectMapper.withIndentedOutput();
    String inputAsJsonString = objectMapper.writeValueAsStringUnchecked(input);

    ObjectNode jsonSchema = createJsonSchema(AiReviewOutput.class);

    OpenAIClient.ChatCompletionsRequest chatCompletionsRequest =
        chatCompletionsRequest()
            .model("gpt-4o-2024-08-06")
            .maxTokens(16384)
            .messages(
                List.of(
                    systemMessageBuilder().content(PROMPT).build(),
                    userMessageBuilder().content(inputAsJsonString).build()))
            .responseFormat(
                new OpenAIClient.ChatCompletionsRequest.JsonFormat(
                    "json_schema",
                    new OpenAIClient.ChatCompletionsRequest.JsonFormat.JsonSchema(
                        true, "request_json_format", jsonSchema)))
            .build();

    logger.info(objectMapper.writeValueAsStringUnchecked(chatCompletionsRequest));

    OpenAIClient openAIClient =
        OpenAIClient.builder()
            .apiKey(aiReviewConfigurationProperties.getOpenaiClientToken())
            .build();

    OpenAIClient.ChatCompletionsResponse chatCompletionsResponse =
        openAIClient.getChatCompletions(chatCompletionsRequest).join();

    logger.info(objectMapper.writeValueAsStringUnchecked(chatCompletionsResponse));

    String jsonResponse = chatCompletionsResponse.choices().getFirst().message().content();
    AiReviewOutput review = objectMapper.readValueUnchecked(jsonResponse, AiReviewOutput.class);

    return new ProtoAiReviewResponse(textUnit, review);
  }

  public record ProtoAiReviewRequest(long tmTextUnitVariantId) {}

  public record ProtoAiReviewResponse(TextUnitDTO textUnitDTO, AiReviewOutput aiReviewOutput) {}

  record AiReviewOutput(
      String source,
      Target target,
      DescriptionRating descriptionRating,
      AltTarget altTarget,
      ExistingTargetRating existingTargetRating,
      ReviewRequired reviewRequired) {
    record Target(String content, String explanation, int confidenceLevel) {}

    record AltTarget(String content, String explanation, int confidenceLevel) {}

    record DescriptionRating(String explanation, int score) {}

    record ExistingTargetRating(String explanation, int score) {}

    record ReviewRequired(boolean required, String reason) {}
  }

  record AiReviewInput(
      String locale, String source, String sourceDescription, ExistingTarget existingTarget) {
    record ExistingTarget(String content, boolean hasBrokenPlaceholders) {}
  }

  static final String PROMPT =
      """
          Your role is to act as a translator.
          You are tasked with translating provided source strings while preserving both the tone and the technical structure of the string. This includes protecting any tags, placeholders, or code elements that should not be translated.

          The input will be provided in JSON format with the following fields:

              •	"source": The source text to be translated.
              •	"locale": The target language locale, following the BCP47 standard (e.g., “fr”, “es-419”).
              •	"sourceDescription": A description providing context for the source text.
              •	"existingTarget" (optional): An existing translation to review.

          Instructions:

              •	If the source is colloquial, keep the translation colloquial; if it’s formal, maintain formality in the translation.
              •	Pay attention to regional variations specified in the "locale" field (e.g., “es” vs. “es-419”, “fr” vs. “fr-CA”, “zh” vs. “zh-Hant”), and ensure the translation length remains similar to the source text.
              •	Aim to provide the best translation, while compromising on length to ensure it remains close to the original text length

          Handling Tags and Code:

          Some strings contain code elements such as tags (e.g., {atag}, ICU message format, or HTML tags). You are provided with a inputs of tags that need to be protected. Ensure that:

              •	Tags like {atag} remain untouched.
              •	In cases of nested content (e.g., <a href={url}>text that needs translation</a>), only translate the inner text while preserving the outer structure.
              •	Complex structures like ICU message formats should have placeholders or variables left intact (e.g., {count, plural, one {# item} other {# items}}), but translate any inner translatable text.

          Ambiguity and Context:

          After translating, assess the usefulness of the "sourceDescription" field:

              •	Rate its usefulness on a scale of 0 to 2:
              •	0 – Not helpful at all; irrelevant or misleading.
              •	1 – Somewhat helpful; provides partial or unclear context but is useful to some extent.
              •	2 – Very helpful; provides clear and sufficient guidance for the translation.

          If the source is ambiguous—for example, if it could be interpreted as a noun or a verb—you must:

              •	Indicate the ambiguity in your explanation.
              •	Provide translations for all possible interpretations.
              •	Set "reviewRequired" to true, and explain the need for review due to the ambiguity.

          You will provide an output in JSON format with the following fields:

              •	"source": The original source text.
              •	"target": An object containing:
              •	"content": The best translation.
              •	"explanation": A brief explanation of your translation choices.
              •	"confidenceLevel": Your confidence level (0-100%) in the translation.
              •	"descriptionRating": An object containing:
              •	"explanation": An explanation of how the "sourceDescription" aided your translation.
              •	"score": The usefulness score (0-2).
              •	"altTarget": An object containing:
              •	"content": An alternative translation, if applicable. Focus on showcasing grammar differences,
              •	"explanation": Explanation for the alternative translation.
              •	"confidenceLevel": Your confidence level (0-100%) in the alternative translation.
              •	"existingTargetRating" (if "existingTarget" is provided): An object containing:
              •	"explanation": Feedback on the existing translation’s accuracy and quality.
              •	"score": A rating score (0-2).
              •	"reviewRequired": An object containing:
              •	"required": true or false, indicating if review is needed.
              •	"reason": A detailed explanation of why review is or isn’t needed.
          """;
}
