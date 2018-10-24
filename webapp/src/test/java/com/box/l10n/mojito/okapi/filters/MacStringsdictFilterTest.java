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
 * @author jeanaurambault
 */
public class MacStringsdictFilterTest {

    @Test
    public void testGetCldrPluralFormOfEvent() {
        MacStringsdictFilter instance = new MacStringsdictFilter();
        MacStringsdictFilter.MacStringsdictPluralsHolder macStringsdictPluralsHolder = instance.new MacStringsdictPluralsHolder();
        TextUnit textUnit = new TextUnit();
        textUnit.setName("plural_asdf_somestring_one");
        Event event = new Event(EventType.TEXT_UNIT, textUnit );
        assertEquals("one", macStringsdictPluralsHolder.getCldrPluralFormOfEvent(event));
    }

    @Test
    public void getUsagesFromSkeleton() {
        String textUnitText = "<!-- Comments -->\n"
                + "<!-- Location: path/to/file.java:49 -->\n"
                + "<!-- Location: path/to/file.java:72 -->\n";

        MacStringsdictFilter instance = new MacStringsdictFilter();
        instance.getNoteAndLocationFromEvents(textUnitText);


        Set<String> expectedUsages = new HashSet<>();
        expectedUsages.add("path/to/file.java:49");
        expectedUsages.add("path/to/file.java:72");
        assertEquals(2, instance.usages.size());
        assertEquals(expectedUsages, instance.usages);
    }

    @Test
    public void getUsagesFromSkeletonNone() {
        String skeleton = "<!-- Comments -->\n";

        MacStringsdictFilter instance = new MacStringsdictFilter();
        instance.getNoteAndLocationFromEvents(skeleton);
        assertNull(instance.usages);
    }
}
