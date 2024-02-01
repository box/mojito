package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jyi
 */
public class BackquoteIntegrityCheckerTest {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(BackquoteIntegrityCheckerTest.class);

  BackquoteIntegrityChecker checker = new BackquoteIntegrityChecker();

  @Test
  public void testBackquoteCheckWorks() {
    String source = "Check `shared_item` and `collaborators`.";
    String target = "Vink `shared_item` en `collaborators` aan.";

    checker.check(source, target);
  }

  @Test
  public void testBackquoteWorksWithDifferentOrders() {
    String source = "Check `shared_item` and `collaborators`.";
    String target = "Vink `collaborators` en `shared_item` aan.";

    checker.check(source, target);
  }

  @Test(expected = BackquoteIntegrityCheckerException.class)
  public void testBackquoteCheckWorksWhenMissingAClosingQuote() {
    String source = "Check `shared_item` and `collaborators`.";
    String target = "Vink `collaborators` en `shared_item aan.";

    checker.check(source, target);
  }

  @Test(expected = BackquoteIntegrityCheckerException.class)
  public void testBackquoteCheckWorksWhenMissingQuotes() {
    String source = "Check `shared_item` and `collaborators`.";
    String target = "Vink shared_item en collaborators aan.";

    checker.check(source, target);
  }

  @Test(expected = BackquoteIntegrityCheckerException.class)
  public void testBackquoteCheckWorksWhenTagIsModified() {
    String source = "Check `shared_item` and `collaborators`.";
    String target = "Vink `medewerkers` en `shared_item` aan.";

    checker.check(source, target);
  }

  @Test(expected = BackquoteIntegrityCheckerException.class)
  public void testBackquoteCheckWorksWhenCountDoesNotMatch() {
    String source = "Check `shared_item` and `collaborators`.";
    String target = "Check `shared_item`, `permissions` and `collaborators`.";

    checker.check(source, target);
  }
}
