package com.box.l10n.mojito.utils;

public class AssetContentUtils {
  public static String determineLineSeparator(String content) {
    if (content.contains("\r\n")) {
      return "\r\n";
    }
    if (content.contains("\n")) {
      return "\n";
    }
    if (content.contains("\r")) {
      return "\r";
    }

    return System.lineSeparator();
  }
}
