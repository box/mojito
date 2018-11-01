package com.box.l10n.mojito.pseudoloc;

import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.MessageFormatIntegrityChecker;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.TextUnitIntegrityChecker;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.WhitespaceIntegrityChecker;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 */
public class PseudoLocalizationTest {

    @Test
    public void testExpandExpandsString() {
        PseudoLocalization ps = new PseudoLocalization();
        String expanded = ps.expand("English Sentence");
        assertEquals("The strings should be equal", "萬萬萬萬 English Sentence 國國國國", expanded);
    }

    @Test
    public void testStringIsConvertedToDiacritics() {
        PseudoLocalization ps = new PseudoLocalization();
        String diacriticsString = ps.convertAsciiToDiacritics("English Sentence");
        assertNotEquals("The string should be converted to diacritics", "English Sentence", diacriticsString);
    }

    @Test
    public void testStringIsNotConvertedToDiacritics() {
        // The chars q, Q, and V are not converted so they should not be converted
        // when we call the convert function
        PseudoLocalization ps = new PseudoLocalization();
        String diacriticsString = ps.convertAsciiToDiacritics("qQV");
        assertEquals("The strings should remain the same", "qQV", diacriticsString);
    }

    @Test
    public void testconvertStringToPseudoLoc() {
        PseudoLocalization ps = new PseudoLocalization();
        String pseudoLocalized = ps.convertStringToPseudoLoc("English Sentence");
        assertNotEquals("The string should be pseudolocalized", "English Sentence", pseudoLocalized);
    }

    @Test
    public void testconvertStringToPseudoLocDoesNot() {
        PseudoLocalization ps = new PseudoLocalization();
        String pseudoLocalized = ps.convertStringToPseudoLoc("");
        assertEquals("The strings should be equal", "", pseudoLocalized);
    }

    @Test
    public void testConvertStringToPseudoLocWithNoIntegrityChecker() {
        PseudoLocalization ps = new PseudoLocalization();
        Set<TextUnitIntegrityChecker> checkers = new HashSet<>();
        String pseudoLocalized = ps.convertStringToPseudoLoc("vf", checkers);
        assertEquals("The strings should be equal", "⟦ νƒ ⟧", pseudoLocalized);
    }

    @Test
    public void testConvertStringToPseudoLocWithMultipleIntegrityCheckers() {
        PseudoLocalization ps = new PseudoLocalization();
        Set<TextUnitIntegrityChecker> checkers = new HashSet<>();
        checkers.add(new WhitespaceIntegrityChecker());
        checkers.add(new MessageFormatIntegrityChecker());
        String pseudoLocalized = ps.convertStringToPseudoLoc("vf", checkers);
        assertEquals("The strings should be equal", "⟦ νƒ ⟧", pseudoLocalized);
    }

    @Test
    public void testConvertMessageFormatStringToPseudoLoc() {
        PseudoLocalization ps = new PseudoLocalization();
        Set<TextUnitIntegrityChecker> checkers = new HashSet<>();
        checkers.add(new MessageFormatIntegrityChecker());
        String pseudoLocalized = ps.convertStringToPseudoLoc("English Sentence with {placeholder1} and {placeholder2} which should not be pseudolocalized", checkers);
        assertTrue("The placeholder should not be pseudolocalized", pseudoLocalized.contains("{placeholder1}"));
        assertTrue("The placeholder should not be pseudolocalized", pseudoLocalized.contains("{placeholder2}"));
    }

    @Test
    public void testConvertPluralMessageFormatStringToPseudoLoc1() {
        PseudoLocalization ps = new PseudoLocalization();
        Set<TextUnitIntegrityChecker> checkers = new HashSet<>();
        checkers.add(new MessageFormatIntegrityChecker());
        // v and f map to single unicode character in pseudolocalization which makes it easier to test and verify
        String pseudoLocalized = ps.convertStringToPseudoLoc("Test {count, plural, one {# v} other {# vf}} with Plurals", checkers);
        assertTrue("The plural syntax should not be pseudolocalized", pseudoLocalized.contains("count, plural, one"));
        assertTrue("The plural syntax should not be pseudolocalized", pseudoLocalized.contains("other"));
        assertTrue("The plural text variation should be pseudolocalized", pseudoLocalized.contains("{# ν}"));
        assertTrue("The plural text variation should be pseudolocalized", pseudoLocalized.contains("{# νƒ}"));
    }

    @Test
    public void testConvertPluralMessageFormatStringToPseudoLoc2() {
        PseudoLocalization ps = new PseudoLocalization();
        Set<TextUnitIntegrityChecker> checkers = new HashSet<>();
        checkers.add(new MessageFormatIntegrityChecker());
        // v and f map to single unicode character in pseudolocalization which makes it easier to test and verify
        String pseudoLocalized = ps.convertStringToPseudoLoc("Viewed by {numUsers, plural, one {1 v} other {{numUsers} vf}} and others", checkers);
        assertFalse("The string should be pseudolocalized", pseudoLocalized.contains("Viewed by"));
        assertTrue("The plural syntax should not be pseudolocalized", pseudoLocalized.contains("numUsers, plural, one"));
        assertTrue("The plural syntax should not be pseudolocalized", pseudoLocalized.contains("other"));
        assertTrue("The plural text variation should be pseudolocalized", pseudoLocalized.contains("{1 ν}"));
        assertTrue("The plural text variation should be pseudolocalized while the placeholder should not", pseudoLocalized.contains("{{numUsers} νƒ}"));
    }
}
