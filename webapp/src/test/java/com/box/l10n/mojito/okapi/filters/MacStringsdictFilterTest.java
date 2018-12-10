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

    @Test
    public void getUsagesFromSkeleton() {
        String skeleton = "<!-- Comments -->\n"
                + "<!-- Location: path/to/file.java:49 -->\n"
                + "<!-- Location: path/to/file.java:72 -->\n"
                + "<key>plural_recipe_cook_hours</key>\n"
                + "<dict>\n"
                + "    <key>NSStringLocalizedFormatKey</key>\n"
                + "    <string>%#@hours@ to cook</string>\n"
                + "    <key>hours</key>\n"
                + "    <dict>\n"
                + "        <key>NSStringFormatSpecTypeKey</key>\n"
                + "        <string>NSStringPluralRuleType</string>\n"
                + "        <key>NSStringFormatValueTypeKey</key>\n"
                + "        <string>d</string>\n"
                + "        <key>one</key>\n"
                + "        <string>%d hour to cook</string>\n"
                + "        <key>other</key>\n"
                + "        <string>%d hours to cook</string>\n"
                + "    </dict>\n"
                + "</dict>\n";

        MacStringsdictFilter instance = new MacStringsdictFilter();
        Set<String> usages = instance.getLocationsFromXMLCommentsInSkeleton(skeleton);

        Set<String> expectedUsages = new HashSet<>();
        expectedUsages.add("path/to/file.java:49");
        expectedUsages.add("path/to/file.java:72");
        assertEquals(expectedUsages, usages);
    }

    @Test
    public void getUsagesFromSkeletonNone() {
        String skeleton = "<!-- Comments -->\n"
                + "<key>plural_recipe_cook_hours</key>\n"
                + "<dict>\n"
                + "    <key>NSStringLocalizedFormatKey</key>\n"
                + "    <string>%#@hours@ to cook</string>\n"
                + "    <key>hours</key>\n"
                + "    <dict>\n"
                + "        <key>NSStringFormatSpecTypeKey</key>\n"
                + "        <string>NSStringPluralRuleType</string>\n"
                + "        <key>NSStringFormatValueTypeKey</key>\n"
                + "        <string>d</string>\n"
                + "        <key>one</key>\n"
                + "        <string>%d hour to cook</string>\n"
                + "        <key>other</key>\n"
                + "        <string>%d hours to cook</string>\n"
                + "    </dict>\n"
                + "</dict>\n";

        MacStringsdictFilter instance = new MacStringsdictFilter();
        Set<String> usages = instance.getLocationsFromXMLCommentsInSkeleton(skeleton);
        Set<String> expectedUsages = new HashSet<>();
        assertEquals(expectedUsages, usages);
    }
}
