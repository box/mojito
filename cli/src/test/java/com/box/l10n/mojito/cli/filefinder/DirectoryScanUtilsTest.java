package com.box.l10n.mojito.cli.filefinder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DirectoryScanUtilsTest {

  @Test
  public void testShouldScanDirectory() {
    assertTrue(DirectoryScanUtils.shouldScanDirectory(new String[] {}, new String[] {}));
    assertTrue(DirectoryScanUtils.shouldScanDirectory(new String[] {}, new String[] {"locale"}));
    assertTrue(DirectoryScanUtils.shouldScanDirectory(new String[] {"locale"}, new String[] {}));

    assertTrue(DirectoryScanUtils.shouldScanDirectory(new String[] {"*"}, new String[] {"locale"}));
    assertTrue(
        DirectoryScanUtils.shouldScanDirectory(new String[] {"locale"}, new String[] {"locale"}));
    assertFalse(
        DirectoryScanUtils.shouldScanDirectory(new String[] {"locale"}, new String[] {"locale2"}));
    assertTrue(
        DirectoryScanUtils.shouldScanDirectory(
            new String[] {"webapp", "locale"}, new String[] {"webapp", "locale"}));
    assertFalse(
        DirectoryScanUtils.shouldScanDirectory(
            new String[] {"webapp", "locale"}, new String[] {"webapp", "locale2"}));
    assertFalse(
        DirectoryScanUtils.shouldScanDirectory(
            new String[] {"webapp", "locale"}, new String[] {"webapp2", "locale"}));
    assertTrue(
        DirectoryScanUtils.shouldScanDirectory(
            new String[] {"*"}, new String[] {"webapp", "locale"}));
    assertTrue(
        DirectoryScanUtils.shouldScanDirectory(
            new String[] {"*", "*"}, new String[] {"webapp", "locale"}));
    assertTrue(
        DirectoryScanUtils.shouldScanDirectory(
            new String[] {"*", "locale"}, new String[] {"webapp", "locale"}));
    assertFalse(
        DirectoryScanUtils.shouldScanDirectory(
            new String[] {"*", "locale"}, new String[] {"webapp", "locale2"}));

    assertTrue(
        DirectoryScanUtils.shouldScanDirectory(new String[] {"*", "locale"}, new String[] {}));
    assertTrue(
        DirectoryScanUtils.shouldScanDirectory(
            new String[] {"*", "locale"}, new String[] {"webapp"}));

    assertTrue(
        DirectoryScanUtils.shouldScanDirectory(
            new String[] {"modules", "*", "locale"}, new String[] {"modules", "api"}));
    assertTrue(
        DirectoryScanUtils.shouldScanDirectory(
            new String[] {"modules", "*", "locale"}, new String[] {"modules", "api2"}));
    assertTrue(
        DirectoryScanUtils.shouldScanDirectory(
            new String[] {"modules", "*", "locale"}, new String[] {"modules", "api2", "locale"}));
    assertFalse(
        DirectoryScanUtils.shouldScanDirectory(
            new String[] {"modules", "*", "locale"}, new String[] {"modules", "api2", "locale2"}));
    assertTrue(
        DirectoryScanUtils.shouldScanDirectory(
            new String[] {"modules", "*", "locale"},
            new String[] {"modules", "api2", "locale", "sub1"}));
  }
}
