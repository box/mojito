package com.box.l10n.mojito.cli.command.checks;

import static com.box.l10n.mojito.cli.command.extractioncheck.ExtractionCheckNotificationSender.QUOTE_MARKER;

import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.regex.PlaceholderRegularExpressions;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link AbstractCliChecker} that verifies a source string does not contain an empty placeholder
 * e.g. '{}'.
 *
 * <p>The check uses Single or Double braces regex to identify placeholders in a source string and
 * then checks that the placeholder substring contains regex 'word' values.
 *
 * @author mallen
 */
public class EmptyPlaceholderChecker extends AbstractCliChecker {

  static Logger logger = LoggerFactory.getLogger(EmptyPlaceholderChecker.class);

  private Pattern wordPattern = Pattern.compile("\\w+");

  @Override
  public CliCheckResult run(List<AssetExtractionDiff> assetExtractionDiffs) {
    CliCheckResult cliCheckResult = createCliCheckerResult();
    Set<String> failures = checkForEmptyPlaceholders(assetExtractionDiffs);
    if (!failures.isEmpty()) {
      cliCheckResult.setSuccessful(false);
      cliCheckResult.setNotificationText(buildNotificationText(failures).toString());
    }

    return cliCheckResult;
  }

  private Set<String> checkForEmptyPlaceholders(List<AssetExtractionDiff> assetExtractionDiffs) {
    return cliCheckerOptions.getParameterRegexSet().stream()
        .filter(regex -> isEmptyPlaceholderRegex(regex))
        .flatMap(
            placeholderRegularExpressions ->
                getAddedTextUnitsExcludingInconsistentComments(assetExtractionDiffs).stream()
                    .map(assetExtractorTextUnit -> assetExtractorTextUnit.getSource())
                    .filter(
                        source ->
                            isSourceStringWithEmptyPlaceholders(
                                placeholderRegularExpressions, source)))
        .collect(Collectors.toSet());
  }

  private boolean isEmptyPlaceholderRegex(PlaceholderRegularExpressions regex) {
    return regex.equals(PlaceholderRegularExpressions.SINGLE_BRACE_REGEX)
        || regex.equals(PlaceholderRegularExpressions.DOUBLE_BRACE_REGEX);
  }

  private boolean isSourceStringWithEmptyPlaceholders(
      PlaceholderRegularExpressions placeholderRegularExpressions, String source) {
    Matcher matcher = Pattern.compile(placeholderRegularExpressions.getRegex()).matcher(source);
    while (matcher.find()) {
      String placeholder = source.substring(matcher.start(), matcher.end());
      logger.debug("Found placeholder '{}' in source string '{}'", placeholder, source);
      if (!wordPattern.matcher(placeholder).find()) {
        logger.debug("Found empty placeholder '{}' in source string '{}'", placeholder, source);
        return true;
      }
    }
    return false;
  }

  private StringBuilder buildNotificationText(Set<String> failures) {
    StringBuilder notificationText = new StringBuilder();
    notificationText.append(
        "Found empty placeholders in the following source strings, please remove or update placeholders to contain a descriptive name:");
    notificationText.append(System.lineSeparator());
    notificationText.append(
        failures.stream()
            .map(source -> BULLET_POINT + QUOTE_MARKER + source + QUOTE_MARKER)
            .collect(Collectors.joining(System.lineSeparator())));
    notificationText.append(System.lineSeparator());
    return notificationText;
  }
}
