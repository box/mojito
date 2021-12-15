package com.box.l10n.mojito.cli.command.checks;

import com.ibm.icu.text.MessageFormat;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SingleBracesPlaceholderDescriptionChecker extends AbstractPlaceholderDescriptionCheck {

    static Logger logger = LoggerFactory.getLogger(SingleBracesPlaceholderDescriptionChecker.class);

    @Override
    public Set<String> checkCommentForDescriptions(String source, String comment) {

        try {
            MessageFormat messageFormat = new MessageFormat(source);
            return messageFormat.getArgumentNames().stream()
                    .filter(placeholder -> isPlaceholderDescriptionMissingInComment(comment, placeholder))
                    .map(placeholder -> getFailureText(placeholder))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());
        } catch (IllegalArgumentException e) {
            logger.debug("String '{}' is not in ICU format, attempting to parse with regex", source);
            SimpleRegexPlaceholderDescriptionChecker regexChecker = new SingleBracesRegexPlaceholderDescriptionChecker();
            return regexChecker.checkCommentForDescriptions(source, comment);
        }

    }



}
