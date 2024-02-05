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
  void weShouldBeAbleTo_getWordsInString_andNotIgnoreTitleAttValue() {
    List<String> result =
        CheckerUtils.getWordsInString(
            "Text inside html tags should be ignored  <comp src=\"some.jpg\" alt=\"TitleAtt Content\">, but title");
    assertThat(result)
        .containsExactly(
            "Text",
            "inside",
            "html",
            "tags",
            "should",
            "be",
            "ignored",
            "but",
            "title",
            "TitleAtt",
            "Content");
  }

  @Test
  void weShouldBeAbleTo_getWordsInString_andNotIgnoreAltAttValue() {
    List<String> result =
        CheckerUtils.getWordsInString(
            "Text inside html tags should be ignored  <comp src=\"some.jpg\" alt=\"AltAtt Content\">, but alt");
    assertThat(result)
        .containsExactly(
            "Text", "inside", "html", "tags", "should", "be", "ignored", "but", "alt", "AltAtt",
            "Content");
  }

  @Test
  void weShouldBeAbleTo_getWordsInString_andNotIgnoreValueAttValue() {
    List<String> result =
        CheckerUtils.getWordsInString(
            "Text inside html tags should be ignored  <comp src=\"some.jpg\" value=\"ValueAtt Content\">, but value");
    assertThat(result)
        .containsExactly(
            "Text",
            "inside",
            "html",
            "tags",
            "should",
            "be",
            "ignored",
            "but",
            "value",
            "ValueAtt",
            "Content");
  }

  @Test
  void weShouldBeAbleTo_getWordsInString_andNotIgnorePlaceholderAttValue() {
    List<String> result =
        CheckerUtils.getWordsInString(
            "Text inside html tags should be ignored  <comp src=\"some.jpg\" placeholder=\"PlaceholderAtt Content\">, but placeholder");
    assertThat(result)
        .containsExactly(
            "Text",
            "inside",
            "html",
            "tags",
            "should",
            "be",
            "ignored",
            "but",
            "placeholder",
            "PlaceholderAtt",
            "Content");
  }

  @Test
  void weShouldBeAbleTo_getWordsInString_andIgnoreUnwantedHtmlAttributes() {
    List<String> result =
        CheckerUtils.getWordsInString(
            "Text inside html tags should be ignored <comp src=\"some.jpg\" att=\"AnyAtt Content\" another=\"Another Content\">");
    assertThat(result).containsExactly("Text", "inside", "html", "tags", "should", "be", "ignored");
  }

  @Test
  void weShouldBeAbleTo_getWordsInString_whenThereAreNoSpacesAmongHtmlTags() {
    List<String> result = CheckerUtils.getWordsInString("<b>These are different</b><br>Words");
    assertThat(result).containsExactly("These", "are", "different", "Words");
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

  @Test
  void weShouldBeAbleTo_getWordsInString_andIgnorePlaceholderInsideCurlyBraces() {
    List<String> result =
        CheckerUtils.getWordsInString(
            "Placeholder inside curly braces should be ignored {some_placeholder}");
    assertThat(result)
        .containsExactly("Placeholder", "inside", "curly", "braces", "should", "be", "ignored");
  }

  @Test
  void weShouldBeAbleTo_getWordsInString_andIgnorePlaceholdersInsideCurlyBraces() {
    List<String> result =
        CheckerUtils.getWordsInString(
            "{a_placeholder} inside curly braces should be ignored. Also {another} should be ignored");
    assertThat(result)
        .containsExactly(
            "inside", "curly", "braces", "should", "be", "ignored", "Also", "should", "be",
            "ignored");
  }

  @Test
  void weShouldBeAbleTo_getWordsInString_andIgnoreEmailAddress() {
    List<String> result = CheckerUtils.getWordsInString("Email any@email.com should be ignored");
    assertThat(result).containsExactly("Email", "should", "be", "ignored");
  }

  @Test
  void weShouldBeAbleTo_getWordsInString_andIgnoreEmailAddresses() {
    List<String> result =
        CheckerUtils.getWordsInString(
            "Email any@email.com should be ignored. Also, another@org.com should be ignored");
    assertThat(result)
        .containsExactly("Email", "should", "be", "ignored", "Also", "should", "be", "ignored");
  }
}
