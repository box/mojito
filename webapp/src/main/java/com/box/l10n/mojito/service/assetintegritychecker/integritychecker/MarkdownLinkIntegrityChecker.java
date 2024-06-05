package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;

public class MarkdownLinkIntegrityChecker extends RegexIntegrityChecker {

  @Override
  public String getRegex() {
    return "\\[(?<text>.+?)]\\((?<url>.+?)\\)";
  }

  @Override
  Set<String> getPlaceholders(String string) {
    Set<String> placeholders = new LinkedHashSet<>();

    if (string != null) {
      Matcher matcher = getPattern().matcher(string);
      while (matcher.find()) {
        placeholders.add("[%s](%s)".formatted("--translatable--", matcher.group("url")));
      }
    }
    return placeholders;
  }

  @Override
  public void check(String content, String target) {
    try {
      super.check(content, target);
    } catch (RegexCheckerException ex) {
      throw new MarkdownLinkIntegrityCheckerException("Variable types do not match.");
    }
  }
}
