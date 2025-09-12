package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Integrity checker for Mozilla Fluent messages. */
public class FluentIntegrityChecker extends AbstractTextUnitIntegrityChecker {

  private static final Logger logger = LoggerFactory.getLogger(FluentIntegrityChecker.class);

  private static final String IDENTIFIER_PATTERN = "[A-Za-z_][A-Za-z0-9_-]*";

  private static final String VARIABLE_NAME_PATTERN =
      IDENTIFIER_PATTERN + "(?:\\." + IDENTIFIER_PATTERN + ")*";

  private static final Set<String> CLDR_VARIANT_KEYS =
      Set.of("zero", "one", "two", "few", "many", "other");

  private static final Pattern NUMERIC_VARIANT_KEY = Pattern.compile("^-?\\d+(?:\\.\\d+)?$");

  private static final Pattern VARIABLE_REFERENCE_PATTERN =
      Pattern.compile("\\$(" + VARIABLE_NAME_PATTERN + ")");

  private static final Pattern SELECT_EXPRESSION_PATTERN =
      Pattern.compile("^\\s*\\$(" + VARIABLE_NAME_PATTERN + ")\\s*->", Pattern.DOTALL);

  private static final Pattern VARIANT_KEY_PATTERN =
      Pattern.compile("(\\*?)\\[\\s*([^\\]]+?)\\s*\\]");

  private static final Pattern MESSAGE_OR_TERM_REFERENCE_PATTERN =
      Pattern.compile("^(-?" + IDENTIFIER_PATTERN + "(?:\\." + IDENTIFIER_PATTERN + ")*)$");

  private static final Pattern FUNCTION_CALL_PATTERN =
      Pattern.compile("^(" + IDENTIFIER_PATTERN + ")\\s*\\(");

  @Override
  public void check(String sourceContent, String targetContent)
      throws FluentIntegrityCheckerException {

    ParseResult source = parseWithContext(sourceContent, "source");
    ParseResult target = parseWithContext(targetContent, "target");

    if (!source.variables.equals(target.variables)) {
      throw new FluentIntegrityCheckerException(
          "Target placeholders do not match source. Found: "
              + target.variables
              + ", expected: "
              + source.variables);
    }

    if (!source.references.equals(target.references)) {
      throw new FluentIntegrityCheckerException(
          "Target references do not match source. Found: "
              + target.references
              + ", expected: "
              + source.references);
    }

    if (source.selectExpressions.size() != target.selectExpressions.size()) {
      throw new FluentIntegrityCheckerException(
          "Number of Fluent select expressions in source ("
              + source.selectExpressions.size()
              + ") and target ("
              + target.selectExpressions.size()
              + ") is different");
    }

    for (int i = 0; i < source.selectExpressions.size(); i++) {
      SelectExpression sourceSelect = source.selectExpressions.get(i);
      SelectExpression targetSelect = target.selectExpressions.get(i);

      if (!Objects.equals(sourceSelect.selector, targetSelect.selector)) {
        throw new FluentIntegrityCheckerException(
            "Select expression order differs between source and target."
                + " Expected selector $"
                + sourceSelect.selector
                + " but found $"
                + targetSelect.selector);
      }

      if (targetSelect.defaultVariant == null) {
        throw new FluentIntegrityCheckerException(
            "No default (*) variant found in Fluent select expression for $"
                + targetSelect.selector);
      }

      Set<String> newVariants = new LinkedHashSet<>(targetSelect.variants);
      newVariants.removeAll(sourceSelect.variants);

      for (String newVariant : newVariants) {
        if (!CLDR_VARIANT_KEYS.contains(newVariant)
            && !NUMERIC_VARIANT_KEY.matcher(newVariant).matches()) {
          throw new FluentIntegrityCheckerException(
              "Variants for select expression $"
                  + sourceSelect.selector
                  + " contain unexpected key '"
                  + newVariant
                  + "'. Fluent plural categories must not be translated.");
        }
      }
    }
  }

  private ParseResult parseWithContext(String content, String context)
      throws FluentIntegrityCheckerException {
    try {
      return parse(content);
    } catch (FluentIntegrityCheckerException e) {
      throw new FluentIntegrityCheckerException(
          "Invalid " + context + " pattern - " + e.getMessage(), e);
    }
  }

  private ParseResult parse(String content) throws FluentIntegrityCheckerException {
    if (content == null) {
      throw new FluentIntegrityCheckerException("String to parse must not be null");
    }

    ParseResult result = new ParseResult();
    parseContent(content, result);
    return result;
  }

  private void parseContent(String content, ParseResult result)
      throws FluentIntegrityCheckerException {
    int depth = 0;
    int placeableStart = -1;

    for (int index = 0; index < content.length(); index++) {
      char current = content.charAt(index);

      if (current == '{') {
        if (isEscapedBrace(content, index)) {
          index++;
          continue;
        }
        if (depth == 0) {
          placeableStart = index + 1;
        }
        depth++;
      } else if (current == '}') {
        if (isEscapedBrace(content, index)) {
          index++;
          continue;
        }
        if (depth == 0) {
          throw new FluentIntegrityCheckerException(
              "Unbalanced closing brace detected in Fluent message");
        }
        depth--;
        if (depth == 0) {
          String placeable = content.substring(placeableStart, index);
          processPlaceable(placeable, result);
        }
      }
    }

    if (depth != 0) {
      throw new FluentIntegrityCheckerException(
          "Unbalanced opening brace detected in Fluent message");
    }
  }

  private boolean isEscapedBrace(String content, int index) {
    return (index + 1) < content.length() && content.charAt(index + 1) == content.charAt(index);
  }

  private void processPlaceable(String placeable, ParseResult result)
      throws FluentIntegrityCheckerException {
    String trimmed = placeable.trim();

    if (trimmed.isEmpty()) {
      return;
    }

    logger.debug("Processing Fluent placeable: {}", trimmed);

    Matcher selectMatcher = SELECT_EXPRESSION_PATTERN.matcher(trimmed);
    if (selectMatcher.find()) {
      String selector = selectMatcher.group(1);
      result.variables.add(selector);

      String variantsSection = trimmed.substring(selectMatcher.end());
      Matcher variantMatcher = VARIANT_KEY_PATTERN.matcher(variantsSection);

      Set<String> variants = new LinkedHashSet<>();
      String defaultVariant = null;

      while (variantMatcher.find()) {
        String variantName = variantMatcher.group(2).trim();
        variants.add(variantName);

        if ("*".equals(variantMatcher.group(1))) {
          if (defaultVariant != null && !defaultVariant.equals(variantName)) {
            throw new FluentIntegrityCheckerException(
                "Multiple default variants found in Fluent select expression for $" + selector);
          }
          defaultVariant = variantName;
        }
      }

      if (variants.isEmpty()) {
        throw new FluentIntegrityCheckerException(
            "No variants defined in Fluent select expression for $" + selector);
      }

      if (defaultVariant == null) {
        throw new FluentIntegrityCheckerException(
            "Missing default (*) variant in Fluent select expression for $" + selector);
      }

      result.selectExpressions.add(new SelectExpression(selector, variants, defaultVariant));
      parseContent(trimmed.substring(selectMatcher.end()), result);
      return;
    }

    Matcher variableMatcher = VARIABLE_REFERENCE_PATTERN.matcher(trimmed);
    while (variableMatcher.find()) {
      result.variables.add(variableMatcher.group(1));
    }

    Matcher referenceMatcher = MESSAGE_OR_TERM_REFERENCE_PATTERN.matcher(trimmed);
    if (referenceMatcher.matches()) {
      result.references.add(referenceMatcher.group(1));
      return;
    }

    Matcher functionMatcher = FUNCTION_CALL_PATTERN.matcher(trimmed);
    if (functionMatcher.find()) {
      result.references.add(functionMatcher.group(1));
    }

    parseContent(trimmed, result);
  }

  private static class ParseResult {
    final Set<String> variables = new TreeSet<>();
    final Set<String> references = new TreeSet<>();
    final List<SelectExpression> selectExpressions = new ArrayList<>();
  }

  private static class SelectExpression {
    final String selector;
    final Set<String> variants;
    final String defaultVariant;

    SelectExpression(String selector, Set<String> variants, String defaultVariant) {
      this.selector = selector;
      this.variants = new LinkedHashSet<>(variants);
      this.defaultVariant = defaultVariant;
    }
  }
}
