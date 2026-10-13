package com.box.l10n.mojito.cli.command.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.box.l10n.mojito.cli.command.checks.CheckerRuleId;
import com.box.l10n.mojito.cli.command.checks.CliCheckResult;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.github.GithubClient;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GithubReviewCommentServiceTest {

  private static final int DEFAULT_LINE_ERROR_ALLOWANCE = 5;

  private GithubReviewCommentService createService(int lineNumberErrorAllowance) {
    return new GithubReviewCommentService(lineNumberErrorAllowance, new SimpleMeterRegistry());
  }

  private CliCheckResult createCliCheckResult(
      boolean isError, String checkName, Map<String, CliCheckResult.CheckFailure> fieldFailures) {
    CliCheckResult checkResult = new CliCheckResult(isError, checkName);
    checkResult.appendToFailuresMap(fieldFailures);
    return checkResult;
  }

  private AssetExtractorTextUnit createAssetExtractorTextUnit(String source, Set<String> usages) {
    AssetExtractorTextUnit textUnit = new AssetExtractorTextUnit();
    textUnit.setName(source);
    textUnit.setSource(source);
    textUnit.setUsages(usages);
    return textUnit;
  }

  private AssetExtractionDiff createDiff(AssetExtractorTextUnit... textUnits) {
    AssetExtractionDiff diff = new AssetExtractionDiff();
    diff.setAddedTextunits(List.of(textUnits));
    return diff;
  }

  private Map<String, CliCheckResult.CheckFailure> createFieldFailures(
      String source, CheckerRuleId ruleId, String message) {
    return Map.of(source, new CliCheckResult.CheckFailure(ruleId, message));
  }

  @Test
  void generateReviewComments_withUsagesOnModifiedLines() {
    // Arrange
    GithubReviewCommentService service = createService(DEFAULT_LINE_ERROR_ALLOWANCE);

    AssetExtractionDiff diff =
        createDiff(
            createAssetExtractorTextUnit("source1", Set.of("file1.java:10", "file2.java:20")));

    CliCheckResult checkResult =
        createCliCheckResult(
            true,
            "TestCheck",
            createFieldFailures(
                "source1", CheckerRuleId.EMPTY_PLACEHOLDER_COMMENT, "Failure message 1"));

    Map<String, Set<Integer>> githubModifiedLines =
        Map.of("file1.java", Set.of(10), "file2.java", Set.of(20));

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), githubModifiedLines, "repoName", "");

    // Assert
    List<GithubClient.ReviewComment> orderedReviewComments =
        reviewComments.stream()
            .sorted(Comparator.comparing(GithubClient.ReviewComment::getPath))
            .toList();
    assertThat(orderedReviewComments).hasSize(2);
    assertThat(orderedReviewComments.getFirst().getPath()).isEqualTo("file1.java");
    assertThat(orderedReviewComments.getFirst().getLine()).isEqualTo(10);
    assertThat(orderedReviewComments.getFirst().getBody()).contains("I18N_TestCheck");
    assertThat(orderedReviewComments.getFirst().getBody()).contains("Error");
    assertThat(orderedReviewComments.getFirst().getBody()).contains("Failure message 1");

    assertThat(orderedReviewComments.get(1).getPath()).isEqualTo("file2.java");
    assertThat(orderedReviewComments.get(1).getLine()).isEqualTo(20);
  }

  @Test
  void generateReviewComments_withNoUsages() {
    // Arrange
    GithubReviewCommentService service = createService(DEFAULT_LINE_ERROR_ALLOWANCE);

    AssetExtractionDiff diff =
        createDiff(createAssetExtractorTextUnit("source1", Collections.emptySet()));

    CliCheckResult checkResult =
        createCliCheckResult(
            false,
            "TestCheck",
            createFieldFailures("source1", CheckerRuleId.EMPTY_COMMENT_STRING, "Failure message"));

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), new HashMap<>(), "repoName", "");

    // Assert - no review comments should be generated for text units without usages
    assertThat(reviewComments).isEmpty();
  }

  @Test
  void generateReviewComments_usageInFileNotChangedInPr_isDiscarded() {
    // Arrange
    GithubReviewCommentService service = createService(DEFAULT_LINE_ERROR_ALLOWANCE);

    AssetExtractionDiff diff =
        createDiff(createAssetExtractorTextUnit("source1", Set.of("untouched.java:10")));

    CliCheckResult checkResult =
        createCliCheckResult(
            true,
            "TestCheck",
            createFieldFailures("source1", CheckerRuleId.EMPTY_PLACEHOLDER_COMMENT, "Message"));

    Map<String, Set<Integer>> githubModifiedLines = Map.of("other.java", Set.of(10));

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), githubModifiedLines, "repoName", "");

    // Assert - GitHub rejects comments on files that are not part of the PR diff
    assertThat(reviewComments).isEmpty();
  }

  @Test
  void generateReviewComments_onlyUsagesInChangedFilesAreKept() {
    // Arrange
    GithubReviewCommentService service = createService(DEFAULT_LINE_ERROR_ALLOWANCE);

    AssetExtractionDiff diff =
        createDiff(
            createAssetExtractorTextUnit(
                "source1", Set.of("changed.java:10", "untouched.java:10")));

    CliCheckResult checkResult =
        createCliCheckResult(
            true,
            "TestCheck",
            createFieldFailures("source1", CheckerRuleId.EMPTY_PLACEHOLDER_COMMENT, "Message"));

    Map<String, Set<Integer>> githubModifiedLines = Map.of("changed.java", Set.of(10));

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), githubModifiedLines, "repoName", "");

    // Assert
    assertThat(reviewComments).hasSize(1);
    assertThat(reviewComments.getFirst().getPath()).isEqualTo("changed.java");
    assertThat(reviewComments.getFirst().getLine()).isEqualTo(10);
  }

  @Test
  void generateReviewComments_noModifiedLines_discardsAllUsages() {
    // Arrange
    GithubReviewCommentService service = createService(DEFAULT_LINE_ERROR_ALLOWANCE);

    AssetExtractionDiff diff =
        createDiff(createAssetExtractorTextUnit("source1", Set.of("file1.java:10")));

    CliCheckResult checkResult =
        createCliCheckResult(
            true,
            "TestCheck",
            createFieldFailures("source1", CheckerRuleId.EMPTY_PLACEHOLDER_COMMENT, "Message"));

    // Act - the file is listed in the PR but has no added line
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), Map.of("file1.java", Set.of()), "repoName", "");

    // Assert
    assertThat(reviewComments).isEmpty();
  }

  @Test
  void generateReviewComments_errorLevel() {
    // Arrange
    GithubReviewCommentService service = createService(DEFAULT_LINE_ERROR_ALLOWANCE);

    AssetExtractionDiff diff =
        createDiff(createAssetExtractorTextUnit("source1", Set.of("file1.java:15")));

    CliCheckResult checkResult =
        createCliCheckResult(
            true,
            "ErrorCheck",
            createFieldFailures(
                "source1", CheckerRuleId.EMPTY_PLACEHOLDER_COMMENT, "Error message"));

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), Map.of("file1.java", Set.of(15)), "repoName", "");

    // Assert
    assertThat(reviewComments).hasSize(1);
    assertThat(reviewComments.getFirst().getBody()).contains("I18N_ErrorCheck");
    assertThat(reviewComments.getFirst().getBody()).contains("Error");
    assertThat(reviewComments.getFirst().getBody()).contains("Error message");
    assertThat(reviewComments.getFirst().getBody()).doesNotContain("Warning");
  }

  @Test
  void generateReviewComments_warningLevel() {
    // Arrange
    GithubReviewCommentService service = createService(DEFAULT_LINE_ERROR_ALLOWANCE);

    AssetExtractionDiff diff =
        createDiff(createAssetExtractorTextUnit("source1", Set.of("file1.java:15")));

    // Create warning (not error) check result
    CliCheckResult checkResult =
        createCliCheckResult(
            false,
            "WarningCheck",
            createFieldFailures(
                "source1", CheckerRuleId.EMPTY_CONTEXT_AND_COMMENT_STRINGS, "Warning message"));

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), Map.of("file1.java", Set.of(15)), "repoName", "");

    // Assert
    assertThat(reviewComments).hasSize(1);
    assertThat(reviewComments.getFirst().getBody()).contains("Warning");
    assertThat(reviewComments.getFirst().getBody()).contains("⚠️");
    assertThat(reviewComments.getFirst().getBody()).doesNotContain("Error");
  }

  @Test
  void generateReviewComments_withFileUriPrefix() {
    // Arrange
    GithubReviewCommentService service = createService(DEFAULT_LINE_ERROR_ALLOWANCE);

    AssetExtractionDiff diff =
        createDiff(createAssetExtractorTextUnit("source1", Set.of("/project/src/file1.java:10")));

    CliCheckResult checkResult =
        createCliCheckResult(
            true,
            "TestCheck",
            createFieldFailures("source1", CheckerRuleId.CONTROL_CHARACTER_DETECTED, "Message"));

    // The PR files are relative to the repository, ie. without the mount path prefix
    Map<String, Set<Integer>> githubModifiedLines = Map.of("src/file1.java", Set.of(10));

    // Act - remove prefix "/project/"
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), githubModifiedLines, "repoName", "/project/");

    // Assert
    assertThat(reviewComments).hasSize(1);
    assertThat(reviewComments.getFirst().getPath()).isEqualTo("src/file1.java");
    assertThat(reviewComments.getFirst().getLine()).isEqualTo(10);
  }

  @Test
  void generateReviewComments_lineBeforeModifiedLineWithinAllowance_isMovedOntoTheModifiedLine() {
    // Arrange
    GithubReviewCommentService service = createService(2);

    AssetExtractionDiff diff =
        createDiff(createAssetExtractorTextUnit("source1", Set.of("file1.py:10")));

    CliCheckResult checkResult =
        createCliCheckResult(
            true,
            "TestCheck",
            createFieldFailures(
                "source1", CheckerRuleId.MISSING_DESCRIPTION_FOR_NUMERIC_PLACEHOLDER, "Message"));

    // Modified lines: line 10 is not modified, but line 11 is
    Map<String, Set<Integer>> githubModifiedLines = Map.of("file1.py", Set.of(11, 12, 13));

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), githubModifiedLines, "repoName", "");

    // Assert - the usage is accepted and reported on the first modified line following it
    assertThat(reviewComments).hasSize(1);
    assertThat(reviewComments.getFirst().getPath()).isEqualTo("file1.py");
    assertThat(reviewComments.getFirst().getLine()).isEqualTo(11);
  }

  @Test
  void generateReviewComments_modifiedLineAtTheEdgeOfTheAllowance_isAccepted() {
    // Arrange
    GithubReviewCommentService service = createService(DEFAULT_LINE_ERROR_ALLOWANCE);

    AssetExtractionDiff diff =
        createDiff(createAssetExtractorTextUnit("source1", Set.of("file1.py:10")));

    CliCheckResult checkResult =
        createCliCheckResult(
            true,
            "TestCheck",
            createFieldFailures("source1", CheckerRuleId.EMPTY_PLACEHOLDER_COMMENT, "Message"));

    Map<String, Set<Integer>> githubModifiedLines = Map.of("file1.py", Set.of(15));

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), githubModifiedLines, "repoName", "");

    // Assert
    assertThat(reviewComments).hasSize(1);
    assertThat(reviewComments.getFirst().getLine()).isEqualTo(15);
  }

  @Test
  void generateReviewComments_modifiedLineBeyondTheAllowance_isDiscarded() {
    // Arrange
    GithubReviewCommentService service = createService(DEFAULT_LINE_ERROR_ALLOWANCE);

    AssetExtractionDiff diff =
        createDiff(createAssetExtractorTextUnit("source1", Set.of("file1.py:10")));

    CliCheckResult checkResult =
        createCliCheckResult(
            true,
            "TestCheck",
            createFieldFailures("source1", CheckerRuleId.EMPTY_PLACEHOLDER_COMMENT, "Message"));

    Map<String, Set<Integer>> githubModifiedLines = Map.of("file1.py", Set.of(16));

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), githubModifiedLines, "repoName", "");

    // Assert
    assertThat(reviewComments).isEmpty();
  }

  @Test
  void generateReviewComments_lineAfterTheModifiedLine_isMovedOntoTheClosestModifiedLine() {
    // Arrange
    GithubReviewCommentService service = createService(DEFAULT_LINE_ERROR_ALLOWANCE);

    AssetExtractionDiff diff =
        createDiff(createAssetExtractorTextUnit("source1", Set.of("file1.py:10")));

    CliCheckResult checkResult =
        createCliCheckResult(
            true,
            "TestCheck",
            createFieldFailures("source1", CheckerRuleId.EMPTY_PLACEHOLDER_COMMENT, "Message"));

    // Only lines before the usage were modified, as when only the comment written above the
    // translation function call is changed
    Map<String, Set<Integer>> githubModifiedLines = Map.of("file1.py", Set.of(5, 8, 9));

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), githubModifiedLines, "repoName", "");

    // Assert
    assertThat(reviewComments).hasSize(1);
    assertThat(reviewComments.getFirst().getPath()).isEqualTo("file1.py");
    assertThat(reviewComments.getFirst().getLine()).isEqualTo(9);
  }

  @Test
  void generateReviewComments_modifiedLinesBeforeAndAfterTheUsage_preferTheLinesAfterIt() {
    // Arrange
    GithubReviewCommentService service = createService(DEFAULT_LINE_ERROR_ALLOWANCE);

    AssetExtractionDiff diff =
        createDiff(createAssetExtractorTextUnit("source1", Set.of("file1.py:10")));

    CliCheckResult checkResult =
        createCliCheckResult(
            true,
            "TestCheck",
            createFieldFailures("source1", CheckerRuleId.EMPTY_PLACEHOLDER_COMMENT, "Message"));

    // Line 9 is closer to the usage than line 12, but the lines following the usage are searched
    // first
    Map<String, Set<Integer>> githubModifiedLines = Map.of("file1.py", Set.of(9, 12));

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), githubModifiedLines, "repoName", "");

    // Assert
    assertThat(reviewComments).hasSize(1);
    assertThat(reviewComments.getFirst().getPath()).isEqualTo("file1.py");
    assertThat(reviewComments.getFirst().getLine()).isEqualTo(12);
  }

  @Test
  void generateReviewComments_modifiedLineBeforeTheUsageAtTheEdgeOfTheAllowance_isAccepted() {
    // Arrange
    GithubReviewCommentService service = createService(DEFAULT_LINE_ERROR_ALLOWANCE);

    AssetExtractionDiff diff =
        createDiff(createAssetExtractorTextUnit("source1", Set.of("file1.py:10")));

    CliCheckResult checkResult =
        createCliCheckResult(
            true,
            "TestCheck",
            createFieldFailures("source1", CheckerRuleId.EMPTY_PLACEHOLDER_COMMENT, "Message"));

    // Line 5 is exactly lineNumberErrorAllowance lines before the usage
    Map<String, Set<Integer>> githubModifiedLines = Map.of("file1.py", Set.of(5));

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), githubModifiedLines, "repoName", "");

    // Assert
    assertThat(reviewComments).hasSize(1);
    assertThat(reviewComments.getFirst().getPath()).isEqualTo("file1.py");
    assertThat(reviewComments.getFirst().getLine()).isEqualTo(5);
  }

  @Test
  void generateReviewComments_modifiedLineBeforeTheUsageBeyondTheAllowance_isDiscarded() {
    // Arrange
    GithubReviewCommentService service = createService(DEFAULT_LINE_ERROR_ALLOWANCE);

    AssetExtractionDiff diff =
        createDiff(createAssetExtractorTextUnit("source1", Set.of("file1.py:10")));

    CliCheckResult checkResult =
        createCliCheckResult(
            true,
            "TestCheck",
            createFieldFailures("source1", CheckerRuleId.EMPTY_PLACEHOLDER_COMMENT, "Message"));

    // Line 4 is one line further than lineNumberErrorAllowance before the usage
    Map<String, Set<Integer>> githubModifiedLines = Map.of("file1.py", Set.of(4));

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), githubModifiedLines, "repoName", "");

    // Assert
    assertThat(reviewComments).isEmpty();
  }

  @Test
  void generateReviewComments_multipleCheckFailures() {
    // Arrange
    GithubReviewCommentService service = createService(DEFAULT_LINE_ERROR_ALLOWANCE);

    AssetExtractionDiff diff =
        createDiff(
            createAssetExtractorTextUnit("source1", Set.of("file1.java:10")),
            createAssetExtractorTextUnit("source2", Set.of("file2.java:20")));

    CliCheckResult checkResult1 =
        createCliCheckResult(
            true,
            "Check1",
            createFieldFailures("source1", CheckerRuleId.EMPTY_PLACEHOLDER_COMMENT, "Failure 1"));
    CliCheckResult checkResult2 =
        createCliCheckResult(
            false,
            "Check2",
            createFieldFailures("source2", CheckerRuleId.EMPTY_COMMENT_STRING, "Failure 2"));

    Map<String, Set<Integer>> githubModifiedLines =
        Map.of("file1.java", Set.of(10), "file2.java", Set.of(20));

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult1, checkResult2),
            List.of(diff),
            githubModifiedLines,
            "repoName",
            "");

    // Assert
    assertThat(reviewComments).hasSize(2);
    assertThat(reviewComments.get(0).getBody()).contains("I18N_Check1");
    assertThat(reviewComments.get(0).getBody()).contains("Error");
    assertThat(reviewComments.get(1).getBody()).contains("I18N_Check2");
    assertThat(reviewComments.get(1).getBody()).contains("Warning");
  }

  @Test
  void generateReviewComments_invalidUsageFormat() {
    // Arrange
    GithubReviewCommentService service = createService(DEFAULT_LINE_ERROR_ALLOWANCE);

    AssetExtractionDiff diff =
        createDiff(
            createAssetExtractorTextUnit(
                "source1", Set.of("invalid_format", "file1.java:notanumber")));

    CliCheckResult checkResult =
        createCliCheckResult(
            true,
            "TestCheck",
            createFieldFailures("source1", CheckerRuleId.EMPTY_PLACEHOLDER_COMMENT, "Message"));

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), Map.of("file1.java", Set.of(10)), "repoName", "");

    // Assert - invalid usages should be filtered out
    assertThat(reviewComments).isEmpty();
  }

  @Test
  void generateReviewComments_allowanceAppliesToNonCommentRelatedChecks() {
    // Arrange
    // The allowance does not depend on the rule being comment related nor on the file extension:
    // any usage close enough before a modified line is kept
    GithubReviewCommentService service = createService(DEFAULT_LINE_ERROR_ALLOWANCE);

    String fileUri = "src/main/java/Service.java";
    AssetExtractionDiff diff =
        createDiff(createAssetExtractorTextUnit("javaSource", Set.of(fileUri + ":50")));

    CliCheckResult checkResult =
        createCliCheckResult(
            true,
            "TestCheck",
            createFieldFailures(
                "javaSource", CheckerRuleId.CONTROL_CHARACTER_DETECTED, "Test failure"));

    Map<String, Set<Integer>> githubModifiedLines = Map.of(fileUri, Set.of(1, 2, 3, 52, 100));

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), githubModifiedLines, "test-repo", "");

    // Assert
    assertThat(reviewComments).hasSize(1);
    assertThat(reviewComments.getFirst().getPath()).isEqualTo(fileUri);
    assertThat(reviewComments.getFirst().getLine()).isEqualTo(52);
  }

  @Test
  void generateReviewComments_usageOutsideOfTheDiffContext_isMovedOntoTheModifiedLine() {
    // Arrange
    // The usage is on the line where the call to the translation function starts, 4 lines before
    // the modified lines: GitHub only includes 3 unchanged lines around a change in the PR diff, so
    // a comment on the usage line would be rejected
    GithubReviewCommentService service = createService(DEFAULT_LINE_ERROR_ALLOWANCE);

    String fileUri = "webapp/app/getTemplateForSearchScope.ts";
    AssetExtractionDiff diff =
        createDiff(createAssetExtractorTextUnit("source1", Set.of(fileUri + ":78")));

    CliCheckResult checkResult =
        createCliCheckResult(
            false,
            "AI_CHECKER",
            createFieldFailures("source1", CheckerRuleId.AI_CHECKER_SUGGESTION, "Message"));

    Map<String, Set<Integer>> githubModifiedLines = Map.of(fileUri, Set.of(82, 83));

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult), List.of(diff), githubModifiedLines, "repoName", "");

    // Assert
    assertThat(reviewComments).hasSize(1);
    assertThat(reviewComments.getFirst().getPath()).isEqualTo(fileUri);
    assertThat(reviewComments.getFirst().getLine()).isEqualTo(82);
  }

  @Test
  void generateReviewComments_withNoUsages_commentsOnDeclarationLocation() {
    // Arrange
    GithubReviewCommentService service = createService(DEFAULT_LINE_ERROR_ALLOWANCE);

    AssetExtractorTextUnit textUnit = createAssetExtractorTextUnit("source1", null);
    textUnit.setDeclarationLocation("res/values/strings.xml:4");

    CliCheckResult checkResult =
        createCliCheckResult(
            true,
            "TestCheck",
            createFieldFailures(
                "source1", CheckerRuleId.EMPTY_PLACEHOLDER_COMMENT, "Failure message"));

    Map<String, Set<Integer>> githubModifiedLines = Map.of("res/values/strings.xml", Set.of(4));

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult),
            List.of(createDiff(textUnit)),
            githubModifiedLines,
            "repoName",
            "");

    // Assert
    assertThat(reviewComments).hasSize(1);
    assertThat(reviewComments.getFirst().getPath()).isEqualTo("res/values/strings.xml");
    assertThat(reviewComments.getFirst().getLine()).isEqualTo(4);
  }

  @Test
  void generateReviewComments_withUsages_ignoresDeclarationLocation() {
    // Arrange
    GithubReviewCommentService service = createService(DEFAULT_LINE_ERROR_ALLOWANCE);

    AssetExtractorTextUnit textUnit =
        createAssetExtractorTextUnit("source1", Set.of("file1.java:10"));
    textUnit.setDeclarationLocation("res/values/strings.xml:4");

    CliCheckResult checkResult =
        createCliCheckResult(
            true,
            "TestCheck",
            createFieldFailures(
                "source1", CheckerRuleId.EMPTY_PLACEHOLDER_COMMENT, "Failure message"));

    Map<String, Set<Integer>> githubModifiedLines =
        Map.of("file1.java", Set.of(10), "res/values/strings.xml", Set.of(4));

    // Act
    List<GithubClient.ReviewComment> reviewComments =
        service.generateReviewComments(
            List.of(checkResult),
            List.of(createDiff(textUnit)),
            githubModifiedLines,
            "repoName",
            "");

    // Assert
    assertThat(reviewComments).hasSize(1);
    assertThat(reviewComments.getFirst().getPath()).isEqualTo("file1.java");
    assertThat(reviewComments.getFirst().getLine()).isEqualTo(10);
  }
}
