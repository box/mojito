package com.box.l10n.mojito.cli.command.checks;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class CheckerUtilsTest {

  @Test
  void weShouldBeAbleTo_getWordsInString_providingASingleWordAsInput() {
    List<String> result = CheckerUtils.getWordsInString("Hello");
    List<String> expectedResult = Collections.singletonList("Hello");
    assertEquals(expectedResult, result);
  }

  @Test
  void weShouldBeAbleTo_getWordsInString_providingMultipleWordsAsInput() {
    List<String> result = CheckerUtils.getWordsInString("Hello, how can I help you today?");
    List<String> expectedResult = Arrays.asList("Hello", "how", "can", "I", "help", "you", "today");
    assertEquals(expectedResult, result);
  }

  @Test
  void weShouldBeAbleTo_getWordsInString_ignoringHtmlTag() {
    List<String> result =
        CheckerUtils.getWordsInString("This is a string containing html tag <br>");
    List<String> expectedResult =
        Arrays.asList("This", "is", "a", "string", "containing", "html", "tag");
    assertEquals(expectedResult, result);
  }

  @Test
  void weShouldBeAbleTo_getWordsInString_withoutIgnoringTextBetweenHtmlTags() {
    List<String> result =
        CheckerUtils.getWordsInString("This string has a bold html tag <b>Text</b>");
    List<String> expectedResult =
        Arrays.asList("This", "string", "has", "a", "bold", "html", "tag", "Text");
    assertEquals(expectedResult, result);
  }

  @Test
  void weShouldBeAbleTo_getWordsInString_ignoringMultipleHtmlTags() {
    List<String> result =
        CheckerUtils.getWordsInString(
            "<br>This text </br> have <html> elements <p> for testing </p> purposes<b>.");

    List<String> expectedResult =
        Arrays.asList("This", "text", "have", "elements", "for", "testing", "purposes");
    assertEquals(expectedResult, result);
  }

  @Test
  void weShouldBeAbleTo_getWordsInString_ignoringTextInsideHtmlTags() {
    List<String> result =
        CheckerUtils.getWordsInString(
            "Text inside html tags should be ignored <img src=\"example.jpg\">.");
    List<String> expectedResult =
        Arrays.asList("Text", "inside", "html", "tags", "should", "be", "ignored");
    assertEquals(expectedResult, result);
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
