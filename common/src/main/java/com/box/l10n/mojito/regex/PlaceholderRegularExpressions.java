package com.box.l10n.mojito.regex;

public enum PlaceholderRegularExpressions {

    /**
     * Modified regex from Formatter#formatSpecifier = "%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])";
     * (%[argument_index$][flags][width][.precision][t]conversion)
     * @return
     */
    PRINTF_LIKE_REGEX("%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?(hh|h|l|ll|j|z|t|L)?(%|d|i|u|o|x|X|f|F|e|E|g|G|a|A|c|s|p|n|@)"),

    /**
     * Regex checks for strings in the format %([variable_name])[conversion flags][type]
     */
    PRINTF_LIKE_VARIABLE_TYPE_REGEX("%([{|(])+([a-zA-Z0-9_])+([}|)])+([-#+ 0,\\.(\\<]+)?(\\d+)?(\\.\\d+)?([tT])?(hh|h|l|ll|j|z|t|L)?(%|c|d|e|E|f|F|g|G|i|o|p|r|s|u|x|X)"),

    /**
     * Checks for placeholders like %1, %2
     */
    SIMPLE_PRINTF_REGEX("%\\d+"),

    /**
     * Checks for placeholders within single braces e.g. {placeholder}
     */
    SINGLE_BRACE_REGEX("\\{(.|\\n)*?(?=\\})\\}"),

    /**
     * Checks for placeholders within double braces e.g. {{placeholder}}
     */
    DOUBLE_BRACE_REGEX("\\{\\{(.|\\n)*?(?=\\}\\})\\}\\}"),

    /**
     * Checks for placeholders as % followed by a type e.g. %s, %d
     */
    PLACEHOLDER_NO_SPECIFIER_REGEX("%[d|i|u|o|x|X|f|F|e|E|g|G|a|A|c|s|p|n]"),

    /**
     * Ignores percentage symbols after a bracket as a token.
     */
    PLACEHOLDER_IGNORE_PERCENTAGE_AFTER_BRACKETS("(?<![}|)])%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?(hh|h|l|ll|j|z|t|L)?(%|d|i|u|o|x|X|f|F|e|E|g|G|a|A|c|s|p|n|@)");

    private String regex;

    PlaceholderRegularExpressions(String regex) {
        this.regex = regex;
    }

    public String getRegex() {
        return regex;
    }

}
