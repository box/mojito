package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link CliChecker} that verifies the comment and context parameters are provided and
 * are not identical.
 *
 * @author mallen
 */
public class ContextAndCommentCliChecker extends AbstractCliChecker {

    static Logger logger = LoggerFactory.getLogger(ContextAndCommentCliChecker.class);

    class ContextAndCommentCliCheckerResult {
        String sourceString;
        String failureMessage;
        boolean failed;

        public ContextAndCommentCliCheckerResult(boolean failed, String sourceString, String failureMessage) {
            this.sourceString = sourceString;
            this.failureMessage = failureMessage;
            this.failed = failed;
        }

        public ContextAndCommentCliCheckerResult(boolean failed) {
            this.failed = failed;
        }

        public String getSourceString() {
            return sourceString;
        }

        public String getFailureMessage() {
            return failureMessage;
        }

        public boolean isFailed() {
            return failed;
        }
    }

    @Override
    public CliCheckResult run() {
        CliCheckResult cliCheckResult = new CliCheckResult(isHardFail(), CliCheckerType.CONTEXT_COMMENT_CHECKER.name());
        StringBuilder notificationText = new StringBuilder();
        runChecks(cliCheckResult, notificationText);
        if(!cliCheckResult.isSuccessful()) {
            cliCheckResult.setNotificationText("Context and comment check found failures:" + System.lineSeparator() + notificationText);
        }
        return cliCheckResult;
    }

    private void runChecks(CliCheckResult cliCheckResult, StringBuilder notificationText) {
        getAddedTextUnits().stream()
            .map(assetExtractorTextUnit -> getContextAndCommentCliCheckerResult(assetExtractorTextUnit, checkTextUnit(assetExtractorTextUnit)))
            .filter(result -> result.isFailed())
            .forEach(result -> {
                cliCheckResult.setSuccessful(false);
                appendFailureToNotificationText(notificationText, result);
            });
    }

    private ContextAndCommentCliCheckerResult getContextAndCommentCliCheckerResult(AssetExtractorTextUnit assetExtractorTextUnit, String failureText) {
        ContextAndCommentCliCheckerResult result;
        if(failureText != null) {
            logger.debug("'{}' source string failed check with error: {}", assetExtractorTextUnit.getSource(), failureText);
            result = new ContextAndCommentCliCheckerResult(true, assetExtractorTextUnit.getSource(), failureText);
        } else {
            result = new ContextAndCommentCliCheckerResult(false);
        }
        return result;
    }

    private void appendFailureToNotificationText(StringBuilder notificationText, ContextAndCommentCliCheckerResult result) {
        notificationText.append("* Source string '" + result.getSourceString() + "' failed check with error: " + result.getFailureMessage());
        notificationText.append(System.lineSeparator());
    }

    private String checkTextUnit(AssetExtractorTextUnit assetExtractorTextUnit) {
        String failureText = null;
        String[] splitNameArray = assetExtractorTextUnit.getName().split("---");
        String context = null;
        if (splitNameArray.length > 1) {
            context = splitNameArray[1];
        }
        String comment = assetExtractorTextUnit.getComments();

        if (!isBlank(context) && !isBlank(comment)) {
            if(context.trim().equalsIgnoreCase(comment.trim())) {
                failureText = "Context & comment strings should not be identical.";
            }
        } else if (isBlank(context) && isBlank(comment)) {
            failureText = "Context and comment strings are both empty.";
        } else if (isBlank(context)) {
            failureText = "Context string is empty.";
        } else if (isBlank(comment)) {
            failureText = "Comment string is empty.";
        }

        return failureText;
    }

    private boolean isBlank(String string) {
        return StringUtils.isBlank(string);
    }

}
