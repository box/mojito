package com.box.l10n.mojito.cli.command.checks;

import static com.box.l10n.mojito.cli.command.extractioncheck.ExtractionCheckNotificationSender.QUOTE_MARKER;

import com.box.l10n.mojito.cli.command.CommandException;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * {@link AbstractCliChecker} that checks if a text units context or comment strings match a regex
 * pattern.
 *
 * <p>If a match is found the check fails.
 *
 * @author mallen
 */
public class ContextCommentRejectPatternChecker extends AbstractCliChecker {

  @Override
  public CliCheckResult run(List<AssetExtractionDiff> assetExtractionDiffs) {
    if (StringUtils.isBlank(cliCheckerOptions.getContextCommentRejectPattern())) {
      throw new CommandException(
          "Context comment reject pattern must be provided when using REJECT_PATTERN_CHECKER.");
    }
    return getCliCheckResult(runChecks(assetExtractionDiffs));
  }

  private CliCheckResult getCliCheckResult(String failures) {
    CliCheckResult cliCheckResult = createCliCheckerResult();
    if (StringUtils.isNotBlank(failures)) {
      StringBuilder notificationTextBuilder = new StringBuilder();
      notificationTextBuilder.append(
          "Context and Comment Pattern check failed for regex '"
              + cliCheckerOptions.getContextCommentRejectPattern()
              + "':");
      notificationTextBuilder.append(System.lineSeparator());
      notificationTextBuilder.append(failures);
      cliCheckResult.setSuccessful(false);
      cliCheckResult.setNotificationText(notificationTextBuilder.toString());
    }

    return cliCheckResult;
  }

  private String runChecks(List<AssetExtractionDiff> assetExtractionDiffs) {
    Pattern pattern = Pattern.compile(cliCheckerOptions.getContextCommentRejectPattern());
    return getFailureText(assetExtractionDiffs, pattern);
  }

  private String getFailureText(List<AssetExtractionDiff> assetExtractionDiffs, Pattern pattern) {
    return getAddedTextUnitsExcludingInconsistentComments(assetExtractionDiffs).stream()
        .filter(textUnit -> isInvalidContextOrComment(pattern, textUnit))
        .map(textUnit -> buildFailureText(textUnit))
        .collect(Collectors.joining(System.lineSeparator()));
  }

  private boolean isInvalidContextOrComment(Pattern pattern, AssetExtractorTextUnit textUnit) {
    boolean commentInvalid = false;
    boolean contextInvalid = false;
    if (StringUtils.isNotBlank(textUnit.getComments())) {
      commentInvalid = pattern.matcher(textUnit.getComments()).find();
    }

    if (StringUtils.isNotBlank(getContext(textUnit))) {
      contextInvalid = pattern.matcher(getContext(textUnit)).find();
    }

    return commentInvalid || contextInvalid;
  }

  private String buildFailureText(AssetExtractorTextUnit textUnit) {
    StringBuilder sb = new StringBuilder();
    sb.append("* Source string " + QUOTE_MARKER);
    sb.append(textUnit.getSource());
    sb.append(QUOTE_MARKER + " has an invalid context or comment string.");
    return sb.toString();
  }

  private String getContext(AssetExtractorTextUnit textUnit) {
    String context = "";
    if (textUnit.getName().contains("---")) {
      context = textUnit.getName().split("---")[1];
    }
    return context;
  }
}
