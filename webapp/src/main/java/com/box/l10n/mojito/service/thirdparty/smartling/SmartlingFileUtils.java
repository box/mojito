package com.box.l10n.mojito.service.thirdparty.smartling;

import java.util.regex.Pattern;

public final class SmartlingFileUtils {

  private SmartlingFileUtils() {
    throw new AssertionError("Do not instantiate");
  }

  public static String getOutputSourceFile(long batchNumber, String repositoryName, String prefix) {
    return String.format("%s/%05d_%s_source.xml", repositoryName, batchNumber, prefix);
  }

  public static String getOutputTargetFile(
      long batchNumber, String repositoryName, String prefix, String locale) {
    return String.format("%s/%05d_%s_%s.xml", repositoryName, batchNumber, prefix, locale);
  }

  public static Pattern getFilePattern(String repositoryName) {
    return Pattern.compile(repositoryName + "/(\\d+)_(singular|plural)_source.xml");
  }

  public static Boolean isPluralFile(String fileUri) {
    return fileUri.endsWith("plural_source.xml");
  }
}
