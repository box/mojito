package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

public class PythonFStringIntegrityChecker extends RegexIntegrityChecker {

  @Override
  public String getRegex() {
    return "\\$\\{?[a-zA-Z_][a-zA-Z0-9_]*\\}?";
  }

  @Override
  public void check(String content, String target) {
    try {
      super.check(content, target);
    } catch (RegexCheckerException ex) {
      throw new PythonFStringIntegrityCheckerException("Variable types do not match.");
    }
  }
}
