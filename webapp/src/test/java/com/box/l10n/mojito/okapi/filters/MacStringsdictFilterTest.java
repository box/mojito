package com.box.l10n.mojito.okapi.filters;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MacStringsdictFilterTest {

    @Test
    public void testGetNoteFromXMLCommentsInSkeletonNoComment() {
        String skeleton = "no comments";
        MacStringsdictFilter instance = new MacStringsdictFilter();
        String expResult = null;
        String result = instance.getNoteFromXMLCommentsInSkeleton(skeleton);
        assertEquals(expResult, result);
    }
}


