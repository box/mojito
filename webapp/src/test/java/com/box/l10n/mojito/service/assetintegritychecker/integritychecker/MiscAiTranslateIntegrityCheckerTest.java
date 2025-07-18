package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import static org.junit.jupiter.api.Assertions.*;

import java.text.Normalizer;
import org.junit.jupiter.api.Test;

public class MiscAiTranslateIntegrityCheckerTest {

  MiscAiTranslateIntegrityChecker checker = new MiscAiTranslateIntegrityChecker();

  @Test
  void testHashtagValidCases() {
    assertDoesNotThrow(() -> checker.checkSingleHashtag("#hello", "#hola"));
    assertDoesNotThrow(() -> checker.checkSingleHashtag("#welcome", "#bienvenido"));
    assertDoesNotThrow(() -> checker.checkSingleHashtag("#goodmorning", "#gutenmorgen"));
    assertDoesNotThrow(() -> checker.checkSingleHashtag("#bonjour-2024", "#bonjour-2024"));
    assertDoesNotThrow(() -> checker.checkSingleHashtag("#こんにちは", "#おはよう"));
    assertDoesNotThrow(() -> checker.checkSingleHashtag("Hello world", "#hola"));
    assertDoesNotThrow(
        () ->
            checker.checkSingleHashtag(
                "#hello-world", Normalizer.normalize("#हेलो-वर्ल्ड", Normalizer.Form.NFC)));
  }

  @Test
  void testHashtagInvalidCases() {
    IntegrityCheckException ex;

    ex =
        assertThrows(
            IntegrityCheckException.class,
            () -> checker.checkSingleHashtag("#hello", "hola")); // Missing #
    assertTrue(
        ex.getMessage()
            .contains("Source is a single hashtag, but the target is not a valid hashtag"));

    ex =
        assertThrows(
            IntegrityCheckException.class,
            () -> checker.checkSingleHashtag("#hello", "#hola mundo")); // Space
    assertTrue(
        ex.getMessage()
            .contains("Source is a single hashtag, but the target is not a valid hashtag"));

    ex =
        assertThrows(
            IntegrityCheckException.class,
            () -> checker.checkSingleHashtag("#hello", "#")); // Too short
    assertTrue(
        ex.getMessage()
            .contains("Source is a single hashtag, but the target is not a valid hashtag"));
  }

  @Test
  void testNoCheckIfSourceNotHashtag() {
    assertDoesNotThrow(() -> checker.checkSingleHashtag("NotAHashtag", "randomoutput"));
    assertDoesNotThrow(() -> checker.checkSingleHashtag("Section for debugger", "randomoutput"));
  }
}
