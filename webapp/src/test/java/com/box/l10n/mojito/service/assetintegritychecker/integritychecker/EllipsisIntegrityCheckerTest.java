package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jyi
 */
public class EllipsisIntegrityCheckerTest {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(EllipsisIntegrityCheckerTest.class);

  EllipsisIntegrityChecker checker = new EllipsisIntegrityChecker();

  @Test
  public void testEllipsisIntegrityCheckWorks() throws EllipsisIntegrityCheckerException {

    String source = "Select…";
    String target = "선택…";

    checker.check(source, target);
  }

  @Test(expected = EllipsisIntegrityCheckerException.class)
  public void testEllipsisIntegrityCheckCatchesIncorrectEllipsis()
      throws EllipsisIntegrityCheckerException {

    String source = "Select…";
    String target = "선택...";

    checker.check(source, target);
  }
}
