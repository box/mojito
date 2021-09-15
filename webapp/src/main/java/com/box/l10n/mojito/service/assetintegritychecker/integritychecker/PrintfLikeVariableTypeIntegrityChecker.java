package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

/**
 * Validates that a variable format type is not modified in the target string,
 * only validates variables contained within brackets e.g. %([variable_name])[flags][width][.precision][t]conversion)
 *
 * @author mallen
 */
public class PrintfLikeVariableTypeIntegrityChecker extends RegexIntegrityChecker {

    @Override
    public String getRegex() {
        /**
         * Regex checks for strings in the format %([variable_name])[conversion flags][type]
         */
        return "%([{|(])+([a-zA-Z0-9_])+([}|)])+([-#+ 0,\\.(\\<]+)?(\\d+)?(\\.\\d+)?([tT])?(hh|h|l|ll|j|z|t|L)?(%|c|d|e|E|f|F|g|G|i|o|p|r|s|u|x|X)";
    }

    @Override
    public void check(String content, String target) {
        try {
            super.check(content, target);
        } catch (RegexCheckerException ex) {
            throw new PrintfLikeVariableTypeIntegrityCheckerException("Variable types do not match.");
        }
    }
}
