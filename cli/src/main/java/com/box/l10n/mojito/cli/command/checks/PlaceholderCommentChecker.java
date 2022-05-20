package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.box.l10n.mojito.regex.PlaceholderRegularExpressions;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.box.l10n.mojito.cli.command.extractioncheck.ExtractionCheckNotificationSender.QUOTE_MARKER;

/**
 * {@link AbstractCliChecker} that verifies that a description of a placeholder is present in the associated
 * comment in the form <placeholder name>:<description> or <placeholder position>:<description>
 *
 * @author mallen
 */
public class PlaceholderCommentChecker extends AbstractCliChecker {

    static Logger logger = LoggerFactory.getLogger(PlaceholderCommentChecker.class);

    @Override
    public CliCheckResult run(List<AssetExtractionDiff> assetExtractionDiffs) {
        CliCheckResult cliCheckResult = new CliCheckResult(isHardFail(), CliCheckerType.PLACEHOLDER_COMMENT_CHECKER.name());
        Map<String, List<String>> failureMap = checkForPlaceholderDescriptionsInComment(assetExtractionDiffs);
        if (!failureMap.isEmpty()) {
            cliCheckResult.setSuccessful(false);
            cliCheckResult.setNotificationText(buildNotificationText(failureMap).toString());
        }
        return cliCheckResult;
    }

    protected Map<String, List<String>> checkForPlaceholderDescriptionsInComment(List<AssetExtractionDiff> assetExtractionDiffs) {
        List<AbstractPlaceholderDescriptionCheck> placeholderDescriptionChecks = getPlaceholderCommentChecks();

        return getAddedTextUnits(assetExtractionDiffs).stream()
                .map(assetExtractorTextUnit -> getPlaceholderCommentCheckResult(placeholderDescriptionChecks, assetExtractorTextUnit))
                .filter(result -> !result.getFailures().isEmpty())
                .distinct()
                .collect(Collectors.toMap(PlaceholderCommentCheckResult::getSource, PlaceholderCommentCheckResult::getFailures));
    }

    private PlaceholderCommentCheckResult getPlaceholderCommentCheckResult(List<AbstractPlaceholderDescriptionCheck> placeholderDescriptionChecks, AssetExtractorTextUnit assetExtractorTextUnit) {
        PlaceholderCommentCheckResult result;
        String source = assetExtractorTextUnit.getSource();
        String comment = assetExtractorTextUnit.getComments();
        if (StringUtils.isBlank(comment)) {
            result = new PlaceholderCommentCheckResult(source, Lists.newArrayList("Comment is empty."));
        } else {
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
                .collect(Collectors.toList());
    }

    private AbstractPlaceholderDescriptionCheck getPlaceholderDescriptionCheck(PlaceholderRegularExpressions placeholderRegularExpression) {
        AbstractPlaceholderDescriptionCheck placeholderDescriptionCheck;
        switch (placeholderRegularExpression) {
            case SINGLE_BRACE_REGEX:
                placeholderDescriptionCheck = new SingleBracesPlaceholderDescriptionChecker();
                break;
            case DOUBLE_BRACE_REGEX:
                placeholderDescriptionCheck = new DoubleBracesPlaceholderDescriptionChecker();
                break;
            case PRINTF_LIKE_VARIABLE_TYPE_REGEX:
                placeholderDescriptionCheck = new PrintfLikeVariableTypePlaceholderDescriptionChecker();
                break;
            default:
                placeholderDescriptionCheck = new SimpleRegexPlaceholderDescriptionChecker(placeholderRegularExpression);
        }
        return placeholderDescriptionCheck;
    }

    private StringBuilder buildNotificationText(Map<String, List<String>> failureMap) {
        StringBuilder notificationText = new StringBuilder();
        notificationText.append("Placeholder description in comment check failed.");
        notificationText.append(System.lineSeparator());
        notificationText.append(System.lineSeparator());
        notificationText.append(failureMap.keySet().stream()
                .map(source -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("String " + QUOTE_MARKER + source + QUOTE_MARKER + " failed check:");
                    sb.append(System.lineSeparator());
                    return sb.append(failureMap.get(source).stream()
                                    .map(failure -> "* " + failure)
                                    .collect(Collectors.joining(System.lineSeparator())))
                            .toString();
                })
                .collect(Collectors.joining(System.lineSeparator())));

        return notificationText;
    }

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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PlaceholderCommentCheckResult result = (PlaceholderCommentCheckResult) o;
            return Objects.equals(failures, result.failures) && Objects.equals(source, result.source);
        }

        @Override
        public int hashCode() {
            return Objects.hash(failures, source);
        }
    }
}
