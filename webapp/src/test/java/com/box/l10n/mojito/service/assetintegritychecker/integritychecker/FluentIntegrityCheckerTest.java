package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class FluentIntegrityCheckerTest {

  FluentIntegrityChecker checker = new FluentIntegrityChecker();

  @Test
  public void testValidPlaceholders() throws IntegrityCheckException {
    String source = "welcome = Welcome { $user }";
    String target = "welcome = Bienvenue { $user }";

    checker.check(source, target);
  }

  @Test
  public void testMessageReferencePreserved() throws IntegrityCheckException {
    String source = "greeting = Welcome { brand }";
    String target = "greeting = Bienvenue { brand }";

    checker.check(source, target);
  }

  @Test
  public void testMessageReferenceChangedFails() {
    String source = "greeting = Welcome { brand }";
    String target = "greeting = Bienvenue { marque }";

    FluentIntegrityCheckerException fie =
        assertThrows(FluentIntegrityCheckerException.class, () -> checker.check(source, target));

    assertEquals(
        "Target references do not match source. Found: [marque], expected: [brand]",
        fie.getMessage());
  }

  @Test
  public void testPlaceholderMismatch() throws IntegrityCheckException {
    String source = "welcome = Welcome { $user }";
    String target = "welcome = Bienvenue { $username }";

    FluentIntegrityCheckerException fie =
        assertThrows(FluentIntegrityCheckerException.class, () -> checker.check(source, target));

    assertEquals(
        "Target placeholders do not match source. Found: [username], expected: [user]",
        fie.getMessage());
  }

  @Test
  public void testSelectVariantsMatch() throws IntegrityCheckException {
    String source =
        "items = { $count ->\n"
            + "  [one] { $count } item\n"
            + " *[other] { $count } items\n"
            + "}";
    String target =
        "items = { $count ->\n"
            + "  [one] { $count } article\n"
            + " *[other] { $count } articles\n"
            + "}";

    checker.check(source, target);
  }

  @Test
  public void testSelectVariantsSubsetAllowed() throws IntegrityCheckException {
    String source =
        "items = { $count ->\n"
            + "  [one] { $count } item\n"
            + " *[other] { $count } items\n"
            + "}";
    String target = "items = { $count ->\n" + "  *[other] { $count } items\n" + "}";

    checker.check(source, target);
  }

  @Test
  public void testSelectVariantsSupersetAllowed() throws IntegrityCheckException {
    String source =
        "items = { $count ->\n"
            + "  [one] { $count } item\n"
            + " *[other] { $count } items\n"
            + "}";
    String target =
        "items = { $count ->\n"
            + "  [one] { $count } article\n"
            + "  [few] { $count } articles\n"
            + " *[other] { $count } articles\n"
            + "}";

    checker.check(source, target);
  }

  @Test
  public void testSelectDefaultVariantCanDiffer() throws IntegrityCheckException {
    String source =
        "items = { $count ->\n"
            + "  [one] { $count } item\n"
            + " *[other] { $count } items\n"
            + "}";
    String target =
        "items = { $count ->\n"
            + " *[one] { $count } item\n"
            + "  [other] { $count } items\n"
            + "}";

    checker.check(source, target);
  }

  @Test
  public void testInvalidTargetPattern() throws IntegrityCheckException {
    String source = "welcome = Welcome { $user }";
    String target = "welcome = Bienvenue { $user ";
    FluentIntegrityCheckerException fie =
        assertThrows(FluentIntegrityCheckerException.class, () -> checker.check(source, target));

    assertEquals(
        "Invalid target pattern - Unbalanced opening brace detected in Fluent message",
        fie.getMessage());
  }

  @Test
  public void testPrintfLikePlaceholdersAllowed() {
    String source = "usage = CPU usage: %s%%";
    String target = "usage = Utilisation CPU : %s%%";

    assertDoesNotThrow(() -> checker.check(source, target));
  }

  @Test
  public void testPythonFormatPlaceholdersAllowed() {
    String source = "greeting = Hello {username}, you have {count} messages";
    String target = "greeting = Bonjour {username}, vous avez {count} messages";

    assertDoesNotThrow(() -> checker.check(source, target));
  }

  @Test
  public void testFunctionCallNamePreserved() throws IntegrityCheckException {
    String source = "updated = Last seen { DATETIME($last-seen) }";
    String target = "updated = Derniere visite { DATETIME($last-seen) }";

    checker.check(source, target);
  }

  @Test
  public void testTranslatedVariantKeyFails() throws IntegrityCheckException {
    String source =
        "items = { $count ->\n"
            + "  [one] { $count } item\n"
            + " *[other] { $count } items\n"
            + "}";
    String target =
        "items = { $count ->\n"
            + "  [un] { $count } article\n"
            + " *[other] { $count } articles\n"
            + "}";

    FluentIntegrityCheckerException fie =
        assertThrows(FluentIntegrityCheckerException.class, () -> checker.check(source, target));

    assertEquals(
        "Variants for select expression $count contain unexpected key 'un'. Fluent plural categories must not be translated.",
        fie.getMessage());
  }

  @Test
  public void testMissingDefaultVariantFails() throws IntegrityCheckException {
    String source =
        "items = { $count ->\n"
            + "  [one] { $count } item\n"
            + " *[other] { $count } items\n"
            + "}";
    String target =
        "items = { $count ->\n"
            + "  [one] { $count } article\n"
            + "  [other] { $count } articles\n"
            + "}";

    FluentIntegrityCheckerException fie =
        assertThrows(FluentIntegrityCheckerException.class, () -> checker.check(source, target));

    assertEquals(
        "Invalid target pattern - Missing default (*) variant in Fluent select expression for $count",
        fie.getMessage());
  }

  @Test
  public void testMultipleDefaultVariantsFail() throws IntegrityCheckException {
    String source =
        "items = { $count ->\n"
            + "  [one] { $count } item\n"
            + " *[other] { $count } items\n"
            + "}";
    String target =
        "items = { $count ->\n"
            + " *[one] { $count } item\n"
            + " *[other] { $count } items\n"
            + "}";

    FluentIntegrityCheckerException fie =
        assertThrows(FluentIntegrityCheckerException.class, () -> checker.check(source, target));

    assertEquals(
        "Invalid target pattern - Multiple default variants found in Fluent select expression for $count",
        fie.getMessage());
  }
}
