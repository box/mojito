package com.box.l10n.mojito.okapi.filters;

import static org.junit.Assert.assertEquals;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.resource.TextUnit;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jeanaurambault
 */
public class AndroidFilterTest {

  static Logger logger = LoggerFactory.getLogger(AndroidFilterTest.class);

  @Test
  public void testGetNoteFromXMLCommentsInSkeletonNoComment() {
    String skeleton = "no comments";
    AndroidFilter instance = new AndroidFilter();
    String expResult = null;
    String result = instance.getNoteFromXMLCommentsInSkeleton(skeleton);
    assertEquals(expResult, result);
  }

  @Test
  public void testGetNoteFromXMLCommentsInSkeletonComment() {
    String skeleton = "blalbala <!-- a comment --> blalba";
    AndroidFilter instance = new AndroidFilter();
    String expResult = "a comment";
    String result = instance.getNoteFromXMLCommentsInSkeleton(skeleton);
    assertEquals(expResult, result);
  }

  @Test
  public void testGetNoteFromXMLCommentsInSkeletonMultiline() {
    String skeleton = "blalbala <!-- line 1 -->\n<!-- line 2 -->\n<!-- line 3 --> blalba";
    AndroidFilter instance = new AndroidFilter();
    String expResult = "line 1 line 2 line 3";
    String result = instance.getNoteFromXMLCommentsInSkeleton(skeleton);
    assertEquals(expResult, result);
  }

  @Test
  public void testGetNoteFromXMLCommentsInSkeletonWithSkipTranslatable() {
    String skeleton =
        "<!-- comment for untranslatable -->\n"
            + "<string name=\"to_skip\" translatable=\"false\">To skip</string>\n"
            + "<!-- comment to extract -->\n"
            + "<string name=\"to_extract\">To extract</string>\n";
    AndroidFilter instance = new AndroidFilter();
    String expResult = "comment to extract";
    String result = instance.getNoteFromXMLCommentsInSkeleton(skeleton);
    assertEquals(expResult, result);
  }

  @Test
  public void testGetCldrPluralFormOfEvent() {
    AndroidFilter instance = new AndroidFilter();
    AndroidFilter.AndroidPluralsHolder androidPluralsHolder = instance.new AndroidPluralsHolder();
    TextUnit textUnit = new TextUnit();
    textUnit.setName("plural_asdf_somestring_one");
    Event event = new Event(EventType.TEXT_UNIT, textUnit);
    assertEquals("one", androidPluralsHolder.getCldrPluralFormOfEvent(event));
  }

  @Test
  public void testUnescaping() {
    testUnescaping("Nothing", "Nothing");
    testUnescaping("  does trim   ", "does trim");
    testUnescaping(
        "\"  does not trim in quote but remove them  \"",
        "  does not trim in quote but remove them  ");
    testUnescaping(" a \n b\n   c", "a b c");

    testUnescaping("process escaped line feed \\n", "process escaped line feed \n");
    testUnescaping("process escaped cariage return \\r", "process escaped cariage return \r");
    testUnescaping("unescape other character too \\a", "unescape other character too a");

    // this is cover by the global case but call them out since they should be escaped in android
    testUnescaping("unescape single quote \'", "unescape single quote '");
    testUnescaping("unescape double quote \\\"", "unescape double quote \"");
    testUnescaping("unescape at sign \\@", "unescape at sign @");
    testUnescaping("\\@ unescape starting at sign", "@ unescape starting at sign");
    testUnescaping("unescape question mark \\?", "unescape question mark ?");

    // those are cover by the XML parser, no need to process them even though called out in the doc
    testUnescaping("&apos;", "&apos;");
    testUnescaping("&quot;", "&quot;");
    testUnescaping("&lt;", "&lt;");

    // some html tag are supported
    testUnescaping("<i>bla</i>", "<i>bla</i>");

    // multi lines and spaces
    testUnescaping("\n line1   \n   line2 \n", "line1 line2");

    // unicode escape
    var str = "Unicode\\u00A0escape";
    testUnescaping("Unicode\\u00A0escape", "Unicode\u00A0escape");

    testUnescaping("\na\nb\nc\n", "a b c");
    testUnescaping("\n\n\n\na\n\n\nb\nc\n\n\n", "a b c");

    testUnescaping("\\u0020", " ");
    testUnescaping("\\u0020\\u0020\\u0020", "   ");
    testUnescaping("\\u0020test\\u0020", " test ");
    testUnescaping(
        "\\u0020\n\n\n\na\n\n\nb\nc\n\n\n\\u0020",
        "  a b c  "); // note that spaces are preserved, but the line feeds are turned into spaces
  }

  void testUnescaping(String input, String expected) {
    AndroidFilter instance = new AndroidFilter();
    instance.unescapeUtils = new UnescapeUtils();
    String s = instance.unescape(input);
    logger.debug("> Input:\n{}\n> Expected:\n{}\n> Actual:\n{}\n>>>", input, expected, s);
    assertEquals(expected, s);
  }
}
