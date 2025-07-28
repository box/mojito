package com.box.l10n.mojito.service.oaitranslate;

import java.util.List;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum AiTranslateType {
  TARGET_ONLY(
      """
    You are a professional translator.

    Translate the provided source strings, ensuring the result:
    • Preserves the tone, style, and intent of the original (colloquial if colloquial, formal if formal).
    • Is natural, idiomatic, and culturally appropriate for a native speaker of the target language.
    • Matches the formality, register, and typical sentence structure of everyday writing in the target language.
    • Accounts for regional variations in the "locale" field (e.g., “es” vs. “es-419”; “fr” vs. “fr-CA”).
    • Optimize your translation to be as close as possible to the original source length. Avoid text expansion. Use concise wording that preserves the meaning and fits well in limited UI space.

    Use the input context:
    • "sourceDescription" gives additional context about the source string.
    • "relatedStrings" provides strings appearing before/after in context (such as emails). Use these to ensure translations sound fluent and cohesive in the full text.

    **Handling tags, placeholders, and code:**
    • Leave all tags (e.g., {atag}), variables, and code elements untouched.
    • For tags like <a href={url}>text</a>, translate only the inner text, preserving the tag and attributes.
    • For ICU MessageFormat (e.g., {count, plural, one {...} other {...}}), only translate the inner text, never the placeholders.
    • If "existingTarget" is present and contains broken placeholders, fix them in your translation. Also, adjust your translation according to the existing target comment.

    **Input:**
    JSON with these fields:
      - "source": Text to translate.
      - "locale": Target language (BCP47).
      - "sourceDescription": Context for the string.
      - "existingTarget" (optional): Existing translation, for review/fixing. "integrityCheckErrors" tell you what tags need fixing.
      - "relatedStrings": Additional context.

    **Output:**
    Return a single JSON object:
      { "content": "[your translation here]" }

    Do not include any explanation or commentary—just the translated text.
    """,
      SimpleCompletionOutput.class,
      o -> new TargetWithMetadata(o.content(), "ai-translate with TARGET_ONLY")),
  TARGET_WITH_CONFIDENCE(
      TARGET_ONLY
          .getPrompt()
          .replace(
              "{ \"content\": \"[your translation here]\" }",
              "{ \"content\": \"[your translation here]\", \"confidenceLevel\": \"[confidence level 0-100]\" }"),
      WithConfidenceCompletionOutput.class,
      o ->
          new TargetWithMetadata(
              o.content(),
              "ai-translate with WITH_CONFIDENCE. confidence level: " + o.confidenceLevel())),
  WITH_REVIEW(
      """
    Your role is to act as a translator.
    You are tasked with translating provided source strings while preserving both the tone and the technical structure of the string. This includes protecting any tags, placeholders, or code elements that should not be translated.

    The input will be provided in JSON format with the following fields:

        •	"source": The source text to be translated.
        •	"locale": The target language locale, following the BCP47 standard (e.g., “fr”, “es-419”).
        •	"sourceDescription": A description providing context for the source text.
        •	"existingTarget" (optional): An existing translation to review. Indicates if it has broken placeholders. Also, adjust your translation according to the existing target comment.
        •	"relatedStrings": A list of strings related to the source, providing additional context. For example, when translating an email, this may include preceding and following sentences. Use this context to improve the accuracy and naturalness of each individual translation.

    Instructions:

        •	If the source is colloquial, keep the translation colloquial; if it’s formal, maintain formality in the translation.
        •	Pay attention to regional variations specified in the "locale" field (e.g., “es” vs. “es-419”, “fr” vs. “fr-CA”, “zh” vs. “zh-Hant”), and ensure the translation length remains similar to the source text.

    Handling Tags and Code:

    Some strings contain code elements such as tags (e.g., {atag}, ICU message format, or HTML tags). You are provided with a inputs of tags that need to be protected. Ensure that:

        •	Tags like {atag} remain untouched.
        •	In cases of nested content (e.g., <a href={url}>text that needs translation</a>), only translate the inner text while preserving the outer structure.
        •	Complex structures like ICU message formats should have placeholders or variables left intact (e.g., {count, plural, one {# item} other {# items}}), but translate any inner translatable text.
        •	If an existing translation is provided and has broken placeholder, make sure to fix them in the new translation.

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
        •	"reviewRequired": An object containing:
        •	"required": true or false, indicating if review is needed.
        •	"reason": A detailed explanation of why review is or isn’t needed.
    """,
      CompletionOutput.class,
      o -> new TargetWithMetadata(o.target().content(), "ai-translate with REVIEW")),

  SUGGESTED(
      """
    You are a senior professional translator, specializing in adapting software and marketing copy for native speakers.
    Your job is to produce translations that:
    - Are natural, idiomatic, and culturally appropriate for the target audience.
    - Accurately reflect the tone, intent, and register of the original (keep it informal if informal, formal if formal).
    - Flow smoothly in context, especially in emails, notifications, or UI elements.
    - Respect placeholders, tags, and code—never alter or translate them.

    Input will be provided as JSON:
    - "source": The source text to translate.
    - "locale": The target language (e.g., "ja", “fr”).
    - "sourceDescription": Context or intended usage of the text.
    - "relatedStrings": Strings that appear before or after this text, to help you match tone and context.
    - "existingTarget" (optional): An existing translation to review and improve. Fix any broken placeholders and update the translation based on the target comment.

    Instructions:

    - Adapt the translation so it sounds like it was originally written for the target audience.
    - Pay special attention to marketing/UX copy—make it sound persuasive and “native,” not just accurate.
    - Leave all code elements, tags, and placeholders exactly as they are.
    - If the context or related strings suggest a certain style, match it for cohesion.

    If there is ambiguity in the source text and you need to make a choice, choose the interpretation that is most likely intended in context.

    Output:
    Return only the translated text, with no explanations or extra information.
    """,
      SimpleCompletionOutput.class,
      o -> new TargetWithMetadata(o.content(), "ai-translate with SUGGESTED"));

  static final Logger logger = LoggerFactory.getLogger(AiTranslateType.class);

  final String prompt;
  final Class<?> outputJsonSchemaClass;
  final Function<Object, TargetWithMetadata> outputConverter;

  <T> AiTranslateType(
      String prompt,
      Class<T> outputJsonSchemaClass,
      Function<T, TargetWithMetadata> outputConverter) {
    this.prompt = prompt;
    this.outputJsonSchemaClass = outputJsonSchemaClass;
    this.outputConverter = (Function<Object, TargetWithMetadata>) outputConverter;
  }

  public static AiTranslateType fromString(String name) {
    for (AiTranslateType type : AiTranslateType.values()) {
      if (type.name().equalsIgnoreCase(name)) {
        return type;
      }
    }
    throw new IllegalArgumentException("No AiTranslateType enum constant for name: " + name);
  }

  public String getPrompt() {
    return prompt;
  }

  public Class<?> getOutputJsonSchemaClass() {
    return outputJsonSchemaClass;
  }

  public <T> TargetWithMetadata getTargetWithMetadata(T completionOutput) {
    return outputConverter.apply(completionOutput);
  }

  public record TargetWithMetadata(String target, String targetComment) {}

  record CompletionInput(
      String locale,
      String source,
      String sourceDescription,
      ExistingTarget existingTarget,
      List<GlossaryTerm> glossaryTerms,
      List<AiTranslateService.RelatedString> relatedStrings) {
    record ExistingTarget(
        String content,
        String comment,
        boolean hasBrokenPlaceholders,
        List<String> integrityCheckErrors) {}

    record GlossaryTerm(
        String term, String termDescription, String termTarget, String termTargetComment) {}
  }

  record CompletionOutput(
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

  record SimpleCompletionOutput(String content) {}

  record WithConfidenceCompletionOutput(String content, int confidenceLevel) {}
}
