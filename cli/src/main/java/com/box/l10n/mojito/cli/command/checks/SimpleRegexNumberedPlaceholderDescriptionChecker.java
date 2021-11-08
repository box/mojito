package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.regex.PlaceholderRegularExpressions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Checks for placeholders by number using a regex pattern, expects to find numbered descriptions in the comment
 * that matches the number of placeholders in the source string.
 *
 * @author mallen
 */
public class SimpleRegexNumberedPlaceholderDescriptionChecker extends AbstractPlaceholderDescriptionCheck {

    private Pattern pattern;

    public SimpleRegexNumberedPlaceholderDescriptionChecker(PlaceholderRegularExpressions placeholderRegularExpressions) {
        this.pattern = Pattern.compile(placeholderRegularExpressions.getRegex());
    }

    @Override
    public Set<String> checkCommentForDescriptions(String source, String comment) {
        Matcher placeHolderMatcher = pattern.matcher(source);
        return getPlaceholderNames(source, placeHolderMatcher).stream()
                .filter(placeholder -> !Pattern.compile(placeholder + ":.+").matcher(comment).find())
                .map(placeholder -> getFailureText(placeholder))
                .collect(Collectors.toSet());
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    private List<String> getPlaceholderNames(String source, Matcher placeHolderMatcher) {
        List<String> placeholderNames = new ArrayList<>();
        Set<String> previousPlaceholders = new HashSet<>();
        while (placeHolderMatcher.find()) {
            String placeholder = source.substring(placeHolderMatcher.start(), placeHolderMatcher.end());
            if(!previousPlaceholders.contains(placeholder)){
                placeholderNames.add(placeholder);
                previousPlaceholders.add(placeholder);
            }
        }
        return placeholderNames;
    }
}
