package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks that there are the same placeholders in the source and target content,
 * order is not important. Placeholders are extracted using a regular
 * expression.
 *
 * @author wyau
 */
public abstract class RegexIntegrityChecker implements TextUnitIntegrityChecker {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(RegexIntegrityChecker.class);

    /**
     * This pattern is used to match placeholders
     */
    Pattern pattern;

    /**
     * Gets Regex used to extract placholders.
     *
     * @return Regex used to extract placholders.
     */
    public abstract String getRegex();

    @Override
    public void check(String sourceContent, String targetContent) throws RegexCheckerException {

        logger.debug("Get placeholder of the target");
        Set<String> sourcePlaceholders = getPlaceholders(sourceContent);
        logger.debug("Source Placeholder: {}", sourcePlaceholders);

        logger.debug("Get placeholder of the source");
        Set<String> targetPlaceholders = getPlaceholders(targetContent);
        logger.debug("Target Placeholder: {}", targetPlaceholders);

        logger.debug("Make sure the target has the same placeholder as the source");

        if (!sourcePlaceholders.equals(targetPlaceholders)) {
            throw new RegexCheckerException("Placeholders in source and target are different");
        }
    }

    /**
     * Gets the pattern used to extract the placeholders.
     *
     * @return the pattern used to extract the placeholders.
     */
    Pattern getPattern() {

        if (pattern == null) {
            pattern = Pattern.compile(getRegex());
        }

        return pattern;
    }

    /**
     * Gets the placeholders contained in a string.
     *
     * @param string that contains the placeholders
     * @return the placeholders
     */
    Set<String> getPlaceholders(String string) {

        Set<String> placeholders = new HashSet<>();

        if (string != null) {

            Matcher matcher = getPattern().matcher(string);

            while (matcher.find()) {
                placeholders.add(matcher.group());
            }
        }

        return placeholders;
    }
}
