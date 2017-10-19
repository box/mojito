package com.box.l10n.mojito.okapi.filters;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.resource.TextUnit;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author jeanaurambault
 */
public class AndroidFilterTest {

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
        String skeleton = "blalbala <!-- line 1 --> <!-- line 2 -->  <!-- line 3 --> blalba";
        AndroidFilter instance = new AndroidFilter();
        String expResult = "line 1 line 2 line 3";
        String result = instance.getNoteFromXMLCommentsInSkeleton(skeleton);
        assertEquals(expResult, result);
    }

    @Test
    public void testGetCldrPluralFormOfEvent() {
        AndroidFilter instance = new AndroidFilter();
        AndroidFilter.AndroidPluralsHolder androidPluralsHolder = instance.new AndroidPluralsHolder();
        TextUnit textUnit = new TextUnit();
        textUnit.setName("plural_asdf_somestring_one");
        Event event = new Event(EventType.TEXT_UNIT, textUnit ); 
        assertEquals("one", androidPluralsHolder.getCldrPluralFormOfEvent(event));
    }

}
