package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jyi
 */
public class SimplePrintfLikeIntegrityCheckerTest {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(SimplePrintfLikeIntegrityCheckerTest.class);

  SimplePrintfLikeIntegrityChecker checker = new SimplePrintfLikeIntegrityChecker();

  @Test
  public void testPlaceholderCheckWorks() throws SimplePrintfLikeIntegrityCheckerException {

    String source = "There are %1 files and %2 folders";
    String target = "Il y a %1 fichiers et %2 dossiers";

    checker.check(source, target);
  }

  @Test
  public void testPlaceholderAndTranslationCheckWorks()
      throws SimplePrintfLikeIntegrityCheckerException {

    String source = "There are %1 files and %2 folders";
    String target = "%1개 파일과 %2개 폴더가 있습니다";

    checker.check(source, target);
  }

  @Test
  public void testTranslationAndPlaceholderCheckWorks()
      throws SimplePrintfLikeIntegrityCheckerException {

    String source = "%1 file, %2 folder";
    String target = "파일%1, 폴더%2";

    checker.check(source, target);
  }

  @Test
  public void testPlaceholderCheckWorksWithDifferentOrder()
      throws SimplePrintfLikeIntegrityCheckerException {

    String source = "There are %1 files and %2 folders";
    String target = "Il y a %2 dossiers et %1 fichiers";

    checker.check(source, target);
  }

  @Test
  public void testPlaceholderCheckFailsIfDifferentPlaceholdersCount()
      throws SimplePrintfLikeIntegrityCheckerException {

    String source = "There are %1 files and %2 folders";
    String target = "Il y a %1 fichiers";

    try {
      checker.check(source, target);
      fail("SimplePrintfLikeIntegrityCheckerException must be thrown");
    } catch (SimplePrintfLikeIntegrityCheckerException e) {
      assertEquals(e.getMessage(), "Placeholders in source and target are different");
    }
  }

  @Test
  public void testPlaceholderCheckFailsIfSamePlaceholdersCountButSomeRepeatedOrMissing()
      throws SimplePrintfLikeIntegrityCheckerException {

    String source = "There are %1 files and %2 folders";
    String target = "Il y a %1 fichiers et %1 dossiers";

    try {
      checker.check(source, target);
      fail("SimplePrintfLikeIntegrityCheckerException must be thrown");
    } catch (SimplePrintfLikeIntegrityCheckerException e) {
      assertEquals(e.getMessage(), "Placeholders in source and target are different");
    }
  }

  @Test
  public void testWebappPlaceholderWithoutSpaces1()
      throws SimplePrintfLikeIntegrityCheckerException {

    String source =
        "%1Selecting 'All managed users' gives anyone in your organization the ability to invite groups to collaborate.%1";
    String target =
        "%1\"Все управляемые пользователи\" — все пользователи вашей организации могу приглашать группы для совместной работы.%1";

    checker.check(source, target);
  }

  @Test
  public void testWebappPlaceholderWithoutSpaces2()
      throws SimplePrintfLikeIntegrityCheckerException {

    String source = "%1Tip%2: This option will only affect the HTML folder embed widget.";
    String target = "%1Совет%2. Эта настройка повлияет только на встроенный HTML-виджет папки.";

    checker.check(source, target);
  }
}
