package com.box.l10n.mojito.cli.command.checks;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckerUtilsTest {

  @Test
  void weShouldBeAbleTo_getWordsInString_providingASingleWordAsInput() {
    List<String> result = CheckerUtils.getWordsInString("Hello");
    assertEquals(1, result.size());
    assertTrue(result.contains("Hello"));
  }

  @Test
  void weShouldBeAbleTo_getWordsInString_providingMultipleStringsAsInput() {
    List<String> result = CheckerUtils.getWordsInString("Hello, how can I help you today?");
    assertEquals(7, result.size());
    assertTrue(result.contains("Hello"));
    assertTrue(result.contains("how"));
    assertTrue(result.contains("can"));
    assertTrue(result.contains("I"));
    assertTrue(result.contains("help"));
    assertTrue(result.contains("you"));
    assertTrue(result.contains("today"));
  }

  @Test
  void weShouldBeAbleTo_getWordsInString_ignoringHtmlTag() {
    List<String> result =
        CheckerUtils.getWordsInString("This is a string containing html tag <br>");
    assertEquals(7, result.size());
    assertFalse(result.contains("<br>"));
    assertFalse(result.contains("br"));
  }

  @Test
  void weShouldBeAbleTo_getWordsInString_withoutIgnoringTextBetweenHtmlTags() {
    List<String> result =
        CheckerUtils.getWordsInString("This string has a bold html tag <b>Text</b>");
    assertEquals(8, result.size());
    assertFalse(result.contains("<b>"));
    assertFalse(result.contains("</b>"));
    assertFalse(result.contains("b"));
    assertTrue(result.contains("Text"));
  }

  @Test
  void weShouldBeAbleTo_getWordsInString_ignoringMultipleHtmlTags() {
    List<String> result =
        CheckerUtils.getWordsInString(
            "<br>This text </br> have <html> elements <p> for testing </p> purposes<b>.");

    assertEquals(7, result.size());

    assertTrue(result.contains("This"));
    assertTrue(result.contains("text"));
    assertTrue(result.contains("have"));
    assertTrue(result.contains("elements"));
    assertTrue(result.contains("for"));
    assertTrue(result.contains("testing"));
    assertTrue(result.contains("purposes"));
  }

  @Test
  void weShouldBeAbleTo_getWordsInString_ignoringTextInsideHtmlTags() {
    List<String> result =
        CheckerUtils.getWordsInString(
            "Text inside html tags should be ignored <img src=\"example.jpg\">.");

    assertEquals(7, result.size());

    assertTrue(result.contains("Text"));
    assertTrue(result.contains("inside"));
    assertTrue(result.contains("html"));
    assertTrue(result.contains("tags"));
    assertTrue(result.contains("should"));
    assertTrue(result.contains("be"));
    assertTrue(result.contains("ignored"));
  }

  @Test
  void weShouldNotThrowExceptionWhen_getWordsInString_providingEmptyStringAsInput() {
    List<String> result = CheckerUtils.getWordsInString("");
    assertEquals(0, result.size());
  }

  @Test
  void weShouldNotThrowExceptionWhen_getWordsInString_providingNullAsInput() {
    List<String> result = CheckerUtils.getWordsInString(null);
    assertEquals(0, result.size());
  }
}
