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
  public void testWorksWithNoTag() {
    String source = "This is testing the translate tag checker.";
    String target = "Ceci teste le vérificateur de balises de traduction";

    checker.check(source, target);
  }

  @Test
  public void testWorksNormalTag() {
    String source = "This is testing the <span>translate</span> tag checker.";
    String target = "Ceci teste le vérificateur de balises de <span>traduction</span>";

    checker.check(source, target);
  }

  @Test
  public void testThrowsWithTag() {
    String source = "This is testing the translate tag checker.";
    String target =
        "Ceci teste le <span translate=\"no\">vérificateur</span> de balises de traduction";

    try {
      checker.check(source, target);
      fail("Check should fail with span translate tag in target string.");
    } catch (IntegrityCheckException e) {
    }
  }

  @Test
  public void testBackwardsTag() {
    String source = "This is testing the translate tag checker.";
    String target = "يتم الآن اختبار <span translate=\"no\">مدقق</span> علامات الترجمة.";

    try {
      checker.check(source, target);
      fail("Check should fail with span translate tag in target string.");
    } catch (IntegrityCheckException e) {
    }
  }
}
