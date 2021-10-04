package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Integrity checker that modifies any placeholders with format '%[type]' in the source & target strings to use
 * a placeholder with parameter specifier '%1$[type]' before executing the {@link PrintfLikeIntegrityChecker} checks.
 *
 * @author mallen
 *
 */
public class PrintfLikeAddParameterSpecifierIntegrityChecker extends PrintfLikeIntegrityChecker {

    private static final String PLACEHOLDER_NO_SPECIFIER_REGEX = "%[d|i|u|o|x|X|f|F|e|E|g|G|a|A|c|s|p|n]";
    private Pattern pattern = Pattern.compile(PLACEHOLDER_NO_SPECIFIER_REGEX);

    @Override
    public void check(String sourceContent, String targetContent) throws PrintfLikeIntegrityCheckerException {
        super.check(addParameterSpecifierToPlaceholders(sourceContent), addParameterSpecifierToPlaceholders(targetContent));
    }

    /**
     * Replace all instances of placeholders with format '%[type]' with '%1$[type]'
     *
     * @param str
     * @return
     */
    private String addParameterSpecifierToPlaceholders(String str) {
        Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            String match = matcher.group();
            // Pattern will match percentage symbol & type e.g. '%s', replace '%' with '%1$' in match so placeholder type is maintained.
            String placeHolderWithSpecifier = match.replace("%", "%1\\$");
            str = str.replaceAll(match, placeHolderWithSpecifier);
        }
        return str;
    }
}
