package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

/**
 * Checks that there are the same c-printf like placeholders 
 * in the source and target content, order is not important.
 *
 * @author wyau
 */
public class PrintfLikeIntegrityChecker extends RegexIntegrityChecker {

    /**
     * Modified regex from Formatter#formatSpecifier = "%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])";
     * (%[argument_index$][flags][width][.precision][t]conversion) 
     * @return 
     */
    @Override
    public String getRegex() {
        return "%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?(hh|h|l|ll|j|z|t|L)?(%|d|i|u|o|x|X|f|F|e|E|g|G|a|A|c|s|p|n|@)";
    }

    @Override
    public void check(String sourceContent, String targetContent) throws PrintfLikeIntegrityCheckerException {
        
        try {
            super.check(sourceContent, targetContent);
        } catch (RegexCheckerException rce) {
            throw new PrintfLikeIntegrityCheckerException((rce.getMessage()));
        }
    }

}
