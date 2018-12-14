package com.box.l10n.mojito.okapi.filters;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.resource.TextUnit;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 *
 * @author emagalindan
 */
public class MacStringsdictFilterTest {

    @Test
    public void testGetNoteFromXMLCommentsInSkeletonNoComment() {
        String skeleton = "no comments";
        MacStringsdictFilter instance = new MacStringsdictFilter();
        String expResult = null;
        String result = instance.getNoteFromXMLCommentsInSkeleton(skeleton);
        assertEquals(expResult, result);
    }

    @Test
    public void testGetNoteFromXMLCommentsInSkeletonComment() {
        String skeleton = "blalbala <!-- a comment --> blalba";
        MacStringsdictFilter instance = new MacStringsdictFilter();
        String expResult = "a comment";
        String result = instance.getNoteFromXMLCommentsInSkeleton(skeleton);
        assertEquals(expResult, result);
    }

    @Test
    public void testGetNoteFromXMLCommentsInSkeletonMultipleComments() {
        String skeleton = "blalbala <!-- line 1 -->\n <!-- line 2 -->\n  <!-- line 3 --> blalba";
        MacStringsdictFilter instance = new MacStringsdictFilter();
        String expResult = "line 1 line 2 line 3";
        String result = instance.getNoteFromXMLCommentsInSkeleton(skeleton);
        assertEquals(expResult, result);
    }

    @Test
    public void testGetNoteFromXMLCommentsInSkeletonMultiline() {
        String skeleton = "blalbala <!-- line 1\nline 2 \nline 3 --> blalba";
        MacStringsdictFilter instance = new MacStringsdictFilter();
        String expResult = "line 1\nline 2 \nline 3";
        String result = instance.getNoteFromXMLCommentsInSkeleton(skeleton);
        assertEquals(expResult, result);
    }

    @Test
    public void testGetCldrPluralFormOfEvent() {
        MacStringsdictFilter instance = new MacStringsdictFilter();
        MacStringsdictFilter.MacStringsdictPluralsHolder macStringsdictPluralsHolder = instance.new MacStringsdictPluralsHolder();
        TextUnit textUnit = new TextUnit();
        textUnit.setName("plural_asdf_somestring_one");
        Event event = new Event(EventType.TEXT_UNIT, textUnit );
        assertEquals("one", macStringsdictPluralsHolder.getCldrPluralFormOfEvent(event));
    }
    
}
