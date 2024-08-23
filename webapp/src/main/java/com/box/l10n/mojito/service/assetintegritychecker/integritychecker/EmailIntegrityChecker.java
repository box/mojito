package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

public class EmailIntegrityChecker extends RegexIntegrityChecker {

  static final String EMAIL_REGEX = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}";

  @Override
  public String getRegex() {
    return EMAIL_REGEX;
  }

  @Override
  public void check(String content, String target) {
    try {
      super.check(content, target);
    } catch (RegexCheckerException ex) {
      throw new EmailIntegrityCheckerException("Emails are changed.");
    }
  }
}
