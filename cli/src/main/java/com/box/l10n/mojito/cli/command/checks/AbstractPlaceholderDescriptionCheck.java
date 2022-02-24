package com.box.l10n.mojito.cli.command.checks;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static com.box.l10n.mojito.cli.command.extractioncheck.ExtractionCheckNotificationSender.QUOTE_MARKER;

public abstract class AbstractPlaceholderDescriptionCheck {

    public abstract Set<String> checkCommentForDescriptions(String source, String comment);

    public Optional<String> getFailureText(String placeholder) {
        String failureText = null;
        if (StringUtils.isNumeric(placeholder)) {
            failureText = "Missing description for placeholder number " + QUOTE_MARKER + placeholder + QUOTE_MARKER + " in comment. Please add a description in the string comment in the form " + placeholder + ":<description>";
        } else if (!placeholder.trim().isEmpty()) {
            failureText = "Missing description for placeholder with name " + QUOTE_MARKER + placeholder + QUOTE_MARKER + " in comment. Please add a description in the string comment in the form " + placeholder + ":<description>";
        }
        return Optional.ofNullable(failureText);
    }

    protected boolean isPlaceholderDescriptionMissingInComment(String comment, String placeholder) {
        return StringUtils.isBlank(comment) || !Pattern.compile(placeholder + ":.+").matcher(comment).find();
    }
}
