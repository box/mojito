package com.box.l10n.mojito.cli.command.checks;

import java.util.List;

public class DoubleBracesPlaceholderDescriptionChecker extends MessageFormatPlaceholderDescriptionChecker {

    private static final String LEFT_DOUBLE_BRACES_REGEX = "\\{\\{.*?";
    private static final String RIGHT_DOUBLE_BRACES_REGEX = "\\}\\}.*?";

    @Override
    public List<String> checkCommentForDescriptions(String source, String comment) {
        return super.checkCommentForDescriptions(replaceDoubleBracesWithSingle(source), comment);
    }

    private String replaceDoubleBracesWithSingle(String str) {
        return str.replaceAll(LEFT_DOUBLE_BRACES_REGEX, "{")
                .replaceAll(RIGHT_DOUBLE_BRACES_REGEX, "}");
    }
}
