package com.box.l10n.mojito.cli.command.checks;

import static com.box.l10n.mojito.cli.command.extractioncheck.ExtractionCheckNotificationSender.QUOTE_MARKER;
import static java.util.stream.Collectors.toList;

import com.box.l10n.mojito.apiclient.AIServiceClient;
import com.box.l10n.mojito.apiclient.model.AICheckRequest;
import com.box.l10n.mojito.apiclient.model.AICheckResponse;
import com.box.l10n.mojito.apiclient.model.AICheckResult;
import com.box.l10n.mojito.cli.command.CommandException;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.google.common.base.Strings;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

@Configurable
public class AIChecker extends AbstractCliChecker {

  private static final int RETRY_MAX_ATTEMPTS = 10;

  private static final int RETRY_MIN_DURATION_SECONDS = 1;

  private static final int RETRY_MAX_BACKOFF_DURATION_SECONDS = 60;

  static Logger logger = LoggerFactory.getLogger(AIChecker.class);

  RetryBackoffSpec retryConfiguration =
      Retry.backoff(RETRY_MAX_ATTEMPTS, Duration.ofSeconds(RETRY_MIN_DURATION_SECONDS))
          .maxBackoff(Duration.ofSeconds(RETRY_MAX_BACKOFF_DURATION_SECONDS));

  @Autowired AIServiceClient aiServiceClient;

  @Override
  public CliCheckResult run(List<AssetExtractionDiff> assetExtractionDiffs) {

    logger.debug("Running AI checks");

    List<AssetExtractorTextUnit> textUnits =
        assetExtractionDiffs.stream()
            .flatMap(diff -> diff.getAddedTextunits().stream())
            .collect(toList());

    String repositoryName = cliCheckerOptions.getRepositoryName();

    if (Strings.isNullOrEmpty(repositoryName)) {
      throw new CommandException(
          "Repository name must be provided in checker options when using OpenAI checks.");
    }

    List<com.box.l10n.mojito.apiclient.model.AssetExtractorTextUnit> assetExtractorTextUnits =
        textUnits.stream()
            .map(
                textUnit -> {
                  com.box.l10n.mojito.apiclient.model.AssetExtractorTextUnit
                      assetExtractorTextUnit =
                          new com.box.l10n.mojito.apiclient.model.AssetExtractorTextUnit();
                  assetExtractorTextUnit.setName(textUnit.getName());
                  assetExtractorTextUnit.setSource(textUnit.getSource());
                  assetExtractorTextUnit.setComments(textUnit.getComments());
                  assetExtractorTextUnit.setPluralForm(textUnit.getPluralForm());
                  assetExtractorTextUnit.setUsages(
                      textUnit.getUsages() == null ? null : textUnit.getUsages().stream().toList());
                  return assetExtractorTextUnit;
                })
            .toList();

    AICheckRequest aiCheckRequest = new AICheckRequest();
    aiCheckRequest.setTextUnits(assetExtractorTextUnits);
    aiCheckRequest.setRepositoryName(repositoryName);

    return Mono.fromCallable(() -> executeChecks(aiCheckRequest))
        .retryWhen(retryConfiguration)
        .doOnError(ex -> logger.error("Failed to run AI checks: {}", ex.getMessage(), ex))
        .onErrorReturn(getRetriesExhaustedResult())
        .block();
  }

  private CliCheckResult executeChecks(AICheckRequest aiCheckRequest) {
    AICheckResponse response = aiServiceClient.executeAIChecks(aiCheckRequest);

    if (response.isError()) {
      throw new CommandException("Failed to run AI checks: " + response.getErrorMessage());
    }

    Map<String, List<AICheckResult>> failureMap = new HashMap<>();

    response
        .getResults()
        .forEach(
            (sourceString, openAICheckResults) -> {
              List<AICheckResult> failures =
                  openAICheckResults.stream()
                      .filter(result -> !result.isSuccess())
                      .collect(toList());

              if (!failures.isEmpty()) {
                failureMap.put(sourceString, failures);
              }
            });

    CliCheckResult cliCheckResult = createCliCheckerResult();
    if (!failureMap.isEmpty()) {
      String notificationText = buildNotificationText(failureMap);
      cliCheckResult.setNotificationText(notificationText);
      cliCheckResult.setSuccessful(false);
    }
    return cliCheckResult;
  }

  private String buildNotificationText(Map<String, List<AICheckResult>> failures) {
    StringBuilder notification = new StringBuilder();
    for (String sourceString : failures.keySet()) {
      List<AICheckResult> AICheckResults = failures.get(sourceString);
      notification.append(buildStringFailureText(sourceString, AICheckResults));
    }
    return notification.toString();
  }

  private String buildStringFailureText(String sourceString, List<AICheckResult> failures) {
    StringBuilder failureText = new StringBuilder();

    failureText
        .append("The string ")
        .append(QUOTE_MARKER)
        .append(sourceString)
        .append(QUOTE_MARKER)
        .append(" has the following issues:")
        .append(System.lineSeparator());

    for (AICheckResult failure : failures) {
      failureText
          .append(BULLET_POINT)
          .append(failure.getSuggestedFix())
          .append(System.lineSeparator());
    }

    return failureText.toString();
  }

  private CliCheckResult getRetriesExhaustedResult() {
    CliCheckResult cliCheckResult = createCliCheckerResult();
    cliCheckResult.setSuccessful(false);
    if (!Strings.isNullOrEmpty(cliCheckerOptions.getOpenAIErrorMessage())) {
      cliCheckResult.setNotificationText(cliCheckerOptions.getOpenAIErrorMessage());
    } else {
      cliCheckResult.setNotificationText("Failed to run AI checks.");
    }
    return cliCheckResult;
  }
}
