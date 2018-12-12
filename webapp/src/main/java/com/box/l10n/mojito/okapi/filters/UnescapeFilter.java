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

    public String unescape(String text) {
        String unescapedText = text.replaceAll("(\\\\)(\"|')", "$2");
        unescapedText = unescapedText.replaceAll("\\\\n", "\n");
        unescapedText = unescapedText.replaceAll("\\\\r", "\r");
        return unescapedText;
    }
}
