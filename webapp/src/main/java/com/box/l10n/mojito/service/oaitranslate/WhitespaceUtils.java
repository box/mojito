package com.box.l10n.mojito.service.oaitranslate;

public class WhitespaceUtils {

  public static String restoreLeadingAndTrailingWhitespace(String source, String target) {
    int leading = 0;
    int trailing = 0;
    while (leading < source.length() && Character.isWhitespace(source.charAt(leading))) leading++;
    while (trailing < source.length() - leading
        && Character.isWhitespace(source.charAt(source.length() - 1 - trailing))) trailing++;
    return source.substring(0, leading) + target + source.substring(source.length() - trailing);
  }
}
