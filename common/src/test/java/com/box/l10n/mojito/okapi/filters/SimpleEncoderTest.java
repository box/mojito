package com.box.l10n.mojito.okapi.filters;

import static org.junit.Assert.assertEquals;

import net.sf.okapi.common.encoder.EncoderContext;
import org.junit.Before;
import org.junit.Test;

/** Tests for {@link SimpleEncoder} to verify C-style escape sequences for PO files. */
public class SimpleEncoderTest {

  private SimpleEncoder encoder;
  private EncoderContext context;

  @Before
  public void setUp() {
    encoder = new SimpleEncoder();
    encoder.setOptions(null, "UTF-8", "\n");
    context = EncoderContext.TEXT;
  }

  @Test
  public void testEncodeBackslash() {
    assertEquals("\\\\", encoder.encode('\\', context));
  }

  @Test
  public void testEncodeNewline() {
    assertEquals("\\n", encoder.encode('\n', context));
  }

  @Test
  public void testEncodeCarriageReturn() {
    assertEquals("\\r", encoder.encode('\r', context));
  }

  @Test
  public void testEncodeDoubleQuote() {
    assertEquals("\\\"", encoder.encode('"', context));
  }

  @Test
  public void testEncodeRegularCharacter() {
    assertEquals("a", encoder.encode('a', context));
    assertEquals("Z", encoder.encode('Z', context));
    assertEquals("1", encoder.encode('1', context));
    assertEquals("/", encoder.encode('/', context));
  }

  @Test
  public void testEncodeStringWithBackslash() {
    String input = "C:\\Users\\test";
    String expected = "C:\\\\Users\\\\test";
    assertEquals(expected, encoder.encode(input, context));
  }

  @Test
  public void testEncodeStringWithNewline() {
    String input = "line1\nline2";
    String expected = "line1\\nline2";
    assertEquals(expected, encoder.encode(input, context));
  }

  @Test
  public void testEncodeStringWithMultipleEscapes() {
    String input = "path\\to\\file\nwith \"quotes\"";
    String expected = "path\\\\to\\\\file\\nwith \\\"quotes\\\"";
    assertEquals(expected, encoder.encode(input, context));
  }

  @Test
  public void testEncodeEmptyString() {
    assertEquals("", encoder.encode("", context));
  }

  @Test
  public void testEncodeOnlyBackslashes() {
    assertEquals("\\\\\\\\\\\\", encoder.encode("\\\\\\", context));
  }
}
