package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

/**
 * Checks the validity of the composite format (C#).
 *
 * @author jaurambault
 */
public class CompositeFormatIntegrityChecker extends RegexIntegrityChecker {

  @Override
  public String getRegex() {
    return "(\\{){1,3}[^\\{\\}]*\\}+";
  }

  @Override
  public void check(String sourceContent, String targetContent)
      throws CompositeFormatIntegrityCheckerException {
    try {
      super.check(sourceContent, targetContent);
    } catch (RegexCheckerException rce) {
      throw new CompositeFormatIntegrityCheckerException(
          "Composite Format placeholders in source and target are different");
    }
  }
}
