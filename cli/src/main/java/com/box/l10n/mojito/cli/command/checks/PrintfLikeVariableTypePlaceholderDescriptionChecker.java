package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.regex.PlaceholderRegularExpressions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PrintfLikeVariableTypePlaceholderDescriptionChecker extends AbstractPlaceholderDescriptionCheck {

    private Pattern pattern = Pattern.compile(PlaceholderRegularExpressions.PRINTF_LIKE_VARIABLE_TYPE_REGEX.getRegex());

    /**
     * Pattern to extract the name from within brackets e.g %(count)d or %{count}d will extract 'count' as the
     * placeholder name.
     */
    private Pattern namePattern = Pattern.compile("(?<=\\()\\w+?(?=\\))|(?<=\\{)\\w+?(?=\\})");

    @Override
    public Set<String> checkCommentForDescriptions(String source, String comment) {
        Matcher placeHolderMatcher = pattern.matcher(source);
        return getPlaceholderNames(source, placeHolderMatcher).stream()
            .filter(placeholder -> !Pattern.compile(placeholder + ":.+").matcher(comment).find())
            .map(placeholder -> getFailureText(placeholder))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());
    }

    private List<String> getPlaceholderNames(String source, Matcher placeHolderMatcher) {
        List<String> placeholderNames = new ArrayList<>();
        int placeholderCount = 0;
        while (placeHolderMatcher.find()) {
            placeholderNames.add(getPlaceholderName(source.substring(placeHolderMatcher.start(), placeHolderMatcher.end()), placeholderCount));
            placeholderCount++;
        }
        return placeholderNames;
    }

    private String getPlaceholderName(String placeholderText, int placeholderCount) {
        String name;
        Matcher nameMatcher = namePattern.matcher(placeholderText);
        if(nameMatcher.find()){
            name = placeholderText.substring(nameMatcher.start(), nameMatcher.end());
        }else {
            name = Integer.toString(placeholderCount);
        }
        return name;
    }
}
