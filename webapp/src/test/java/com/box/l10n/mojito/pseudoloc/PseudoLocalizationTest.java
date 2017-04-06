package com.box.l10n.mojito.pseudoloc;

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
}
