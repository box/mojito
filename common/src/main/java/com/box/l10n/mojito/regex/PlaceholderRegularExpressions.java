package com.box.l10n.mojito.regex;

public class PlaceholderRegularExpressions {

    /**
     * Modified regex from Formatter#formatSpecifier = "%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])";
     * (%[argument_index$][flags][width][.precision][t]conversion)
     * @return
     */
    public static final String PRINTF_LIKE_REGEX = "%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?(hh|h|l|ll|j|z|t|L)?(%|d|i|u|o|x|X|f|F|e|E|g|G|a|A|c|s|p|n|@)";

    /**
     * Regex checks for strings in the format %([variable_name])[conversion flags][type]
     */
    public static final String PRINTF_LIKE_VARIABLE_TYPE_REGEX = "%([{|(])+([a-zA-Z0-9_])+([}|)])+([-#+ 0,\\.(\\<]+)?(\\d+)?(\\.\\d+)?([tT])?(hh|h|l|ll|j|z|t|L)?(%|c|d|e|E|f|F|g|G|i|o|p|r|s|u|x|X)";

    /**
     * Checks for placeholders like %1, %2
     */
    public static final String SIMPLE_PRINTF_REGEX =  "%\\d+";

    /**
     * Checks for placeholders within single braces e.g. {placeholder}
     */
    public static final String SINGLE_BRACE_REGEX = "\\{\\w*\\}";

    /**
     * Checks for placeholders within double braces e.g. {{placeholder}}
     */
    public static final String DOUBLE_BRACE_REGEX = "\\{\\{\\w*\\}\\}";

    /**
     * Checks for placeholders as % followed by a type e.g. %s, %d
     */
    public static final String PLACEHOLDER_NO_SPECIFIER_REGEX = "%[d|i|u|o|x|X|f|F|e|E|g|G|a|A|c|s|p|n]";

    /**
     * Ignores percentage symbols after a bracket as a token.
     */
    public static final String PLACEHOLDER_IGNORE_PERCENTAGE_AFTER_BRACKETS = "(?<![}|)])%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?(hh|h|l|ll|j|z|t|L)?(%|d|i|u|o|x|X|f|F|e|E|g|G|a|A|c|s|p|n|@)";

}
