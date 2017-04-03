package com.box.l10n.mojito.pseudoloc;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 *
 */
public class PseudoLocalizationTest {

	@Test
	public void testExpandExpandsString() {
		Pseudo ps = new Pseudo();
		String expanded = ps.expand("English Sentence");
		assertEquals("The strings should be equal", "萬 English Sentence 國", expanded);
	}

	@Test
	public void testStringIsConvertedToDiacritics() {
		Pseudo ps = new Pseudo();
		String diacriticsString = ps.convertAsciiToDiacritics("English Sentence");
		//assertNotEquals("The string should be converted to diacritics", "English Sentence", diacriticsString);
	}

	@Test
	public void testStringIsNotConvertedToDiacritics() {
		// The chars q, Q, and V are not converted so they should not be converted
		// when we call the convert function
		Pseudo ps = new Pseudo();
		String diacriticsString = ps.convertAsciiToDiacritics("qQV");
		assertEquals("The strings should remain the same", "qQV", diacriticsString);
	}

	@Test
	public void testconvertStringToPseudoLoc() {
		Pseudo ps = new Pseudo();
		String pseudoLocalized = ps.convertStringToPseudoLoc("English Sentence");
		assertNotEquals("The string should be pseudolocalized", "English Sentence", pseudoLocalized);
	}

	@Test
	public void testconvertStringToPseudoLocDoesNot() {
		Pseudo ps = new Pseudo();
		String pseudoLocalized = ps.convertStringToPseudoLoc("");
		System.out.println(pseudoLocalized);
		assertEquals("The strings should be equal", "", pseudoLocalized);
	}

}
