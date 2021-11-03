package com.box.l10n.mojito.cli.command.checks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checker that verifies that a description of a placeholder is present in the associated
 * comment in the form <placeholder name>:<description> or <placeholder position>:<description>
 *
 * @author mallen
 */
public class PlaceholderCommentChecker extends AbstractCliChecker {

    static Logger logger = LoggerFactory.getLogger(PlaceholderCommentChecker.class);

    private Pattern wordPattern = Pattern.compile("\\w+");

    @Override
    public CliCheckResult call() {
        CliCheckResult cliCheckResult = new CliCheckResult(isHardFail(), CliCheckerType.PLACEHOLDER_COMMENT_CHECKER.name());
        Map<String, List<String>> failureMap = checkForPlaceholderDescriptionsInComment();
        if(!failureMap.isEmpty()) {
            cliCheckResult.setSuccessful(false);
            cliCheckResult.setNotificationText(buildNotificationText(failureMap).toString());
        }
        return cliCheckResult;
    }

    private Map<String, List<String>> checkForPlaceholderDescriptionsInComment() {
        List<Pattern> patterns = getRegexPatterns();
        Map<String, List<String>> failureMap = new HashMap<>();
        getAddedTextUnits().stream().forEach(assetExtractorTextUnit -> {
            String source = assetExtractorTextUnit.getSource();
            String comment = assetExtractorTextUnit.getComments();
            List<String> failures = new ArrayList<>();
            patterns.stream().forEach(pattern -> {
                Matcher matcher = pattern.matcher(source);
                int placeholderCount = 0;
                while (matcher.find()) {
                    String placeholder = source.substring(matcher.start(), matcher.end());
                    logger.debug("Found placeholder '{}' in source string '{}'", placeholder, source);
                    placeholderCount++;
                    Matcher placeholderNameMatcher = wordPattern.matcher(placeholder);
                    if (placeholderNameMatcher.find()) {
                        String placeholderName = placeholder.substring(placeholderNameMatcher.start(), placeholderNameMatcher.end());
                        logger.debug("Checking if placeholder name '{}' is present in comment with description.", placeholderName);
                        Matcher commentMatcher = Pattern.compile(placeholderName + ":.+").matcher(comment);
                        if (!commentMatcher.find()) {
                            failures.add("Missing description for placeholder with name '" + placeholderName + "' in comment.");
                        }
                    } else {
                        // no name found, look for placeholder positions in comment e.g. 1:description,2:description
                        Matcher commentMatcher = Pattern.compile(placeholderCount + ":.+").matcher(comment);
                        if(!commentMatcher.find()) {
                            failures.add("Missing description for placeholder with position " + placeholderCount + " in comment.");
                        }
                    }
                }
            });
            if(!failures.isEmpty()) {
                failureMap.put(source, failures);
            }
        });
        return failureMap;
    }

    private StringBuilder buildNotificationText(Map<String, List<String>> failureMap) {
        StringBuilder notificationText = new StringBuilder();
        notificationText.append("Placeholder description in comment check failed.");
        notificationText.append(System.lineSeparator());
        notificationText.append(System.lineSeparator());
        failureMap.keySet().stream().forEach(source -> {
            notificationText.append("String '" + source + "' failed check: ");
            notificationText.append(System.lineSeparator());
            failureMap.get(source).stream().forEach(failure -> {
                notificationText.append("\t* " + failure);
                notificationText.append(System.lineSeparator());
            });
        });

        return notificationText;
    }
}
