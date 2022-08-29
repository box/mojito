package com.box.l10n.mojito.cli.filefinder.locale;

import static org.junit.Assert.*;

import org.junit.Test;

/** @author jaurambault */
public class AndroidLocaleTypeTest {

  @Test
  public void testGetTargetLocaleRepresentationNoRegion() {
    String targetLocale = "fr";
    AndroidLocaleType instance = new AndroidLocaleType();
    String expResult = "fr";
    String result = instance.getTargetLocaleRepresentation(targetLocale);
    assertEquals(expResult, result);
  }

  @Test
  public void testGetTargetLocaleRepresentationWithRegion() {
    String targetLocale = "en-GB";
    AndroidLocaleType instance = new AndroidLocaleType();
    String expResult = "en-rGB";
    String result = instance.getTargetLocaleRepresentation(targetLocale);
    assertEquals(expResult, result);
  }
}
