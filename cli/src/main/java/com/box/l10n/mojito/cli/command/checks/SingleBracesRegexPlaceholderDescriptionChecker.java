package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.regex.PlaceholderRegularExpressions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

public class SingleBracesRegexPlaceholderDescriptionChecker extends SimpleRegexPlaceholderDescriptionChecker {

    public SingleBracesRegexPlaceholderDescriptionChecker() {
        super(PlaceholderRegularExpressions.SINGLE_BRACE_REGEX);
    }

    @Override
    protected List<String> getPlaceholderNames(String source, Matcher placeHolderMatcher) {
        List<String> placeholderNames = new ArrayList<>();
        Set<String> previousPlaceholders = new HashSet<>();
        while (placeHolderMatcher.find()) {
            String placeholder = source.substring(placeHolderMatcher.start(), placeHolderMatcher.end())
                    .replaceAll("\\{", "")
                    .replaceAll("\\}", "");
            if(!previousPlaceholders.contains(placeholder)){
                placeholderNames.add(placeholder);
                previousPlaceholders.add(placeholder);
            }
        }
        return placeholderNames;
    }
}
