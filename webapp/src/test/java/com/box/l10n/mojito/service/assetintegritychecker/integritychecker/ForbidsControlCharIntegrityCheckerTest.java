package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.Test;

public class ForbidsControlCharIntegrityCheckerTest {

  ForbidsControlCharIntegrityChecker checker = new ForbidsControlCharIntegrityChecker();

  @Test
  public void testValidTarget() throws IntegrityCheckException {
    checker.check("", "Hello world!");
    checker.check("", "Tabs\tare\nfine.\r");
  }

  @Test
  public void testNullCharInTarget() {
    try {
      checker.check("", "Bad\u0000String");
      fail("IntegrityCheckException expected");
    } catch (IntegrityCheckException e) {
      assertTrue(e.getMessage().contains("Forbidden control character (U+0000)"));
    }
  }

  @Test
  public void testUnitSeparatorInTarget() {
    try {
      checker.check("", "Bad\u001FString");
      fail("IntegrityCheckException expected");
    } catch (IntegrityCheckException e) {
      assertTrue(e.getMessage().contains("Forbidden control character (U+001F)"));
    }
  }

  @Test
  public void testBellCharInTarget() {
    try {
      checker.check("", "Bad\u0007String");
      fail("IntegrityCheckException expected");
    } catch (IntegrityCheckException e) {
      assertTrue(e.getMessage().contains("Forbidden control character (U+0007)"));
    }
  }
}
