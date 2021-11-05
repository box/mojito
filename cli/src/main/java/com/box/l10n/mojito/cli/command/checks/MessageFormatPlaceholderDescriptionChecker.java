package com.box.l10n.mojito.cli.command.checks;

import com.ibm.icu.text.MessageFormat;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MessageFormatPlaceholderDescriptionChecker extends AbstractPlaceholderDescriptionCheck {

    @Override
    public Set<String> checkCommentForDescriptions(String source, String comment) {
        MessageFormat messageFormat = new MessageFormat(source);
        return messageFormat.getArgumentNames().stream()
                .filter(placeholder -> !Pattern.compile(placeholder + ":.+").matcher(comment).find())
                .map(placeholder -> getFailureText(placeholder))
                .collect(Collectors.toSet());
    }

}
