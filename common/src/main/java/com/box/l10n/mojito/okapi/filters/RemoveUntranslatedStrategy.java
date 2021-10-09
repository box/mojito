package com.box.l10n.mojito.okapi.filters;

/**
 * Speficy how removing untranslated text unit is performed
 */
public enum RemoveUntranslatedStrategy {
    /**
     * the text unit is replace by a NOOP event and is then drop from the final document
     */
    NOOP_EVENT,
    /**
     * Generate the output with a placeholder and remove the untranslated in post processing
     */
    PLACEHOLDER_AND_POST_PROCESSING;

    public static final String UNTRANSLATED_PLACEHOLDER = "@#$untranslated$#@";
}
