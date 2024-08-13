package com.box.l10n.mojito.cli.command.checks;

import static com.box.l10n.mojito.cli.command.extractioncheck.ExtractionCheckNotificationSender.QUOTE_MARKER;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.google.common.base.Strings;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link AbstractCliChecker} that verifies the comment and context parameters are provided and are
 * not identical.
 *
 * @author mallen
 */
public class ContextAndCommentCliChecker extends AbstractCliChecker {

  static Logger logger = LoggerFactory.getLogger(ContextAndCommentCliChecker.class);

  static class ContextAndCommentCliCheckerResult {
    String sourceString;
    String failureMessage;
    boolean failed;

    public ContextAndCommentCliCheckerResult(
        boolean failed, String sourceString, String failureMessage) {
      this.sourceString = sourceString;
      this.failureMessage = failureMessage;
      this.failed = failed;
    }

    public ContextAndCommentCliCheckerResult(boolean failed) {
      this.failed = failed;
    }

    public String getSourceString() {
      return sourceString;
    }

    public String getFailureMessage() {
      return failureMessage;
    }

    public boolean isFailed() {
      return failed;
    }
  }

  @Override
  public CliCheckResult run(List<AssetExtractionDiff> assetExtractionDiffs) {
    CliCheckResult cliCheckResult = createCliCheckerResult();
    List<ContextAndCommentCliCheckerResult> results = runChecks(assetExtractionDiffs);
    if (results.stream().anyMatch(ContextAndCommentCliCheckerResult::isFailed)) {
      cliCheckResult.setSuccessful(false);
      cliCheckResult.setNotificationText(
          "Context and comment check found failures:"
              + System.lineSeparator()
              + results.stream()
                  .filter(ContextAndCommentCliCheckerResult::isFailed)
                  .map(
                      result ->
                          BULLET_POINT
                              + "Source string "
                              + QUOTE_MARKER
                              + result.getSourceString()
                              + QUOTE_MARKER
                              + " failed check with error: "
                              + result.getFailureMessage())
                  .collect(Collectors.joining(System.lineSeparator()))
              + System.lineSeparator());
    }
    return cliCheckResult;
  }

  private Optional<Pattern> getContextCommentExcludeFilesPattern() {
    if (StringUtils.isNotBlank(this.cliCheckerOptions.getContextCommentExcludeFilesPattern())) {
      return of(Pattern.compile(this.cliCheckerOptions.getContextCommentExcludeFilesPattern()));
    }
    return empty();
  }

  private List<ContextAndCommentCliCheckerResult> runChecks(
      List<AssetExtractionDiff> assetExtractionDiffs) {
    Optional<Pattern> excludeFilesPattern = this.getContextCommentExcludeFilesPattern();
    return getAddedTextUnitsExcludingInconsistentComments(assetExtractionDiffs).stream()
        .filter(
            assetExtractorTextUnit -> {
              if (excludeFilesPattern.isPresent() && assetExtractorTextUnit.getUsages() != null) {
                return assetExtractorTextUnit.getUsages().stream()
                    .map(usage -> usage.replaceAll(":\\d+$", ""))
                    .noneMatch(usage -> excludeFilesPattern.get().matcher(usage).find());
              }
              return true;
            })
        .map(
            assetExtractorTextUnit ->
                getContextAndCommentCliCheckerResult(
                    assetExtractorTextUnit, checkTextUnit(assetExtractorTextUnit)))
        .collect(Collectors.toList());
  }

  private ContextAndCommentCliCheckerResult getContextAndCommentCliCheckerResult(
      AssetExtractorTextUnit assetExtractorTextUnit, String failureText) {
    ContextAndCommentCliCheckerResult result;
    if (failureText != null) {
      logger.debug(
          "'{}' source string failed check with error: {}",
          assetExtractorTextUnit.getSource(),
          failureText);
      result =
          new ContextAndCommentCliCheckerResult(
              true, assetExtractorTextUnit.getSource(), failureText);
    } else {
      result = new ContextAndCommentCliCheckerResult(false);
    }
    return result;
  }

  private String checkTextUnit(AssetExtractorTextUnit assetExtractorTextUnit) {
    String failureText = null;
    String[] splitNameArray = assetExtractorTextUnit.getName().split("---");
    String context = null;
    if (isPlural(assetExtractorTextUnit) && cliCheckerOptions.getPluralsSkipped()) {
      return failureText;
    }
    if (splitNameArray.length > 1) {
      context = splitNameArray[1];
    }
    String comment = assetExtractorTextUnit.getComments();

    if (!isBlank(context) && !isBlank(comment)) {
      if (context.trim().equalsIgnoreCase(comment.trim())) {
        failureText = "Context & comment strings should not be identical.";
      }
    } else if (isBlank(context) && isBlank(comment)) {
      failureText = "Context and comment strings are both empty.";
    } else if (isBlank(context)) {
      failureText = "Context string is empty.";
    } else if (isBlank(comment)) {
      failureText = "Comment string is empty.";
    }

    return failureText;
  }

  private boolean isPlural(AssetExtractorTextUnit assetExtractorTextUnit) {
    return !Strings.isNullOrEmpty(assetExtractorTextUnit.getPluralForm());
  }

  private boolean isBlank(String string) {
    return StringUtils.isBlank(string);
  }
}
