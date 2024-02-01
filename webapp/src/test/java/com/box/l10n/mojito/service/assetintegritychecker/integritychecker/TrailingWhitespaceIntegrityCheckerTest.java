package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jyi
 */
public class TrailingWhitespaceIntegrityCheckerTest {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(TrailingWhitespaceIntegrityChecker.class);

  TrailingWhitespaceIntegrityChecker checker = new TrailingWhitespaceIntegrityChecker();

  @Test
  public void testTrailingWhitespaceCheckWorks() {
    String source = "There are %1 files and %2 folders \n";
    String target = "Il y a %1 fichiers et %2 dossiers \n";

    checker.check(source, target);
  }

  @Test
  public void testTrailingWhitespaceCheckWorksWithoutTrailingWhitespaces() {
    String source = "There are %1 files and %2 folders";
    String target = "Il y a %1 fichiers et %2 dossiers";

    checker.check(source, target);
  }

  @Test(expected = TrailingWhitespaceIntegrityCheckerException.class)
  public void testTrailingWhitespaceCheckCatchesRemovedNewline() {
    String source = "There are %1 files and %2 folders\n";
    String target = "Il y a %1 fichiers et %2 dossiers";

    checker.check(source, target);
  }

  @Test(expected = TrailingWhitespaceIntegrityCheckerException.class)
  public void testTrailingWhitespaceCheckCatchesRemovedSpace() {
    String source = "There are %1 files and %2 folders ";
    String target = "Il y a %1 fichiers et %2 dossiers";

    checker.check(source, target);
  }

  @Test(expected = TrailingWhitespaceIntegrityCheckerException.class)
  public void testTrailingWhitespaceCheckCatchesDifferentTrailingWhitespaces() {
    String source = "There are %1 files and %2 folders \n";
    String target = "Il y a %1 fichiers et %2 dossiers\n ";

    checker.check(source, target);
  }
}
