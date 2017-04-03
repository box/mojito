package com.box.l10n.mojito.cli.pseudoloc;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 */
public class PseudolocalizationTest {

	@Test
	public void testExpandExpandsString() {
		String expanded = PseudoLocalization.expand("English Sentence");
		assertEquals("The strings should be equal", "萬 English Sentence 國", expanded);
	}

	@Test
	public void testStringIsConvertedToDiacritics() {
		String diacriticsString = PseudoLocalization.convertAsciiToDiacritics("English Sentence");
		assertNotEquals("The string should be converted to diacritics ", "English Sentence", diacriticsString);
	}

	@Test
	public void testStringIsNotConvertedToDiacritics() {
		// The chars q, Q, and V are not converted so they should not be converted
		// when we call the convert function
		String diacriticsString = PseudoLocalization.convertAsciiToDiacritics("qQV");
		assertEquals("The strings should remain the same", "qQV", diacriticsString);
	}

	@Test
	public void testconvertStringToPseudoLoc() {
		String pseudoLocalized = PseudoLocalization.convertStringToPseudoLoc("English Sentence");
		assertNotEquals("The string should be pseudolocalized", "English Sentence", pseudoLocalized);
	}

	@Test
	public void testconvertStringToPseudoLocDoesNot() {
		String pseudoLocalized = PseudoLocalization.convertStringToPseudoLoc("");
		assertEquals("The strings should be equal", "", pseudoLocalized);
	}
}
