package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import com.google.common.base.Objects;
import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks that there are the same leading and trailing whitespaces in the source and target content.
 * 
 * @author jyi
 */
public class WhitespaceIntegrityChecker extends AbstractTextUnitIntegrityChecker {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(WhitespaceIntegrityChecker.class);

    private static final String STRING_WITH_LEADING_REGEX = "^(\\s+).+";
    private static final String STRING_WITH_TRAILING_REGEX = ".+?(\\s+)$";
    private static final String GROUP_WITH_WHITESPACES = "$1";

    @Override
    public void check(String sourceContent, String targetContent) throws WhitespaceIntegrityCheckerException {
        checkLeadingWhitespaces(sourceContent, targetContent);
        checkTrailingWhitespaces(sourceContent, targetContent);
    }
    
    public void checkLeadingWhitespaces(String sourceContent, String targetContent) throws WhitespaceIntegrityCheckerException {
        logger.debug("Get leading whitespaces around the source");
        String sourceWhiteSpaces = getLeadingWhitespaces(sourceContent);
        logger.debug("Source leading whitespaces: {}", sourceWhiteSpaces);

        logger.debug("Get leading whitespaces around the target");
        String targetWhiteSpaces = getLeadingWhitespaces(targetContent);
        logger.debug("Target leading whitespaces: {}", targetWhiteSpaces);

        logger.debug("Make sure the target has the same leading whitespaces as the source");
        if (!Objects.equal(sourceWhiteSpaces, targetWhiteSpaces)) {
            throw new WhitespaceIntegrityCheckerException("Leading whitespaces around source and target are different");
        }
    }
    
    public void checkTrailingWhitespaces(String sourceContent, String targetContent) throws WhitespaceIntegrityCheckerException {
        logger.debug("Get trailing whitespaces around the source");
        String sourceWhiteSpaces = getTrailingWhitespaces(sourceContent);
        logger.debug("Source trailing whitespaces: {}", sourceWhiteSpaces);

        logger.debug("Get trailing whitespaces around the target");
        String targetWhiteSpaces = getTrailingWhitespaces(targetContent);
        logger.debug("Target trailing whitespaces: {}", targetWhiteSpaces);

        logger.debug("Make sure the target has the same trailing whitespaces as the source");
        if (!Objects.equal(sourceWhiteSpaces, targetWhiteSpaces)) {
            throw new WhitespaceIntegrityCheckerException("Trailing shitespaces around source and target are different");
        }
    }

    /**
     * Gets the leading whitespace in a string.
     *
     * @param string
     * @return the leading whitespace or null if no trailing whitespace
     */
    String getLeadingWhitespaces(String string) {
        String leadingWhitepsaces = null;
        if (string.matches(STRING_WITH_LEADING_REGEX)) {
            leadingWhitepsaces = string.replaceAll(STRING_WITH_LEADING_REGEX, GROUP_WITH_WHITESPACES);
        }
        return leadingWhitepsaces;
    }
    
    /**
     * Gets the trailing whitespace in a string.
     *
     * @param string
     * @return the trailing whitespace or null if no trailing whitespace
     */
    String getTrailingWhitespaces(String string) {
        String trailingWhitespaces = null;
        if (string.matches(STRING_WITH_TRAILING_REGEX)) {
            trailingWhitespaces = string.replaceAll(STRING_WITH_TRAILING_REGEX, GROUP_WITH_WHITESPACES);
        }
        return trailingWhitespaces;
    }
    
}
