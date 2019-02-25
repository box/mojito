package com.box.l10n.mojito.okapi.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author emagalindan
 */
@Component
public class UnescapeFilter {

    /**
     * Logger
     */
    static Logger logger = LoggerFactory.getLogger(UnescapeFilter.class);

    /**
     * Unescapes line feed, cariage return, single quote and double quote
     *
     * @param text
     * @return
     */
    public String unescape(String text) {
        String unescapedText = replaceEscapedCarriageReturn(text);
        unescapedText = replaceEscapedLineFeed(unescapedText);
        unescapedText = replaceEscapedQuotes(unescapedText);
        return unescapedText;
    }

    String replaceEscapedCarriageReturn(String text) {
        return text.replaceAll("\\\\r", "\r");
    }

    String replaceEscapedLineFeed(String text) {
        return text.replaceAll("\\\\n", "\n");
    }

    /**
     * Replaces \' and \" with ' or "
     *
     * @param text
     * @return
     */
    String replaceEscapedQuotes(String text) {
        return text.replaceAll("\\\\(\"|')", "$1");
    }

    /**
     * Replace other escape character with the character itself.
     *
     * Must be call after replacing espace sequence that need a different treatment like {@link #replaceEscapedLineFeed(String)}
     *
     * @param text
     * @return
     */
    String replaceEscapedCharacters(String text) {
        return text.replaceAll("\\\\(.)?", "$1");
    }

    /**
     * Collapse multiple spaces into a single space.
     *
     * @param text
     * @return
     */
    String collapseSpaces(String text) {
        return text.replaceAll("\\s+", " ");
    }

    String replaceLineFeedWithSpace(String text) {
        return text.replaceAll("\n", " ");
    }
}
