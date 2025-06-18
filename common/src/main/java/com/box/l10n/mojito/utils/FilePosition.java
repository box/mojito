package com.box.l10n.mojito.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record FilePosition(String path, Integer line, Integer col) {
  private static final Pattern PATTERN = Pattern.compile("^(.*?)(?::(\\d+))?(?::(\\d+))?$");

  public static FilePosition from(String usage) {
    String path = usage;
    Integer line = null;
    Integer col = null;

    Matcher matcher = PATTERN.matcher(usage);
    if (matcher.matches()) {
      path = matcher.group(1);
      String lineStr = matcher.group(2);
      String colStr = matcher.group(3);

      if (lineStr != null) {
        line = Integer.parseInt(lineStr);
      }
      if (colStr != null) {
        col = Integer.parseInt(colStr);
      }
    }

    return new FilePosition(path, line, col);
  }
}
