package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.box.l10n.mojito.regex.PlaceholderRegularExpressions.PLACEHOLDER_IGNORE_PERCENTAGE_AFTER_BRACKETS;

/**
 * '%' symbol preceded by a '}' or ')' are ignored as variable tokens. This can be useful
 * for avoiding false negatives when handling translations that contain percentages inside
 * the translation text.
 *
 * @author mallen
 */
public class PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker extends RegexIntegrityChecker {

    @Override
    public String getRegex() {
        return PLACEHOLDER_IGNORE_PERCENTAGE_AFTER_BRACKETS;
    }

    @Override
    public void check(String content, String target) {
        try {
            super.check(content, target);
            checkForStandalonePercentageSymbols(content, target);
        } catch (RegexCheckerException ex) {
            throw new PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException(ex.getMessage());
        }
    }

    private void checkForStandalonePercentageSymbols(String content, String target) {
        String standalonePercentageRegex = "((?<!%)%)((?!%))((?!\\S))";
        executeRegexOnPlaceholders(getPlaceholders(content), standalonePercentageRegex);
        executeRegexOnPlaceholders(getPlaceholders(target), standalonePercentageRegex);
    }

    private void executeRegexOnPlaceholders(Set<String> placeholders, String regex) {
        Pattern pattern = Pattern.compile(regex);

        for(String s : placeholders) {
            if (s != null) {

                Matcher matcher = pattern.matcher(s);

                while (matcher.find()) {
                    throw new RegexCheckerException("Standalone % found, percentages should be doubled (%%) for formatting.");
                }
            }
        }
    }

}
