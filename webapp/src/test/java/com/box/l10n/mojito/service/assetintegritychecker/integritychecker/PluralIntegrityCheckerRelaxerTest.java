package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.Test;

public class PluralIntegrityCheckerRelaxerTest {

  @Test
  public void testShouldRelaxIntegrityCheck_whenPluralFormIsOther() {
    PluralIntegrityCheckerRelaxer relaxer = new PluralIntegrityCheckerRelaxer();
    boolean result =
        relaxer.shouldRelaxIntegrityCheck(
            "source %d", "target", "other", new PrintfLikeIntegrityChecker());

    assertFalse(result, "Integrity check should not be relaxed for plural form 'other'.");
  }

  @Test
  public void testShouldRelaxIntegrityCheck_whenPlaceholdersDifferByOne_One() {
    PluralIntegrityCheckerRelaxer relaxer = new PluralIntegrityCheckerRelaxer();
    boolean result =
        relaxer.shouldRelaxIntegrityCheck(
            "source %d", "target", "one", new PrintfLikeIntegrityChecker());

    assertTrue(
        result, "Integrity check should be relaxed when placeholders differ by one for 'one' form");
  }

  @Test
  public void testShouldRelaxIntegrityCheck_whenPlaceholdersDifferByOne_One_DuplicatePlaceholder() {
    PluralIntegrityCheckerRelaxer relaxer = new PluralIntegrityCheckerRelaxer();
    boolean result =
        relaxer.shouldRelaxIntegrityCheck(
            "source %d %d", "target", "one", new PrintfLikeIntegrityChecker());

    assertTrue(
        result,
        "Integrity check should be relaxed when placeholders differ by one for 'one' form (as placeholders are stored in a set, ignoring duplicates)");
  }

  @Test
  public void testShouldRelaxIntegrityCheck_whenPlaceholdersDifferByTwo_One() {
    PluralIntegrityCheckerRelaxer relaxer = new PluralIntegrityCheckerRelaxer();
    boolean result =
        relaxer.shouldRelaxIntegrityCheck(
            "source %1d %2d", "target", "one", new PrintfLikeIntegrityChecker());

    assertFalse(
        result,
        "Integrity check should not be relaxed when placeholders differ by two for 'one' form");
  }

  @Test
  public void testShouldRelaxIntegrityCheck_withUnsupportedChecker() {
    PluralIntegrityCheckerRelaxer relaxer = new PluralIntegrityCheckerRelaxer();

    boolean result =
        relaxer.shouldRelaxIntegrityCheck("source %d", "target", "one", new URLIntegrityChecker());

    assertFalse(result, "Integrity check should not be relaxed for unsupported checker types.");
  }
}
