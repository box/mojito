package com.box.l10n.mojito.okapi.filters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import org.junit.Test;

/** @author emagalindan */
public class MacStringsdictFilterKeyTest {

  @Test
  public void testGetNoteFromXMLCommentsInSkeletonNoComment() {
    String skeleton = "no comments";
    MacStringsdictFilterKey instance = new MacStringsdictFilterKey();
    String expResult = null;
    String result = instance.getNoteFromXMLCommentsInSkeleton(skeleton);
    assertEquals(expResult, result);
  }

  @Test
  public void testGetNoteFromXMLCommentsInSkeletonComment() {
    String skeleton = "blalbala <!-- a comment --> blalba";
    MacStringsdictFilterKey instance = new MacStringsdictFilterKey();
    String expResult = "a comment";
    String result = instance.getNoteFromXMLCommentsInSkeleton(skeleton);
    assertEquals(expResult, result);
  }

  @Test
  public void testGetNoteFromXMLCommentsInSkeletonMultipleComments() {
    String skeleton = "blalbala <!-- line 1 -->\n <!-- line 2 -->\n  <!-- line 3 --> blalba";
    MacStringsdictFilterKey instance = new MacStringsdictFilterKey();
    String expResult = "line 1 line 2 line 3";
    String result = instance.getNoteFromXMLCommentsInSkeleton(skeleton);
    assertEquals(expResult, result);
  }

  @Test
  public void testGetNoteFromXMLCommentsInSkeletonMultiline() {
    String skeleton = "blalbala <!-- line 1\nline 2 \nline 3 --> blalba";
    MacStringsdictFilterKey instance = new MacStringsdictFilterKey();
    String expResult = "line 1\nline 2 \nline 3";
    String result = instance.getNoteFromXMLCommentsInSkeleton(skeleton);
    assertEquals(expResult, result);
  }

  @Test
  public void testIsPluralGroupEnding() {
    MacStringsdictFilterKey instance = new MacStringsdictFilterKey();
    assertTrue(
        instance.isPluralGroupEnding(
            new DocumentPart("id", false, new GenericSkeleton("</dict>\n</dict>"))));
    assertTrue(
        instance.isPluralGroupEnding(
            new DocumentPart("id", false, new GenericSkeleton("</dict>\n   </dict>"))));
    assertTrue(
        instance.isPluralGroupEnding(
            new DocumentPart("id", false, new GenericSkeleton("</dict></dict>"))));
  }

  @Test
  public void testGetCldrPluralFormOfEvent() {
    MacStringsdictFilterKey instance = new MacStringsdictFilterKey();
    MacStringsdictFilterKey.MacStringsdictPluralsHolder macStringsdictPluralsHolder =
        instance.new MacStringsdictPluralsHolder();
    TextUnit textUnit = new TextUnit();
    textUnit.setName("plural_asdf_somestring_one");
    Event event = new Event(EventType.TEXT_UNIT, textUnit);
    assertEquals("one", macStringsdictPluralsHolder.getCldrPluralFormOfEvent(event));
  }
}
