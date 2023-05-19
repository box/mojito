package com.box.l10n.mojito.cli.command.checks;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CheckerUtilsTest {

  @Test
  void weShouldBeAbleTo_getWordsInString_providingASingleWordAsInput() {
    List<String> result = CheckerUtils.getWordsInString("Hello");
    assertThat(result).containsExactly("Hello");
  }

  @Test
  void weShouldBeAbleTo_getWordsInString_providingMultipleWordsAsInput() {
    List<String> result = CheckerUtils.getWordsInString("Hello, how can I help you today?");
    assertThat(result).containsExactly("Hello", "how", "can", "I", "help", "you", "today");
  }

  @Test
  void weShouldBeAbleTo_getWordsInString_ignoringHtmlTag() {
    List<String> result =
        CheckerUtils.getWordsInString("This is a string containing html tag <br>");
    assertThat(result).containsExactly("This", "is", "a", "string", "containing", "html", "tag");
  }

  @Test
  void weShouldBeAbleTo_getWordsInString_withoutIgnoringTextBetweenHtmlTags() {
    List<String> result =
        CheckerUtils.getWordsInString("This string has a bold html tag <b>Text</b>");
    assertThat(result).containsExactly("This", "string", "has", "a", "bold", "html", "tag", "Text");
  }

  @Test
  void weShouldBeAbleTo_getWordsInString_ignoringMultipleHtmlTags() {
    List<String> result =
        CheckerUtils.getWordsInString(
            "<br>This text </br> have <html> elements <p> for testing </p> purposes<b>.");
    assertThat(result)
        .containsExactly("This", "text", "have", "elements", "for", "testing", "purposes");
  }

  @Test
  void weShouldBeAbleTo_getWordsInString_ignoringTextInsideHtmlTags() {
    List<String> result =
        CheckerUtils.getWordsInString(
            "Text inside html tags should be ignored <img src=\"example.jpg\">.");
    assertThat(result).containsExactly("Text", "inside", "html", "tags", "should", "be", "ignored");
  }

  @Test
  void weShouldNotThrowExceptionWhen_getWordsInString_providingEmptyStringAsInput() {
    List<String> result = CheckerUtils.getWordsInString("");
    assertThat(result).isEmpty();
  }

  @Test
  void weShouldNotThrowExceptionWhen_getWordsInString_providingNullAsInput() {
    List<String> result = CheckerUtils.getWordsInString(null);
    assertThat(result).isEmpty();
  }
}
