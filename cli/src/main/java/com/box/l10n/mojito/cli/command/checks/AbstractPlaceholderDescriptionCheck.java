package com.box.l10n.mojito.cli.command.checks;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.Set;

public abstract class AbstractPlaceholderDescriptionCheck {

    public abstract Set<String> checkCommentForDescriptions(String source, String comment);

    public Optional<String> getFailureText(String placeholder) {
        String failureText = null;
        if (StringUtils.isNumeric(placeholder)) {
            failureText = "Missing description for placeholder number '" + placeholder + "' in comment.";
        } else if (!placeholder.trim().isEmpty()) {
            failureText ="Missing description for placeholder with name '" + placeholder + "' in comment.";
        }
        return Optional.ofNullable(failureText);
    }
}
