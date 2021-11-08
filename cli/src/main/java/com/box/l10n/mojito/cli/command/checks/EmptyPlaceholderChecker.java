package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.regex.PlaceholderRegularExpressions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Checker that verifies a source string does not contain an empty placeholder e.g. '{}'.
 *
 * The check uses the provided regex patterns to identify placeholders in a source string and then checks that the
 * placeholder substring contains regex 'word' values.
 *
 * @author mallen
 */
public class EmptyPlaceholderChecker extends AbstractCliChecker {

    static Logger logger = LoggerFactory.getLogger(EmptyPlaceholderChecker.class);

    private Pattern wordPattern = Pattern.compile("\\w+");

    @Override
    public CliCheckResult run() {
        CliCheckResult cliCheckResult = new CliCheckResult(isHardFail(), CliCheckerType.EMPTY_PLACEHOLDER_CHECKER.name());
        Set<String> failures = checkForEmptyPlaceholders();
        if (!failures.isEmpty()) {
            cliCheckResult.setSuccessful(false);
            cliCheckResult.setNotificationText(buildNotificationText(failures).toString());
        }

        return cliCheckResult;
    }

    private Set<String> checkForEmptyPlaceholders() {
        return cliCheckerOptions.getParameterRegexSet().stream()
                .filter(regex -> isEmptyPlaceholderRegex(regex))
                .flatMap(placeholderRegularExpressions -> getAddedTextUnits().stream()
                    .map(assetExtractorTextUnit -> assetExtractorTextUnit.getSource())
                    .filter(source -> isSourceStringWithEmptyPlaceholders(placeholderRegularExpressions, source))).collect(Collectors.toSet());
    }

    private boolean isEmptyPlaceholderRegex(PlaceholderRegularExpressions regex) {
        return regex.equals(PlaceholderRegularExpressions.SINGLE_BRACE_REGEX) || regex.equals(PlaceholderRegularExpressions.DOUBLE_BRACE_REGEX);
    }

    private boolean isSourceStringWithEmptyPlaceholders(PlaceholderRegularExpressions placeholderRegularExpressions, String source) {
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
        notificationText.append("Found empty placeholders in the following source strings:");
        notificationText.append(System.lineSeparator());
        failures.stream().forEach(source -> {
            notificationText.append("\t* '" + source + "'");
            notificationText.append(System.lineSeparator());
        });
        notificationText.append("Please remove or update placeholders to contain a descriptive name.");
        notificationText.append(System.lineSeparator());
        return notificationText;
    }
}
