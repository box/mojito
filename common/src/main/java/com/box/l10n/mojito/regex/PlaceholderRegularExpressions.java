package com.box.l10n.mojito.regex;

public class PlaceholderRegularExpressions {

    public static String PRINTF_LIKE_REGEX = "%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?(hh|h|l|ll|j|z|t|L)?(%|d|i|u|o|x|X|f|F|e|E|g|G|a|A|c|s|p|n|@)";

    public static String PRINTF_LIKE_VARIABLE_TYPE_REGEX = "%([{|(])+([a-zA-Z0-9_])+([}|)])+([-#+ 0,\\.(\\<]+)?(\\d+)?(\\.\\d+)?([tT])?(hh|h|l|ll|j|z|t|L)?(%|c|d|e|E|f|F|g|G|i|o|p|r|s|u|x|X)";

    public static String SIMPLE_PRINTF_REGEX =  "%\\d+";

    public static String SINGLE_BRACE_REGEX = "{\\w*}";

    public static String DOUBLE_BRACE_REGEX = "{{\\w*}}";

    public static String PLACEHOLDER_NO_SPECIFIER_REGEX = "%[d|i|u|o|x|X|f|F|e|E|g|G|a|A|c|s|p|n]";

    public static String PLACEHOLDER_IGNORE_PERCENTAGE_AFTER_BRACKETS = "(?<![}|)])%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?(hh|h|l|ll|j|z|t|L)?(%|d|i|u|o|x|X|f|F|e|E|g|G|a|A|c|s|p|n|@)";

}
