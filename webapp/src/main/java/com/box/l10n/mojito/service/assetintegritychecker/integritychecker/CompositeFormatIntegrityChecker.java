package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

/**
 * Checks the validity of the composite format (C#).
 *
 * @author jaurambault
 */
public class CompositeFormatIntegrityChecker extends RegexIntegrityChecker {

  @Override
  public String getRegex() {
    return "\\{.*?\\}";
  }

  @Override
  public void check(String sourceContent, String targetContent)
      throws CompositeFormatIntegrityCheckerException {
    try {
      super.check(sourceContent, targetContent);
    } catch (RegexCheckerException rce) {
      throw new CompositeFormatIntegrityCheckerException(rce);
    }
  }
}
