package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

/**
 * Checker for composite format (Windows C# like)
 *
 * @author jaurambault
 */
public class CompositeFormatIntegrityCheckerTest {

  @Test
  public void testGetPlaceholder() throws CompositeFormatIntegrityCheckerException {

    CompositeFormatIntegrityChecker checker = new CompositeFormatIntegrityChecker();
    String string = "{0} '{2}' {0:0.00}% \"{1}\"";

    Set<String> placeholders = checker.getPlaceholders(string);

    Set<String> expected = new HashSet<>();
    expected.add("{0}");
    expected.add("{2}");
    expected.add("{0:0.00}");
    expected.add("{1}");

    assertEquals(expected, placeholders);
  }

  @Test
  public void testNoPlaceholder() throws CompositeFormatIntegrityCheckerException {

    CompositeFormatIntegrityChecker checker = new CompositeFormatIntegrityChecker();

    String source = "No placeholder";
    String target = "Pas de placholder";

    checker.check(source, target);
  }

  @Test
  public void testBase() throws CompositeFormatIntegrityCheckerException {

    CompositeFormatIntegrityChecker checker = new CompositeFormatIntegrityChecker();

    String source = "Failed to grant {0} access to {1}";
    String target = "Echec en donnant {0} accès à {1}";

    checker.check(source, target);
  }

  @Test
  public void testMustache() {
    CompositeFormatIntegrityChecker checker = new CompositeFormatIntegrityChecker();

    String source = "A {{mustache}} template";
    String target = "Un modėle {{mustache}}";

    checker.check(source, target);
  }

  @Test(expected = CompositeFormatIntegrityCheckerException.class)
  public void testMustacheInvalidMissingCurlyBraces() {
    CompositeFormatIntegrityChecker checker = new CompositeFormatIntegrityChecker();

    String source = "A {{mustache}} template";
    String target = "Un modėle {mustache}";

    checker.check(source, target);
  }

  @Test(expected = CompositeFormatIntegrityCheckerException.class)
  public void testMustacheInvalidMissingPlaceholder() {
    CompositeFormatIntegrityChecker checker = new CompositeFormatIntegrityChecker();

    String source = "A {{mustache}} template";
    String target = "Un modėle";

    checker.check(source, target);
  }

  @Test(expected = CompositeFormatIntegrityCheckerException.class)
  public void testMissingPlaceholder() throws CompositeFormatIntegrityCheckerException {

    CompositeFormatIntegrityChecker checker = new CompositeFormatIntegrityChecker();

    String source = "{1}";
    String target = "";

    checker.check(source, target);
  }

  @Test
  public void testInvertedPlaceholder() throws CompositeFormatIntegrityCheckerException {

    CompositeFormatIntegrityChecker checker = new CompositeFormatIntegrityChecker();

    String source = "{0} {1}";
    String target = "{1} {0}";

    checker.check(source, target);
  }

  /**
   * regression test for when we were using {@link MessageFormatIntegrityChecker} as checker for
   * Windows
   */
  @Test
  public void testCharactersAroundPlaceholder() throws CompositeFormatIntegrityCheckerException {

    CompositeFormatIntegrityChecker checker = new CompositeFormatIntegrityChecker();

    String source = "'{0}' copied";
    String target = "\"{0}\" copiado";

    checker.check(source, target);
  }

  /**
   * regression test for when we were using {@link MessageFormatIntegrityChecker} as checker for
   * Windows
   */
  @Test
  public void testCurlyBraces() throws CompositeFormatIntegrityCheckerException {

    CompositeFormatIntegrityChecker checker = new CompositeFormatIntegrityChecker();

    String source = "{0:0.00}% used";
    String target = "{0:0.00}% usados";

    checker.check(source, target);
  }
}
