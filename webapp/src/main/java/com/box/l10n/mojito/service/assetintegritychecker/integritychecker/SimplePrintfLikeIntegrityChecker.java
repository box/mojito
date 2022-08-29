package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import static com.box.l10n.mojito.regex.PlaceholderRegularExpressions.SIMPLE_PRINTF_REGEX;

/**
 * Checks that there are the same placeholders like %1, %2, etc in the source and target content,
 * order is not important.
 *
 * @author jyi
 */
public class SimplePrintfLikeIntegrityChecker extends RegexIntegrityChecker {

  @Override
  public String getRegex() {
    return SIMPLE_PRINTF_REGEX.getRegex();
  }

  @Override
  public void check(String sourceContent, String targetContent)
      throws PrintfLikeIntegrityCheckerException {

    try {
      super.check(sourceContent, targetContent);
    } catch (RegexCheckerException rce) {
      throw new SimplePrintfLikeIntegrityCheckerException((rce.getMessage()));
    }
  }
}
