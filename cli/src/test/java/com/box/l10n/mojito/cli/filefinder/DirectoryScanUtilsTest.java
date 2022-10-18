package com.box.l10n.mojito.cli.filefinder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;
import java.util.Arrays;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class DirectoryScanUtilsTest {
  @Test
  public void testShouldScan() {
    final DirectoryScanUtils directoryScanUtils =
        new DirectoryScanUtils(
            Paths.get("test"),
            Arrays.asList("modules/*/src", "strings"),
            Arrays.asList("modules/1"));
    assertFalse(directoryScanUtils.shouldScan(Paths.get("test/.git")));
    assertFalse(directoryScanUtils.shouldScan(Paths.get("test/modules/1")));
    assertTrue(directoryScanUtils.shouldScan(Paths.get("test/")));
    assertTrue(directoryScanUtils.shouldScan(Paths.get("test/modules/")));
    assertTrue(directoryScanUtils.shouldScan(Paths.get("test/modules/2")));
    assertTrue(directoryScanUtils.shouldScan(Paths.get("test/modules/2/src/")));
    assertTrue(directoryScanUtils.shouldScan(Paths.get("test/modules/3/src/sub")));
    assertTrue(directoryScanUtils.shouldScan(Paths.get("test/strings")));
  }

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

  @Test
  public void testSplitPatternNonNull() {
    Assertions.assertThat(DirectoryScanUtils.splitPatterns(Arrays.asList("p1", "p2/p3")))
        .containsExactly(new String[] {"p1"}, new String[] {"p2", "p3"});
  }

  @Test
  public void testSplitPatterNull() {
    assertNull(DirectoryScanUtils.splitPatterns(null));
  }
}
