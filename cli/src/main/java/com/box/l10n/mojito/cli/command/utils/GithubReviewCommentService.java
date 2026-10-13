package com.box.l10n.mojito.cli.command.utils;

import com.box.l10n.mojito.cli.command.checks.CliCheckResult;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.github.GithubClient;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.box.l10n.mojito.sarif.model.ResultLevel;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Service for generating GitHub PR review comments based on i18n check failures. This
 * implementation follows the same pattern as SarifFileGenerator to ensure consistency between SARIF
 * annotations and inline PR review comments.
 */
@Component
public class GithubReviewCommentService {

  static Logger logger = LoggerFactory.getLogger(GithubReviewCommentService.class);

  private final MeterRegistry meterRegistry;

  /**
   * Number of lines a usage is allowed to sit before or after a line modified in the PR to still be
   * considered as pointing at that modification.
   */
  private final int lineNumberErrorAllowance;

  GithubReviewCommentService(
      @Value("${l10n.extraction-check.review-comments.lineNumberErrorAllowance:4}")
          int lineNumberErrorAllowance,
      MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    this.lineNumberErrorAllowance = lineNumberErrorAllowance;
  }

  private String buildCheckDisplayName(String checkName) {
    return "I18N_" + checkName;
  }

  private String buildCheckDisplayName(CliCheckResult checkResult) {
    return buildCheckDisplayName(checkResult.getCheckName());
  }

  private static boolean hasUsages(AssetExtractorTextUnit assetExtractorTextUnit) {
    return assetExtractorTextUnit != null
        && assetExtractorTextUnit.getUsages() != null
        && !assetExtractorTextUnit.getUsages().isEmpty();
  }

  private static boolean hasDeclarationLocation(AssetExtractorTextUnit assetExtractorTextUnit) {
    return assetExtractorTextUnit != null
        && assetExtractorTextUnit.getDeclarationLocation() != null
        && !assetExtractorTextUnit.getDeclarationLocation().isEmpty();
  }

  /**
   * Returns the locations to comment on for a text unit: its usages, or the line that declares it
   * in the asset when its format doesn't reference the source code.
   */
  private static Set<String> getLocations(AssetExtractorTextUnit assetExtractorTextUnit) {
    return hasUsages(assetExtractorTextUnit)
        ? assetExtractorTextUnit.getUsages()
        : Set.of(assetExtractorTextUnit.getDeclarationLocation());
  }

  /**
   * Returns the closest modified line in the given direction from the usage (1 for the lines
   * following it, -1 for the lines preceding it), up to a max (inclusive) of the {@link
   * #lineNumberErrorAllowance}.
   */
  private Optional<Integer> findClosestModifiedLine(
      Set<Integer> modifiedLines, int startLineNumber, int direction) {
    for (int i = 1; i <= this.lineNumberErrorAllowance; i++) {
      int lineNumber = startLineNumber + direction * i;
      if (modifiedLines.contains(lineNumber)) {
        return Optional.of(lineNumber);
      }
    }
    return Optional.empty();
  }

  /**
   * Returns the line to comment on for a usage, or empty when the usage does not point at a change
   * made in the PR.
   *
   * <p>A usage points at a change made in the PR when its line was modified, or when a modified
   * line sits within {@link #lineNumberErrorAllowance} lines of it. The lines following the usage
   * are searched first: the usage reported is the line where the call to the translation function
   * starts, or the line a flagged comment was extracted from, which sits a few lines before the
   * modified string declaration. The lines preceding the usage are searched next, for changes made
   * only to a comment written above the translation function call.
   *
   * <p>When the usage line is not modified, the comment is reported on the closest modified line
   * found. GitHub only accepts review comments on lines that are part of the PR diff, and the usage
   * line is not guaranteed to be one: the diff only includes a few unchanged lines around each
   * change.
   */
  private Optional<Integer> getCommentLineNumber(
      Set<Integer> modifiedLines, String repoName, String fileUri, int startLineNumber) {

    if (modifiedLines.contains(startLineNumber)) {
      return Optional.of(startLineNumber);
    }

    meterRegistry
        .counter("GithubReviewCommentService.LineNumberIncorrect", "repository", repoName)
        .increment();

    Optional<Integer> commentLineNumber =
        findClosestModifiedLine(modifiedLines, startLineNumber, 1)
            .or(() -> findClosestModifiedLine(modifiedLines, startLineNumber, -1));

    if (commentLineNumber.isEmpty()) {
      logger.debug(
          "Review Comment Generation - Discarding usage {}:{}, no line modified in the PR within {}"
              + " lines of it",
          fileUri,
          startLineNumber,
          this.lineNumberErrorAllowance);
    }

    return commentLineNumber;
  }

  private List<UsageLocation> getUsageLocations(
      AssetExtractorTextUnit assetExtractorTextUnit,
      Map<String, Set<Integer>> githubModifiedLines,
      String repoName,
      String prefixToRemoveFromFileUri) {
    return getLocations(assetExtractorTextUnit).stream()
        .map(
            usage -> {
              int colonIndex = usage.lastIndexOf(':');
              if (colonIndex == -1) {
                return null;
              }

              try {
                String fileUri = usage.substring(0, colonIndex);
                if (prefixToRemoveFromFileUri != null
                    && !prefixToRemoveFromFileUri.isEmpty()
                    && fileUri.startsWith(prefixToRemoveFromFileUri)) {
                  fileUri = fileUri.substring(prefixToRemoveFromFileUri.length());
                }
                int startLineNumber = Integer.parseInt(usage.substring(colonIndex + 1));

                Set<Integer> modifiedLines = githubModifiedLines.get(fileUri);
                if (modifiedLines == null || modifiedLines.isEmpty()) {
                  meterRegistry
                      .counter(
                          "GithubReviewCommentService.FileNotModifiedInPr", "repository", repoName)
                      .increment();
                  logger.debug(
                      "Review Comment Generation - Discarding usage {}:{}, the file has no line"
                          + " modified in the PR",
                      fileUri,
                      startLineNumber);
                  return null;
                }

                Optional<Integer> commentLineNumber =
                    getCommentLineNumber(modifiedLines, repoName, fileUri, startLineNumber);
                if (commentLineNumber.isEmpty()) {
                  return null;
                }

                return new UsageLocation(fileUri, commentLineNumber.get());

              } catch (NumberFormatException e) {
                logger.warn(
                    "Review Comment Generation - Unable to parse line number: {}",
                    usage.substring(colonIndex + 1));
                return null;
              }
            })
        .filter(Objects::nonNull)
        .toList();
  }

  private String formatReviewCommentBody(String checkName, ResultLevel level, String message) {
    String icon = level == ResultLevel.ERROR ? "🛑" : "⚠️";
    String levelText = level == ResultLevel.ERROR ? "Error" : "Warning";

    return String.format("%s **%s: %s**\n\n%s", icon, levelText, checkName, message);
  }

  /**
   * Generates GitHub PR review comments based on CLI check failures.
   *
   * <p>A text unit with no usages is commented on at the line that declares it in the asset, when
   * known. Usages pointing at a file that has no line modified in the PR are discarded, as are
   * usages that do not point at a change made in the PR. The comments that are kept are reported on
   * a modified line (see {@link #getCommentLineNumber}).
   *
   * @param cliCheckerFailures List of check failures from CLI checkers
   * @param assetExtractionDiffs List of asset extraction diffs containing text units with usages
   * @param githubModifiedLines Map of file paths to sets of modified line numbers in the PR
   * @param repoName Repository name for metrics tracking
   * @param prefixToRemoveFromFileUris Optional prefix to remove from file URIs
   * @return List of ReviewComment objects ready to be posted to GitHub
   */
  public List<GithubClient.ReviewComment> generateReviewComments(
      List<CliCheckResult> cliCheckerFailures,
      List<AssetExtractionDiff> assetExtractionDiffs,
      Map<String, Set<Integer>> githubModifiedLines,
      String repoName,
      String prefixToRemoveFromFileUris) {

    List<GithubClient.ReviewComment> reviewComments = new ArrayList<>();
    Map<String, AssetExtractorTextUnit> nameToAssetTextUnitMap =
        assetExtractionDiffs.stream()
            .flatMap(diff -> diff.getAddedTextunits().stream())
            .collect(Collectors.toMap(AssetExtractorTextUnit::getName, x -> x));

    for (CliCheckResult checkFailure : cliCheckerFailures) {
      ResultLevel resultLevel = checkFailure.isHardFail() ? ResultLevel.ERROR : ResultLevel.WARNING;
      String checkDisplayName = buildCheckDisplayName(checkFailure);

      for (Map.Entry<String, CliCheckResult.CheckFailure> entry :
          checkFailure.getNameToFailuresMap().entrySet()) {

        String source = entry.getKey();
        CliCheckResult.CheckFailure resultCheckFailure = entry.getValue();
        AssetExtractorTextUnit assetExtractorTextUnit = nameToAssetTextUnitMap.get(source);

        if (hasUsages(assetExtractorTextUnit) || hasDeclarationLocation(assetExtractorTextUnit)) {
          List<UsageLocation> usageLocations =
              getUsageLocations(
                  assetExtractorTextUnit,
                  githubModifiedLines,
                  repoName,
                  prefixToRemoveFromFileUris);

          for (UsageLocation location : usageLocations) {
            String commentBody =
                formatReviewCommentBody(
                    checkDisplayName, resultLevel, resultCheckFailure.failureMessage());
            reviewComments.add(
                new GithubClient.ReviewComment(
                    commentBody, location.getFilePath(), location.getLineNumber()));
          }
        }
      }
    }

    logger.info(
        "Generated {} review comments for repository '{}'", reviewComments.size(), repoName);
    return reviewComments;
  }

  /** Data class for representing a usage location */
  static class UsageLocation {
    private final String filePath;
    private final int lineNumber;

    public UsageLocation(String filePath, int lineNumber) {
      this.filePath = filePath;
      this.lineNumber = lineNumber;
    }

    public String getFilePath() {
      return filePath;
    }

    public int getLineNumber() {
      return lineNumber;
    }
  }
}
