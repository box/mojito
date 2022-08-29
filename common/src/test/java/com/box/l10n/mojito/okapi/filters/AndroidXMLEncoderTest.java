package com.box.l10n.mojito.okapi.filters;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AndroidXMLEncoderTest {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(AndroidXMLEncoderTest.class);

  AndroidXMLEncoder androidXMLEncoder;

  @Before
  public void before() {
    androidXMLEncoder = new AndroidXMLEncoder(false);
    androidXMLEncoder.unescapeUtils = new UnescapeUtils();
  }

  @Test
  public void testEscapeCommonAnnotation() {
    String actual =
        androidXMLEncoder.escapeCommon(
            "&lt;annotation font=\\\"title_emphasis\\\"&gt;Android&lt;/annotation&gt;!");
    assertEquals("<annotation font=\"title_emphasis\">Android</annotation>!", actual);
  }

  @Test
  public void testEscapeCommonB() {
    String actual = androidXMLEncoder.escapeCommon("&lt;b&gt;Android&lt;/b&gt;!");
    assertEquals("<b>Android</b>!", actual);
  }

  @Test
  public void testEscapeDoubleQuotes() {
    assertEquals("a\\\"b", androidXMLEncoder.escapeDoubleQuotes("a\"b"));
  }

  @Test
  public void testEscapeSingleQuote() {
    assertEquals("a\\'b", androidXMLEncoder.escapeSingleQuotes("a'b"));
  }

  @Test
  public void testEscape2SingleQuote() {
    assertEquals("a\\'\\'b", androidXMLEncoder.escapeSingleQuotes("a''b"));
  }

  @Test
  public void testEscapeStartSingleQuote() {
    assertEquals("\\'b", androidXMLEncoder.escapeSingleQuotes("'b"));
  }
}
