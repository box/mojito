package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import org.junit.Ignore;
import org.junit.Test;

public class EmailIntegrityCheckerTest {

  EmailIntegrityChecker checker = new EmailIntegrityChecker();

  @Test
  public void testNoEmail() {
    String source = "There is no email";
    String target = "Il n'y a pas d'email";
    checker.check(source, target);
  }

  @Test(expected = EmailIntegrityCheckerException.class)
  public void testMissingInTarget() {
    String source = "There is an ja@test.com";
    String target = "Il n'y a pas d'email";
    checker.check(source, target);
  }

  @Test(expected = EmailIntegrityCheckerException.class)
  public void testAddedInTarget() {
    String source = "There is no email";
    String target = "Il n'y a un email ja@test.com";
    checker.check(source, target);
  }

  @Ignore // not supported yet
  @Test(expected = EmailIntegrityCheckerException.class)
  public void testDuplicates() {
    String source = "There is an ja@test.com";
    String target = "Il n'y a un email ja@test.com ja@test.com";
    checker.check(source, target);
  }
}
