package com.box.l10n.mojito.cli.command.checks;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

public abstract class AbstractPlaceholderDescriptionCheck {

    public abstract List<String> checkCommentForDescriptions(String source, String comment);

    public String getFailureText(String placeholder) {
        String failureText;
        if(StringUtils.isNumeric(placeholder)) {
            failureText = "Missing description for placeholder number '" + placeholder + "' in comment.";
        }else {
            failureText ="Missing description for placeholder with name '" + placeholder + "' in comment.";
        }
        return failureText;
    }
}
