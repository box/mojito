package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerTest {

  /** logger */
  static Logger logger =
      LoggerFactory.getLogger(PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker.class);

  @Test
  public void testGetPlaceholder()
      throws PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException {

    PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker checker =
        new PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker();
    String string = "%1$ld개 파일과 %2$@개 폴더가 있습니다 %d %1$-04d %1$04d %2$.2ld";

    Set<String> placeholders = checker.getPlaceholders(string);

    Set<String> expected = new HashSet<>();
    expected.add("%1$ld");
    expected.add("%2$@");
    expected.add("%d");
    expected.add("%1$-04d");
    expected.add("%1$04d");
    expected.add("%2$.2ld");

    logger.debug("expected: {}", expected);
    logger.debug("actual: {}", placeholders);

    assertEquals(expected, placeholders);
  }

  @Test
  public void testMacSinglePlaceholderCheckWorks()
      throws PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException {

    PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker checker =
        new PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker();
    String source = "There are %@ files";
    String target = "Il y a %@ fichiers";

    checker.check(source, target);
  }

  @Test
  public void testMacMultiplePlaceholdersCheckWorks()
      throws PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException {

    PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker checker =
        new PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker();
    String source = "There are %1$ld files and %2$@ folders";
    String target = "Il y a %1$ld fichiers et %2$@ dossiers";

    checker.check(source, target);
  }

  @Test
  public void testMacPlaceholdersCheckWithRemovedSpacesWorks1()
      throws PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException {

    PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker checker =
        new PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker();
    String source = "%1$@ of %2$@ %3$@";
    String target = "%1$@/%2$@%3$@";

    checker.check(source, target);
  }

  @Test
  public void testMacPlaceholdersCheckWithRemovedSpacesWorks2()
      throws PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException {

    PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker checker =
        new PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker();
    String source = "%1$@ %2$@:";
    String target = "%1$@%2$@:";

    checker.check(source, target);
  }

  @Test
  public void testMacMultiplePlaceholdersAndTranslationCheckWorks()
      throws PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException {

    PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker checker =
        new PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker();
    String source = "There are %1$ld files and %2$@ folders";
    String target = "%1$ld개 파일과 %2$@개 폴더가 있습니다";

    checker.check(source, target);
  }

  @Test
  public void testMacTranslationAndMultiplePlaceholdersCheckWorks()
      throws PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException {

    PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker checker =
        new PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker();
    String source = "%1$ld files, %2$@ folders";
    String target = "파일%1$ld, 폴더%2$@";

    checker.check(source, target);
  }

  @Test
  public void testMacPlaceholderCheckWorksWithDifferentOrder()
      throws PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException {

    PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker checker =
        new PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker();
    String source = "There are %1$lld files and %2$.2ld folders";
    String target = "Il y a %2$.2ld dossiers et %1$lld fichiers";

    checker.check(source, target);
  }

  @Test
  public void testMacPlaceholderCheckFailsIfDifferentPlaceholdersCount()
      throws PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException {

    PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker checker =
        new PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker();
    String source = "There are %1$04d files and %2$@ folders";
    String target = "Il y a %1$04d fichiers";

    try {
      checker.check(source, target);
      fail("PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException must be thrown");
    } catch (PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException e) {
      assertEquals(e.getMessage(), "Placeholders in source and target are different");
    }
  }

  @Test
  public void testMacPlaceholderCheckFailsIfSamePlaceholdersCountButSomeRepeatedOrMissing()
      throws PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException {

    PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker checker =
        new PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker();
    String source = "There are %1$-04d files and %2$@ folders";
    String target = "Il y a %1$-04d fichiers et %1$-04d dossiers";

    try {
      checker.check(source, target);
      fail("PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException must be thrown");
    } catch (PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException e) {
      assertEquals(e.getMessage(), "Placeholders in source and target are different");
    }
  }

  @Test
  public void testMacPlaceholderCheckFailsIfSamePlaceholdersCountButSpecifierModified()
      throws PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException {

    PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker checker =
        new PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker();
    String source = "There are %1$.1ld files and %2$.2ld folders";
    String target = "Il y a %1$.1ld fichiers et %1$.1ld dossiers";

    try {
      checker.check(source, target);
      fail("PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException must be thrown");
    } catch (PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException e) {
      assertEquals(e.getMessage(), "Placeholders in source and target are different");
    }
  }

  @Test
  public void testAndroidSinglePlaceholderCheckWorks()
      throws PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException {

    PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker checker =
        new PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker();
    String source = "There are %d files";
    String target = "Il y a %d fichiers";

    checker.check(source, target);
  }

  @Test
  public void testAndroidMultiplePlaceholdersCheckWorks()
      throws PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException {

    PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker checker =
        new PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker();
    String source = "There are %1$s files and %2$d folders";
    String target = "Il y a %1$s fichiers et %2$d dossiers";

    checker.check(source, target);
  }

  @Test
  public void testAndroidMultiplePlaceholdersAndTranslationCheckWorks()
      throws PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException {

    PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker checker =
        new PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker();
    String source = "There are %1$s files and %2$d folders";
    String target = "%1$s개 파일과 %2$d개 폴더가 있습니다";

    checker.check(source, target);
  }

  @Test
  public void testAndroidTranslationAndMultiplePlaceholdersCheckWorks()
      throws PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException {

    PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker checker =
        new PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker();
    String source = "%1$s files, %2$d folders";
    String target = "파일%1$s, 폴더%2$d";

    checker.check(source, target);
  }

  @Test
  public void testAndroidPlaceholderCheckWorksWithDifferentOrder()
      throws PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException {

    PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker checker =
        new PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker();
    String source = "There are %1$d files and %2$d folders";
    String target = "Il y a %2$d dossiers et %1$d fichiers";

    checker.check(source, target);
  }

  @Test
  public void testAndroidPlaceholderCheckFailsIfDifferentPlaceholdersCount()
      throws PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException {

    PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker checker =
        new PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker();
    String source = "There are %1$d files and %2$d folders";
    String target = "Il y a %1$d fichiers";

    try {
      checker.check(source, target);
      fail("PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException must be thrown");
    } catch (PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException e) {
      assertEquals(e.getMessage(), "Placeholders in source and target are different");
    }
  }

  @Test
  public void testAndroidPlaceholderCheckFailsIfSamePlaceholdersCountButSomeRepeatedOrMissing()
      throws PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException {

    PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker checker =
        new PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker();
    String source = "There are %1$d files and %2$d folders";
    String target = "Il y a %1$d fichiers et %1$d dossiers";

    try {
      checker.check(source, target);
      fail("PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException must be thrown");
    } catch (PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException e) {
      assertEquals(e.getMessage(), "Placeholders in source and target are different");
    }
  }

  @Test
  public void testAndroidPlaceholderCheckFailsIfSamePlaceholdersCountButSpecifierModified()
      throws PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException {

    PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker checker =
        new PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker();
    String source = "There are %1$d files and %2$d folders";
    String target = "Il y a %1$d fichiers et %2$s dossiers";

    try {
      checker.check(source, target);
      fail("PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException must be thrown");
    } catch (PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException e) {
      assertEquals(e.getMessage(), "Placeholders in source and target are different");
    }
  }

  @Test
  public void testSpecifierWithRemovedSpace()
      throws PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException {

    PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker checker =
        new PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker();
    String source = "%1$s of %2$s GB";
    String target = "%1$s/%2$sGB";

    checker.check(source, target);
  }

  @Test
  public void testIncorrectlyModifiedSpecifier()
      throws PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException {

    PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker checker =
        new PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker();
    String source = "%1.1f GB";
    String target = "%1,1f Go";

    try {
      checker.check(source, target);
      fail("PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException must be thrown");
    } catch (PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException e) {
      assertEquals(e.getMessage(), "Placeholders in source and target are different");
    }
  }

  @Test
  public void testTokenIsNotRecognizedIfPrecededByRightCurlyBracket()
      throws PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException {
    // Verify that "% to" is not matched as a token
    PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker checker =
        new PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker();
    String source = "from {from_val}% to {to_val}%";
    String target = "{from_val}%-ról {to_val}%-ra";

    checker.check(source, target);
  }

  @Test
  public void testTokenIsNotRecognizedIfPrecededByRightBracket()
      throws PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException {
    // Verify that "% to" is not matched as a token
    PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker checker =
        new PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker();
    String source = "from (from_val)% to (to_val)%";
    String target = "(from_val)%-ról (to_val)%-ra";

    checker.check(source, target);
  }

  @Test
  public void testStandalonePercentageCheckFails()
      throws PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException {
    PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker checker =
        new PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker();
    String source = "The level is down to 10% of the original.";
    String target = "The level is down to 10% of the original.";

    try {
      checker.check(source, target);
      fail("PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException must be thrown");
    } catch (PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException e) {
      assertEquals(
          e.getMessage(), "Standalone % found, percentages should be doubled (%%) for formatting.");
    }
  }

  @Test
  public void testDoublePercentageCheckPasses()
      throws PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException {
    PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker checker =
        new PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker();
    String source = "The level is down to 10%% of the original.";
    String target = "The level is down to 10%% of the original.";

    checker.check(source, target);
  }

  @Test
  public void testTurkishPercentageFormatting()
      throws PrintfLikeIgnorePercentageAfterBracketsIntegrityCheckerException {
    PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker checker =
        new PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker();
    String source = "The level is down to 10%% of the original.";
    String target = "Seviye, orijinalin %%10'una düştü.";

    checker.check(source, target);
  }
}
