package com.box.l10n.mojito.service.oaitranslate;

import static com.box.l10n.mojito.service.oaitranslate.WhitespaceUtils.restoreLeadingAndTrailingWhitespace;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.Test;

public class WhitespaceUtilsTest {

  @Test
  public void testRestoreLeadingAndTrailingWhitespace() {
    assertEquals("bar", restoreLeadingAndTrailingWhitespace("foo", "bar"));
    assertEquals("  bar", restoreLeadingAndTrailingWhitespace("  foo", "bar"));
    assertEquals("bar  ", restoreLeadingAndTrailingWhitespace("foo  ", "bar"));
    assertEquals("  bar  ", restoreLeadingAndTrailingWhitespace("  foo  ", "bar"));
    assertEquals("    bar", restoreLeadingAndTrailingWhitespace("    ", "bar"));
    assertEquals("bar", restoreLeadingAndTrailingWhitespace("", "bar"));
    assertEquals("    ", restoreLeadingAndTrailingWhitespace("  foo  ", ""));
    assertEquals("\t bar\n", restoreLeadingAndTrailingWhitespace("\t foo\n", "bar"));
  }
}
