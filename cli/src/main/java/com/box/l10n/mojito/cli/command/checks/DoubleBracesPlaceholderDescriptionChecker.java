package com.box.l10n.mojito.cli.command.checks;

import java.util.Set;

public class DoubleBracesPlaceholderDescriptionChecker
    extends SingleBracesPlaceholderDescriptionChecker {

  private static final String LEFT_DOUBLE_BRACES_REGEX = "\\{\\{.*?";
  private static final String RIGHT_DOUBLE_BRACES_REGEX = "\\}\\}.*?";

  @Override
  public Set<String> checkCommentForDescriptions(String source, String comment) {
    return super.checkCommentForDescriptions(replaceDoubleBracesWithSingle(source), comment);
  }

  private String replaceDoubleBracesWithSingle(String str) {
    return str.replaceAll(LEFT_DOUBLE_BRACES_REGEX, "{").replaceAll(RIGHT_DOUBLE_BRACES_REGEX, "}");
  }
}
