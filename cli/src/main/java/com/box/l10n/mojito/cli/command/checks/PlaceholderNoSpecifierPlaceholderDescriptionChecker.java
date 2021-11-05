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
 * Checks for placeholders in the form '%s', '%d' etc.
 */
public class PlaceholderNoSpecifierPlaceholderDescriptionChecker extends AbstractPlaceholderDescriptionCheck {

    private Pattern pattern = Pattern.compile(PlaceholderRegularExpressions.PLACEHOLDER_NO_SPECIFIER_REGEX.getRegex());

    @Override
    public Set<String> checkCommentForDescriptions(String source, String comment) {
        Matcher placeHolderMatcher = pattern.matcher(source);
        return getPlaceholderNames(source, placeHolderMatcher).stream()
                .filter(placeholder -> !Pattern.compile(placeholder + ":.+").matcher(comment).find())
                .map(placeholder -> getFailureText(placeholder))
                .collect(Collectors.toSet());
    }

    private List<String> getPlaceholderNames(String source, Matcher placeHolderMatcher) {
        List<String> placeholderNames = new ArrayList<>();
        int placeholderCount = 0;
        Set<String> previousPlaceholders = new HashSet<>();
        while (placeHolderMatcher.find()) {
            String placeholder = source.substring(placeHolderMatcher.start(), placeHolderMatcher.end());
            if(!previousPlaceholders.contains(placeholder)){
                placeholderNames.add(Integer.toString(placeholderCount));
                previousPlaceholders.add(placeholder);
                placeholderCount++;
            }
        }
        return placeholderNames;
    }
}
