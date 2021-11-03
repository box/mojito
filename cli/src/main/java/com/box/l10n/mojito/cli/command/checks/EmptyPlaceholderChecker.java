package com.box.l10n.mojito.cli.command.checks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public CliCheckResult call() {
        CliCheckResult cliCheckResult = new CliCheckResult(isHardFail(), CliCheckerType.EMPTY_PLACEHOLDER_CHECKER.name());
        List<String> failures = checkForEmptyPlaceholders();
        if (!failures.isEmpty()) {
            cliCheckResult.setSuccessful(false);
            cliCheckResult.setNotificationText(buildNotificationText(failures).toString());
        }

        return cliCheckResult;
    }

    private List<String> checkForEmptyPlaceholders() {
        List<String> failures = new ArrayList<>();
        List<Pattern> patterns = getRegexPatterns();
        getAddedTextUnits().stream().forEach(assetExtractorTextUnit -> {
            String source = assetExtractorTextUnit.getSource();
            patterns.stream().forEach(pattern -> {
                Matcher matcher = pattern.matcher(source);
                while (matcher.find()) {
                    String placeholder = source.substring(matcher.start(), matcher.end());
                    logger.debug("Found placeholder '{}' in source string '{}'", placeholder, source);
                    if(!wordPattern.matcher(placeholder).find()) {
                        logger.debug("Found empty placeholder '{}' in source string '{}'", placeholder, source);
                        failures.add(source);
                        break;
                    }
                }
            });
        });
        return failures;
    }

    private StringBuilder buildNotificationText(List<String> failures) {
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
