package com.box.l10n.mojito.cli.command.checks;

import static com.box.l10n.mojito.cli.command.checks.CliCheckerParameters.OPEN_AI_RETRY_ERROR_MSG;
import static com.box.l10n.mojito.cli.command.checks.CliCheckerParameters.REPOSITORY_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.cli.command.CommandException;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.box.l10n.mojito.rest.ai.AIException;
import com.box.l10n.mojito.rest.client.AIServiceClient;
import com.box.l10n.mojito.rest.entity.AICheckResponse;
import com.box.l10n.mojito.rest.entity.AICheckResult;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.util.retry.Retry;

public class AICheckerTest {

  @Mock AIServiceClient AIServiceClient;

  List<AssetExtractionDiff> assetExtractionDiffs;

  AIChecker AIChecker;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    AIChecker = new AIChecker();
    AIChecker.AIServiceClient = AIServiceClient;
    List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
    AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
    assetExtractorTextUnit.setName("Some string id --- Test context");
    assetExtractorTextUnit.setSource("A source string with no errors.");
    assetExtractorTextUnit.setComments("Test comment");
    addedTUs.add(assetExtractorTextUnit);
    assetExtractionDiffs = new ArrayList<>();
    AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
    assetExtractionDiff.setAddedTextunits(addedTUs);
    assetExtractionDiffs.add(assetExtractionDiff);

    AIChecker.setCliCheckerOptions(
        new CliCheckerOptions(
            Sets.newHashSet(),
            Sets.newHashSet(),
            ImmutableMap.<String, String>builder()
                .put(REPOSITORY_NAME.getKey(), "test-repo")
                .put(OPEN_AI_RETRY_ERROR_MSG.getKey(), "Retries exhausted for OpenAI check")
                .build()));
  }

  @Test
  public void testCheckSuccess() {
    AICheckResponse AICheckResponse = new AICheckResponse();
    AICheckResponse.setError(false);
    AICheckResponse.setErrorMessage(null);
    Map<String, List<AICheckResult>> checkResults = new HashMap<>();
    List<AICheckResult> results = new ArrayList<>();

    AICheckResult AICheckResult = new AICheckResult();
    AICheckResult.setSuccess(true);
    AICheckResult.setSuggestedFix("");

    results.add(AICheckResult);
    checkResults.put("A source string with no errors.", results);
    AICheckResponse.setResults(checkResults);

    when(AIServiceClient.executeAIChecks(any())).thenReturn(AICheckResponse);
    CliCheckResult result = AIChecker.run(assetExtractionDiffs);

    verify(AIServiceClient, times(1)).executeAIChecks(any());
    assertTrue(result.isSuccessful());
    assertTrue(result.getNotificationText().isEmpty());
  }

  @Test
  public void testCheckFailure() {
    AICheckResponse AICheckResponse = new AICheckResponse();
    AICheckResponse.setError(false);
    AICheckResponse.setErrorMessage(null);
    Map<String, List<AICheckResult>> checkResults = new HashMap<>();
    List<AICheckResult> results = new ArrayList<>();

    AICheckResult AICheckResult = new AICheckResult();
    AICheckResult.setSuccess(false);
    AICheckResult.setSuggestedFix("Spelling mistake found in the source string.");

    results.add(AICheckResult);
    checkResults.put("A source string with no errors.", results);
    AICheckResponse.setResults(checkResults);

    when(AIServiceClient.executeAIChecks(any())).thenReturn(AICheckResponse);
    CliCheckResult result = AIChecker.run(assetExtractionDiffs);

    verify(AIServiceClient, times(1)).executeAIChecks(any());
    assertFalse(result.isSuccessful());
    assertFalse(result.getNotificationText().isEmpty());
    assertTrue(
        result
            .getNotificationText()
            .contains("The string `A source string with no errors.` has the following issues:"));
    assertTrue(
        result.getNotificationText().contains("Spelling mistake found in the source string."));
  }

  @Test
  public void testRetryExhausted() {
    AIChecker.retryConfiguration =
        Retry.backoff(10, Duration.ofMillis(1)).maxBackoff(Duration.ofMillis(1));
    when(AIServiceClient.executeAIChecks(any())).thenThrow(new AIException("Test error"));
    CliCheckResult result = AIChecker.run(assetExtractionDiffs);
    verify(AIServiceClient, times(11)).executeAIChecks(any());
    assertFalse(result.isSuccessful());
    assertTrue(result.getNotificationText().contains("Retries exhausted for OpenAI check"));
  }

  @Test
  public void exceptionIfNoRepoProvided() {
    AIChecker.setCliCheckerOptions(
        new CliCheckerOptions(
            Sets.newHashSet(),
            Sets.newHashSet(),
            ImmutableMap.<String, String>builder()
                .put(OPEN_AI_RETRY_ERROR_MSG.getKey(), "Retries exhausted for OpenAI check")
                .build()));
    Exception ex = assertThrows(CommandException.class, () -> AIChecker.run(assetExtractionDiffs));
    assertEquals(
        "Repository name must be provided in checker options when using OpenAI checks.",
        ex.getMessage());
  }

  @Test
  public void testErrorResultReturnedFromCli() {
    AIChecker.retryConfiguration =
        Retry.backoff(1, Duration.ofMillis(1)).maxBackoff(Duration.ofMillis(1));
    AICheckResponse AICheckResponse = new AICheckResponse();
    AICheckResponse.setError(true);
    AICheckResponse.setErrorMessage("Some error message");
    when(AIServiceClient.executeAIChecks(any())).thenReturn(AICheckResponse);
    CliCheckResult result = AIChecker.run(assetExtractionDiffs);
    assertFalse(result.isSuccessful());
    assertTrue(result.getNotificationText().contains("Retries exhausted for OpenAI check"));
  }
}
