package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

/**
 * Checks that there are the same placeholders like %1, %2, etc
 * in the source and target content, order is not important.
 * 
 * @author jyi
 */
public class SimplePrintfLikeIntegrityChecker extends RegexIntegrityChecker {
    
    @Override
    public String getRegex() {
        return "%\\d+";
    }

    @Override
    public void check(String sourceContent, String targetContent) throws PrintfLikeIntegrityCheckerException {
        
        try {
            super.check(sourceContent, targetContent);
        } catch (RegexCheckerException rce) {
            throw new SimplePrintfLikeIntegrityCheckerException((rce.getMessage()));
        }
    }
}
