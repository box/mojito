package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;

import java.util.HashMap;
import java.util.Map;

public class ContextAndCommentCliChecker extends AbstractCliChecker{

    @Override
    public CliCheckResult call() {
        CliCheckResult cliCheckResult = new CliCheckResult(isHardFail(), CliCheckerType.CONTEXT_COMMENT_CHECKER.name());
        Map<String, String> failureMap = new HashMap<>();
        getAddedTextUnits().stream().forEach(assetExtractorTextUnit -> {
            String failureText = checkTextUnit(assetExtractorTextUnit);
            if(failureText != null) {
                failureMap.put(assetExtractorTextUnit.getSource(), failureText);
            }
        });

        if(!failureMap.isEmpty()) {
            StringBuilder notificationText = buildNotificationText(cliCheckResult, failureMap);
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

        if (!isNullOrEmpty(context) && !isNullOrEmpty(comment)) {
            if(context.trim().equalsIgnoreCase(comment.trim())) {
                failureText = "Context & comment strings should not be identical.";
            }
        }

        if(isNullOrEmpty(context) && isNullOrEmpty(comment)) {
            failureText = "Context and comment strings are both empty.";
        } else if (isNullOrEmpty(context)) {
            failureText = "Context string is empty.";
        } else if (isNullOrEmpty(comment)) {
            failureText = "Comment string is empty.";
        }

        return failureText;
    }

    private StringBuilder buildNotificationText(CliCheckResult cliCheckResult, Map<String, String> failureMap) {
        cliCheckResult.setSuccessful(false);
        StringBuilder notificationText = new StringBuilder();
        notificationText.append("Context and comment check found failures:");
        notificationText.append(System.lineSeparator());
        failureMap.keySet().stream().forEach(key -> {
            notificationText.append("\t* Source string '" + key + "' failed check with error: " + failureMap.get(key));
            notificationText.append(System.lineSeparator());
        });

        return notificationText;
    }

    private boolean isNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }

}
