package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.Test;

public class PythonFStringIntegrityCheckerTest {

  PythonFStringIntegrityChecker checker = new PythonFStringIntegrityChecker();

  @Test
  public void testPlaceholderOK() {
    String source = "This is a $placeholder";
    String target = "C'est un $placeholder";
    checker.check(source, target);
  }

  @Test
  public void testPlaceholderMissing() {
    String source = "This is a $placeholder";
    String target = "C'est un $placehor";
    assertThrowsExactly(
        PythonFStringIntegrityCheckerException.class, () -> checker.check(source, target));
  }

  @Test
  public void testPlaceholderCurlyOK() {
    String source = "This is a ${placeholder}";
    String target = "C'est un ${placeholder}";
    checker.check(source, target);
  }

  @Test
  public void testPlaceholderCurlyMissing() {
    String source = "This is a ${placeholder}";
    String target = "C'est un ${placehor";
    assertThrowsExactly(
        PythonFStringIntegrityCheckerException.class, () -> checker.check(source, target));
  }
}
