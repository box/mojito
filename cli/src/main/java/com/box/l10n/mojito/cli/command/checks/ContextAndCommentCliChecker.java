package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Checker that verifies the comment and context parameters are provided and
 * are not identical.
 *
 * @author mallen
 */
public class ContextAndCommentCliChecker extends AbstractCliChecker {

    static Logger logger = LoggerFactory.getLogger(ContextAndCommentCliChecker.class);

    @Override
    public CliCheckResult call() {
        CliCheckResult cliCheckResult = new CliCheckResult(isHardFail(), CliCheckerType.CONTEXT_COMMENT_CHECKER.name());
        Map<String, String> failureMap = new HashMap<>();
        getAddedTextUnits().stream().forEach(assetExtractorTextUnit -> {
            String failureText = checkTextUnit(assetExtractorTextUnit);
            if(failureText != null) {
                logger.debug("'{}' source string failed check with error: {}", assetExtractorTextUnit.getSource(), failureText);
                failureMap.put(assetExtractorTextUnit.getSource(), failureText);
            }
        });

        if(!failureMap.isEmpty()) {
            cliCheckResult.setSuccessful(false);
            StringBuilder notificationText = buildNotificationText(failureMap);
            cliCheckResult.setNotificationText(notificationText.toString());
        }
        return cliCheckResult;
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

    private StringBuilder buildNotificationText(Map<String, String> failureMap) {
        StringBuilder notificationText = new StringBuilder();
        notificationText.append("Context and comment check found failures:");
        notificationText.append(System.lineSeparator());
        failureMap.keySet().stream().forEach(key -> {
            notificationText.append("\t* Source string '" + key + "' failed check with error: " + failureMap.get(key));
            notificationText.append(System.lineSeparator());
        });

        return notificationText;
    }

    private boolean isBlank(String string) {
        return StringUtils.isBlank(string);
    }

}
