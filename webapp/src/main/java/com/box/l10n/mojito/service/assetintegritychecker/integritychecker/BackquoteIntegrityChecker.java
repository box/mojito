package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks that there are the same backquoted strings in the source and target content.
 *
 * @author jyi
 */
public class BackquoteIntegrityChecker extends RegexIntegrityChecker {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(BackquoteIntegrityChecker.class);

  @Override
  public String getRegex() {
    return "`.*?`";
  }

  @Override
  public void check(String sourceContent, String targetContent)
      throws BackquoteIntegrityCheckerException {

    try {
      super.check(sourceContent, targetContent);
    } catch (RegexCheckerException rce) {
      throw new BackquoteIntegrityCheckerException(
          "Backquoted stings are different in source and target");
    }
  }
}
