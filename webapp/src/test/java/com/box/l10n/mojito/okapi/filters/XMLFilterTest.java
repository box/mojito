package com.box.l10n.mojito.okapi.filters;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author jeanaurambault
 */
public class XMLFilterTest {

    @Test
    public void testGetNoteFromXMLCommentsInSkeletonNoComment() {
        String skeleton = "no comments";
        XMLFilter instance = new XMLFilter();
        String expResult = null;
        String result = instance.getNoteFromXMLCommentsInSkeleton(skeleton);
        assertEquals(expResult, result);
    }

    @Test
    public void testGetNoteFromXMLCommentsInSkeletonComment() {
        String skeleton = "blalbala <!-- a comment --> blalba";
        XMLFilter instance = new XMLFilter();
        String expResult = "a comment";
        String result = instance.getNoteFromXMLCommentsInSkeleton(skeleton);
        assertEquals(expResult, result);
    }

    @Test
    public void testGetNoteFromXMLCommentsInSkeletonMultiline() {
        String skeleton = "blalbala <!-- line 1 --> <!-- line 2 -->  <!-- line 3 --> blalba";
        XMLFilter instance = new XMLFilter();
        String expResult = "line 1 line 2 line 3";
        String result = instance.getNoteFromXMLCommentsInSkeleton(skeleton);
        assertEquals(expResult, result);
    }

}
