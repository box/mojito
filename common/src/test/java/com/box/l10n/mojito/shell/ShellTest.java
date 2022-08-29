package com.box.l10n.mojito.shell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class ShellTest {

  @Before
  public void onLinux() {
    Assume.assumeTrue(
        "sh must be available, so assuming linux or mac OS",
        SystemUtils.IS_OS_MAC_OSX || SystemUtils.IS_OS_LINUX);
  }

  @Test
  public void success() {
    Shell shell = new Shell();
    Result result = shell.exec("echo 'coucou'");
    assertEquals("coucou", result.getOutput());
    assertEquals("", result.getError());
    assertEquals(0, result.getExitCode());
  }

  @Test
  public void failure() {
    Shell shell = new Shell();
    Result result = shell.exec("doesnotexistfailure");
    assertEquals("", result.getOutput());
    assertTrue(
        result.getError().contains("doesnotexistfailure")
            && result.getError().contains("not found"));
    assertEquals(127, result.getExitCode());
  }
}
