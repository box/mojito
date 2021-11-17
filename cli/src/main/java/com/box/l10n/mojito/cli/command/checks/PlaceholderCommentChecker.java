package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.box.l10n.mojito.regex.PlaceholderRegularExpressions;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Checker that verifies that a description of a placeholder is present in the associated
 * comment in the form <placeholder name>:<description> or <placeholder position>:<description>
 *
 * @author mallen
 */
public class PlaceholderCommentChecker extends AbstractCliChecker {

    static Logger logger = LoggerFactory.getLogger(PlaceholderCommentChecker.class);

    class PlaceholderCommentCheckResult {
        final List<String> failures;
        final String source;

        PlaceholderCommentCheckResult(String source, List<String> failures) {
            this.source = source;
            this.failures = failures;
        }

        public List<String> getFailures() {
            return failures;
        }

        public String getSource() {
            return source;
        }
    }


    @Override
    public CliCheckResult run() {
        CliCheckResult cliCheckResult = new CliCheckResult(isHardFail(), CliCheckerType.PLACEHOLDER_COMMENT_CHECKER.name());
        Map<String, List<String>> failureMap = checkForPlaceholderDescriptionsInComment();
        if(!failureMap.isEmpty()) {
            cliCheckResult.setSuccessful(false);
            cliCheckResult.setNotificationText(buildNotificationText(failureMap).toString());
        }
        return cliCheckResult;
    }

    protected Map<String, List<String>> checkForPlaceholderDescriptionsInComment() {
        List<AbstractPlaceholderDescriptionCheck> placeholderDescriptionChecks = getPlaceholderCommentChecks();

        return getAddedTextUnits().stream()
                .map(assetExtractorTextUnit -> getPlaceholderCommentCheckResult(placeholderDescriptionChecks, assetExtractorTextUnit))
                .filter(result -> !result.getFailures().isEmpty())
                .collect(Collectors.toMap(PlaceholderCommentCheckResult::getSource, PlaceholderCommentCheckResult::getFailures));
    }

    private PlaceholderCommentCheckResult getPlaceholderCommentCheckResult(List<AbstractPlaceholderDescriptionCheck> placeholderDescriptionChecks, AssetExtractorTextUnit assetExtractorTextUnit) {
        PlaceholderCommentCheckResult result;
        String source = assetExtractorTextUnit.getSource();
        String comment = assetExtractorTextUnit.getComments();
        if(StringUtils.isBlank(comment)){
            result = new PlaceholderCommentCheckResult(source, Lists.newArrayList("Comment is empty."));
        }else {
            List<String> failures = placeholderDescriptionChecks.stream()
                    .flatMap(check -> check.checkCommentForDescriptions(source, comment).stream())
                    .collect(Collectors.toList());
            result = new PlaceholderCommentCheckResult(source, failures);
        }

        return result;
    }

    private List<AbstractPlaceholderDescriptionCheck> getPlaceholderCommentChecks() {
        return cliCheckerOptions.getParameterRegexSet().stream()
                .map(placeholderRegularExpressions -> getPlaceholderDescriptionCheck(placeholderRegularExpressions))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private Optional<AbstractPlaceholderDescriptionCheck> getPlaceholderDescriptionCheck(PlaceholderRegularExpressions placeholderRegularExpression) {
        AbstractPlaceholderDescriptionCheck placeholderDescriptionCheck = null;
        switch (placeholderRegularExpression){
            case SINGLE_BRACE_REGEX:
                placeholderDescriptionCheck = new SingleBracesPlaceholderDescriptionChecker();
                break;
            case DOUBLE_BRACE_REGEX:
                placeholderDescriptionCheck = new DoubleBracesPlaceholderDescriptionChecker();
                break;
            case PRINTF_LIKE_VARIABLE_TYPE_REGEX:
                placeholderDescriptionCheck = new PrintfLikeVariableTypePlaceholderDescriptionChecker();
                break;
            case PLACEHOLDER_NO_SPECIFIER_REGEX:
            case SIMPLE_PRINTF_REGEX:
            case PRINTF_LIKE_REGEX:
            case PLACEHOLDER_IGNORE_PERCENTAGE_AFTER_BRACKETS:
                placeholderDescriptionCheck = new SimpleRegexPlaceholderDescriptionChecker(placeholderRegularExpression);
                break;
            default:
                logger.warn("Placeholder comment checker not implemented for regex {}", placeholderRegularExpression.name());
        }
        return Optional.ofNullable(placeholderDescriptionCheck);
    }

    private StringBuilder buildNotificationText(Map<String, List<String>> failureMap) {
        StringBuilder notificationText = new StringBuilder();
        notificationText.append("Placeholder description in comment check failed.");
        notificationText.append(System.lineSeparator());
        notificationText.append(System.lineSeparator());
        failureMap.keySet().stream().forEach(source -> {
            notificationText.append("String '" + source + "' failed check:");
            notificationText.append(System.lineSeparator());
            failureMap.get(source).stream().forEach(failure -> {
                notificationText.append("* " + failure);
                notificationText.append(System.lineSeparator());
            });
        });

        return notificationText;
    }
}
