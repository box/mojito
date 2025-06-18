package com.box.l10n.mojito.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class FilePositionTest {

  @Test
  public void testOnlyPath() {
    FilePosition result = FilePosition.from("src/Main.java");
    assertEquals("src/Main.java", result.path());
    assertNull(result.line());
    assertNull(result.col());
  }

  @Test
  public void testPathAndLine() {
    FilePosition result = FilePosition.from("src/Main.java:42");
    assertEquals("src/Main.java", result.path());
    assertEquals(Integer.valueOf(42), result.line());
    assertNull(result.col());
  }

  @Test
  public void testPathLineAndCol() {
    FilePosition result = FilePosition.from("src/Main.java:42:7");
    assertEquals("src/Main.java", result.path());
    assertEquals(Integer.valueOf(42), result.line());
    assertEquals(Integer.valueOf(7), result.col());
  }

  @Test
  public void testInvalidFormatDefaults() {
    FilePosition result = FilePosition.from("not valid");
    assertEquals("not valid", result.path());
    assertNull(result.line());
    assertNull(result.col());
  }

  @Test
  public void testColOnlyIgnored() {
    FilePosition result = FilePosition.from("file.java::88");
    assertEquals("file.java:", result.path());
    assertEquals(Integer.valueOf(88), result.line());
    assertNull(result.col());
  }
}
