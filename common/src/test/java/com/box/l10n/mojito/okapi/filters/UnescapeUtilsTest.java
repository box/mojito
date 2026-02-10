package com.box.l10n.mojito.okapi.filters;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UnescapeUtilsTest {

  UnescapeUtils unescapeUtils = new UnescapeUtils();

  @Test
  public void replaceCarriageReturn() {
    assertEquals("\r", unescapeUtils.replaceEscapedCarriageReturn("\\r"));
  }

  @Test
  public void replaceLineFeed() {
    assertEquals("\n", unescapeUtils.replaceEscapedLineFeed("\\n"));
  }

  @Test
  public void replaceEscapedCharacters() {
    assertEquals(".", unescapeUtils.replaceEscapedCharacters("\\."));
    assertEquals("@", unescapeUtils.replaceEscapedCharacters("\\@"));
    assertEquals("?", unescapeUtils.replaceEscapedCharacters("\\?"));
  }

  @Test
  public void replaceEscapedQuotes() {
    assertEquals("\" '", unescapeUtils.replaceEscapedQuotes("\\\" \\'"));
  }

  @Test
  public void replaceEscapedBackslash() {
    assertEquals("\\", unescapeUtils.replaceEscapedBackslash("\\\\"));
    assertEquals("C:\\Users\\test", unescapeUtils.replaceEscapedBackslash("C:\\\\Users\\\\test"));
    assertEquals("\\\\\\", unescapeUtils.replaceEscapedBackslash("\\\\\\\\\\\\"));
  }

  @Test
  public void collapseSpaces() {
    assertEquals(" a b c ", unescapeUtils.collapseSpaces("   a   b   c  "));
  }

  @Test
  public void replaceLineFeedWithSpace() {
    assertEquals("  a  ", unescapeUtils.replaceLineFeedWithSpace("\n a \n"));
  }

  @Test
  public void unescape() {
    assertEquals("  ' \" \n  ", unescapeUtils.unescape("  \' \\\" \\n  "));
  }

  @Test
  public void unescapeWithBackslash() {
    assertEquals("C:\\Users\\test", unescapeUtils.unescape("C:\\\\Users\\\\test"));
  }

  @Test
  public void unescapeComplexString() {
    assertEquals(
        "path\\to\\file\nwith \"quotes\"",
        unescapeUtils.unescape("path\\\\to\\\\file\\nwith \\\"quotes\\\""));
  }
}
