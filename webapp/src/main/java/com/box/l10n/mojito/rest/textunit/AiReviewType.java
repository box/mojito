package com.box.l10n.mojito.rest.textunit;

import java.util.List;

public enum AiReviewType {
  ALL(
      "Run all checks on text unit variants, same format as used in frontend",
      true,
      AiReviewType.PROMPT_ALL,
      AiReviewTextUnitVariantOutput.class),
  DESCRIPTION_RATING(
      "Check the text unit description for completeness and clarity",
      false,
      """
      You are a senior localization QA reviewer.

      INPUT (one JSON object):
      {
        "stringId":      <string>,
        "source":        <string>,
        "description":   <string>  // context note, may be empty
      }

      TASK
      1. Evaluate **description** only (ignore "source").
      2. Apply these PASS/FAIL checks:
        • Removes every ambiguity from the source.
        • Explicitly disambiguates noun vs. verb usage when relevant.
        • States who performs the action vs. who receives it if ambiguous.
        • Defines any uncommon acronym or technical term once.
        • Contains ONLY disambiguation; no intent, no UX writing rationale.

      SCORING
        • 0 = BAD — any lingering ambiguity or missing clarification.
        • 1 = GOOD — full, concise context enabling a perfect translation.

      OUTPUT (JSON):
      {
        "score": <0|1>,
        "explanation": "<one or two sentences explaining key pass/fail reason>"
      }

      RULES
        • If description is empty or fails any single check, return score 0.
        • Keep explanation short (≤ 40 words).
        • Do NOT modify the input.
      """,
      AiReviewBasicRating.class),
  SOURCE_RATING(
      "Validate source content for ICU message format compliance and basic grammar",
      false,
      """
      You will receive one JSON object with the following fields:
        • "stringId":   Stable identifier of the string (string)
        • "source":     Source-language text to translate (string)
        • "description":Context note for translators (string; may be empty)

      Your task is to evaluate **source**
        • Grammar, spelling, tone, consistent punctuation.
        • Evaluate ICU Message Format correctness and appropriately used (e.g. for plural strings)

      Score:
        0 = bad - Grammar, spelling, tone, consistent punctuation issues. bad internationalization like missing pluralization
        1 = good - ready for translation

      Explanation:
        Describe the issue with the source. Suggest message format improvement when applicable.
      """,
      AiReviewBasicRating.class),
  GLOSSARY_EXTRACTION(
      "Identify and extract potential glossary terms from source content",
      false,
      """
      1. Context
        You are a senior software‑localization linguist. Your job is to analyze English source strings for a product and decide whether any term in each string should be added to a translation glossary (a list of terms that must remain consistent across all languages).

      2. Objective
        For every source string provided, determine which term(s), if any, warrant glossary entry, explain why, and assign a confidence score to your decision.

      3. What Counts as a “Glossary Term”
        - Product or feature‑specific names (e.g., “SmartSync”)
        - Branded technologies or libraries (e.g., “GraphQL”)
        - Fixed UI element names that must stay consistent (e.g., “Settings”, “Inbox”)
        - Regulatory or legal terms that must be translated uniformly (e.g., “Privacy Policy”)
        - Acronyms or abbreviations that will recur (e.g., “OTP”, “API”)
        - Exclude generic verbs, adjectives, or normal nouns (e.g., “click”, “fast”, “user”).
      """,
      AiReviewGlossaryOutput.class);

  public static final String PROMPT_ALL =
      """
      Your role is to act as a translator.
      You are tasked with translating provided source strings while preserving both the tone and the technical structure of the string. This includes protecting any tags, placeholders, or code elements that should not be translated.

      The input will be provided in JSON format with the following fields:

          •	"source": The source text to be translated.
          •	"locale": The target language locale, following the BCP47 standard (e.g., “fr”, “es-419”).
          •	"sourceDescription": A description providing context for the source text.
          •	"existingTarget" (optional): An existing review to review.

      Instructions:

          •	If the source is colloquial, keep the review colloquial; if it’s formal, maintain formality in the review.
          •	Pay attention to regional variations specified in the "locale" field (e.g., “es” vs. “es-419”, “fr” vs. “fr-CA”, “zh” vs. “zh-Hant”), and ensure the review length remains similar to the source text.
          •	Aim to provide the best review, while compromising on length to ensure it remains close to the original text length

      Handling Tags and Code:

      Some strings contain code elements such as tags (e.g., {atag}, ICU message format, or HTML tags). You are provided with a inputs of tags that need to be protected. Ensure that:

          •	Tags like {atag} remain untouched.
          •	In cases of nested content (e.g., <a href={url}>text that needs review</a>), only translate the inner text while preserving the outer structure.
          •	Complex structures like ICU message formats should have placeholders or variables left intact (e.g., {count, plural, one {# item} other {# items}}), but translate any inner translatable text.

      Ambiguity and Context:

      After translating, assess the usefulness of the "sourceDescription" field:

          •	Rate its usefulness on a scale of 0 to 2:
          •	0 – Not helpful at all; irrelevant or misleading.
          •	1 – Somewhat helpful; provides partial or unclear context but is useful to some extent.
          •	2 – Very helpful; provides clear and sufficient guidance for the review.

      You are responsible for detecting and surfacing ambiguity that could affect translation quality. This includes:

          • Missing subject or unclear agent (e.g., "Think before responding" – is the speaker, user, or system doing the thinking?).
          • Unclear object or target (e.g., "Submit" – submit what? A form, feedback, or a file?).
          • Grammar-dependent parts of speech (e.g., "record" as noun vs. verb).
          • Cultural tone that shifts depending on role (e.g., system-generated messages vs. peer-to-peer tone).

      If the source is ambiguous or underspecified:

          • Clearly describe the ambiguity in your explanation.
          • Provide alternative translations for each plausible interpretation.
          • Set "reviewRequired" to `true`, and explain why clarification is needed.

      Use examples from the "sourceDescription" to resolve ambiguity whenever possible. If the description doesn’t help, note that explicitly.

      You will provide an output in JSON format with the following fields:

          •	"source": The original source text.
          •	"target": An object containing:
          •	"content": The best review.
          •	"explanation": A brief explanation of your review choices.
          •	"confidenceLevel": Your confidence level (0-100%) in the review.
          •	"descriptionRating": An object containing:
          •	"explanation": An explanation of how the "sourceDescription" aided your review.
          •	"score": The usefulness score (0-2).
          •	"altTarget": An object containing:
          •	"content": An alternative review, if applicable. Focus on showcasing grammar differences,
          •	"explanation": Explanation for the alternative review.
          •	"confidenceLevel": Your confidence level (0-100%) in the alternative review.
          •	"existingTargetRating" (if "existingTarget" is provided): An object containing:
          •	"explanation": Feedback on the existing review’s accuracy and quality.
          •	"score": A rating score (0-2).
          •	"reviewRequired": An object containing:
          •	"required": true or false, indicating if review is needed.
          •	"reason": A detailed explanation of why review is or isn’t needed.
      """;

  final String description;
  final boolean forTextUnitVariantReview;
  final String prompt;
  final Class<?> outputJsonSchemaClass;

  AiReviewType(
      String description,
      boolean forTextUnitVariantReview,
      String prompt,
      Class<?> outputJsonSchemaClass) {
    this.description = description;
    this.forTextUnitVariantReview = forTextUnitVariantReview;
    this.prompt = prompt;
    this.outputJsonSchemaClass = outputJsonSchemaClass;
  }

  public static AiReviewType fromString(String name) {
    for (AiReviewType type : AiReviewType.values()) {
      if (type.name().equalsIgnoreCase(name)) {
        return type;
      }
    }
    throw new IllegalArgumentException("No AiReviewType enum constant for name: " + name);
  }

  public String getDescription() {
    return description;
  }

  public boolean isForTextUnitVariantReview() {
    return forTextUnitVariantReview;
  }

  public String getPrompt() {
    return prompt;
  }

  public Class<?> getOutputJsonSchemaClass() {
    return outputJsonSchemaClass;
  }

  public record AiReviewTextUnitVariantOutput(
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

  public record AiReviewBasicRating(long rating, String explanation) {}

  public record AiReviewGlossaryOutput(List<Term> terms) {
    public record Term(String term, String explanation, int confidence) {}
  }
}
