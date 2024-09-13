package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpanTranslateTagIntegrityCheckerTest {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(SpanTranslateTagIntegrityCheckerTest.class);

  SpanTranslateTagIntegrityChecker checker = new SpanTranslateTagIntegrityChecker();

  @Test
  public void testWithNoTags() {
    String source = "This is testing the translate tag checker.";
    String target = "Ceci teste le vérificateur de balises de traduction";

    checker.check(source, target);
  }

  @Test
  public void testNormalTag() {
    String source = "This is testing the <span>translate</span> tag checker.";
    String target = "Ceci teste le vérificateur de balises de <span>traduction</span>";

    checker.check(source, target);
  }

  @Test
  public void testBothTag() {
    String source = "This is testing the <span translate=\"no\">translate</span> tag checker.";
    String target =
        "Ceci teste le vérificateur de balises de <span translate=\"no\">traduction</span>";

    try {
      checker.check(source, target);
    } catch (IntegrityCheckException e) {
      fail("Check shouldn't fail with span translate tag in both tags.");
    }
  }

  @Test
  public void testOneTag() {
    String source = "This is testing the <span translate=\"no\">translate</span> tag checker.";
    String target = "Ceci teste le vérificateur de balises de traduction";

    try {
      checker.check(source, target);
    } catch (IntegrityCheckException e) {
      fail("Check shouldn't fail with span translate tag only in source text.");
    }
  }

  @Test
  public void testThrowsWithTag() {
    String source = "This is testing the translate tag checker.";
    String target =
        "Ceci teste le vérificateur de balises de <span translate=\"no\">traduction</span>";

    try {
      checker.check(source, target);
      fail("Check should fail if span tag is in target but not source.");
    } catch (IntegrityCheckException e) {
    }
  }

  @Test
  public void testThrowsWithNoClosingTag() {
    String source = "This is testing the translate tag checker.";
    String target = "Ceci teste le <span translate=\"no\">vérificateur de balises de traduction";

    try {
      checker.check(source, target);
      fail("Check should fail with span translate opening tag in target string.");
    } catch (IntegrityCheckException e) {
    }
  }

  @Test
  public void testRightToLeftTag() {
    String source = "This is testing the translate tag checker.";
    String target = "يتم الآن اختبار <span translate=\"no\">مدقق</span> علامات الترجمة.";

    try {
      checker.check(source, target);
      fail("Check should fail with span translate tag in target string.");
    } catch (IntegrityCheckException e) {
    }
  }

  @Test
  public void testRightToLeftTagOne() {
    String source = "This is testing the <span translate=\"no\">translate</span> tag checker.";
    String target = "يتم الآن اختبار مدقق علامات الترجمة.";

    try {
      checker.check(source, target);
    } catch (IntegrityCheckException e) {
      fail("Check shouldn't fail with span translate tag in just the source tag.");
    }
  }

  @Test
  public void testRightToLeftTagBoth() {
    String source = "This is testing the <span translate=\"no\">translate</span> tag checker.";
    String target = "يتم الآن اختبار <span translate=\"no\">مدقق</span> علامات الترجمة.";

    try {
      checker.check(source, target);
    } catch (IntegrityCheckException e) {
      fail("Check shouldn't fail with span translate tag in both target and source.");
    }
  }
}
