
package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import java.util.regex.Matcher;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks that there are the same trailing whitespaces in the source and target content.
 * 
 * @author jyi
 */
public class TrailingWhitespaceIntegrityChecker extends RegexIntegrityChecker {
    
    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(TrailingWhitespaceIntegrityChecker.class);
    
    @Override
    public String getRegex() {
        return "(\\s+$)";
    }

    @Override
    public void check(String sourceContent, String targetContent) throws TrailingWhitespaceIntegrityCheckerException {
        logger.debug("Get trailing whitespaces of the target");
        String sourceTrailingWhiteSpaces = getTrailingWhiteSpaces(sourceContent);
        logger.debug("Source trailing whitespaces: {}", sourceTrailingWhiteSpaces);

        logger.debug("Get trailing whitespaces of the source");
        String targetTrailingWhiteSpaces = getTrailingWhiteSpaces(targetContent);
        logger.debug("Target trailing whitespaces: {}", targetTrailingWhiteSpaces);

        logger.debug("Make sure the target has the same trailing whitespaces as the source");

        if (!StringUtils.equals(sourceTrailingWhiteSpaces, targetTrailingWhiteSpaces)) {
            throw new TrailingWhitespaceIntegrityCheckerException("Trailing whitespaces in source and target are different");
        }
    }

    /**
     * Gets the trailing whitespace in a string.
     *
     * @param string
     * @return the trailing whitespace or null if no trailing whitespace
     */
    String getTrailingWhiteSpaces(String string) {

        String trailingWhiteSpaces = null;

        if (string != null) {
            Matcher matcher = getPattern().matcher(string);

            if (matcher.find()) {
                trailingWhiteSpaces = matcher.group(1);
            }
        }

        return trailingWhiteSpaces;
    }
    
}
