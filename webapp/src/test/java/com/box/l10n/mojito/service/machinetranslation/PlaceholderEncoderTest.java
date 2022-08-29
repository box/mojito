package com.box.l10n.mojito.service.machinetranslation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Ignore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class PlaceholderEncoderTest {

  PlaceholderEncoder placeholderEncoder = new PlaceholderEncoder();

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "  \n\t ",
        "test",
        "<<>>",
        "<<",
        "{}",
        "{{}}",
        "&lt;",
        "{{namedPlaceholder}}",
        "<b>bolded</b>",
        "%1s",
        "%@",
        "<tag/>",
        "<tag1>text</tag1>",
        "<tag1>text \t\n text2</tag1>",
        "<tag2",
        "<tag1><tag2></tag1></tag2>"
      })
  void testEncodeDecodeSameAsOriginal(String text) {
    assertEquals(text, placeholderEncoder.decode(placeholderEncoder.encode(text)));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "<span translate=\"no\"></span>",
        "<span translate=\"no\">    </span>",
        "<span translate=\"no\">  text \n \t text2 </span>",
        "<span translate=\"no\">some translation</span>"
      })
  void testDecodeTranslationSpanRemoved(String text) {
    String spanText = "<span translate=\"no\">" + text + "</span>";
    assertEquals(text, placeholderEncoder.decode(spanText));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{}",
        "{{}}",
        "{param}",
        "{{param}}",
        "%s",
        "%@",
        "%d",
        "%,d",
        "$(string)s",
        "%1$@",
        "%1$i",
        "%1$s",
        "%.2f",
        "%1$d"
      })
  void testEncode(String text) {
    String spanText = "<span translate=\"no\">" + text + "</span>";
    assertEquals(spanText, placeholderEncoder.encode(text));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "<span translate=\"no\">text</span>",
        "<span translate=\"no\">{{}}</span>",
        "<span translate=\"no\">{param}</span>",
        "<span translate=\"no\">{{param}}</span>",
        "<span translate=\"no\">%s</span>",
        "<span translate=\"no\">%@</span>",
        "<span translate=\"no\">%d</span>",
        "<span translate=\"no\">%,d</span>",
        "<span translate=\"no\">$(string)s</span>",
        "<span translate=\"no\">%1$@</span>",
        "<span translate=\"no\">%1$i</span>",
        "<span translate=\"no\">%1$i</span>",
        "<span translate=\"no\">1$s</span>",
        "<span translate=\"no\">%.2f</span>",
        "<span translate=\"no\">%1$d</span>"
      })
  void testDecode(String text) {
    assertEquals(
        text.replace("<span translate=\"no\">", "").replaceAll("</span>", ""),
        placeholderEncoder.decode(text));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        // %s
        "%s",

        // %@
        "%@",

        // %,d
        "%,d",

        // "\\{\\}",
        "{}",

        // \{\{\}\}
        "{{}}",

        // %(\d*?)d
        "%d",
        "%d",
        "%12d",

        // \$\(?(.*?)\)?(s|d)
        "$(string)s",
        "$(_string)s",
        "$(count)d",

        // \{\{?[A-Za-z0-9_ .\[\]]+?\}\}?
        "{name}",
        "{{0}}",
        "{{ 0 }}",
        "{users[2].name}",
        "{{ curly Brackets }}",
        "{{curly_Brackets}}",
        "{{curly.Brackets}}",

        // %(\d+)\$(@|i|s)
        "%1$@",
        "%20$@",
        "%4$i",
        "%20$i",
        "%2$s",
        "%20$s",

        // %\.(\d*)f
        "%.f",
        "%.2f",

        // %(\d+)\$,?d
        "%1$d",
        "%1$,d",
      })
  public void testUnifiedPattern(String sourceString) {
    Matcher matcher = placeholderEncoder.getPlaceholderPattern().matcher(sourceString);
    Assertions.assertTrue(matcher.matches());
  }

  @ParameterizedTest
  @ValueSource(strings = {"%(tes", "%d s", "%d and %d images", "%(s", "% in 30 days"})
  public void testDontMatchFullText(String sourceString) {
    Matcher matcher = placeholderEncoder.getPlaceholderPattern().matcher(sourceString);
    Assertions.assertFalse(matcher.matches());
  }

  @ParameterizedTest
  @MethodSource(value = "getSourceStringAndExpectedSingleMatch")
  void testSubstringMatch(
      String sourceString, String expectedFirstMatch, Integer expectedMatchCount) {
    Matcher matcher = placeholderEncoder.getPlaceholderPattern().matcher(sourceString);

    if (expectedMatchCount != null && expectedMatchCount > 0) {
      Assertions.assertTrue(matcher.find());
      Assertions.assertEquals(expectedFirstMatch, matcher.group());

      int count = 1;
      while (matcher.find()) {
        count++;
      }
      Assertions.assertEquals(expectedMatchCount.intValue(), count);
    } else {
      Assertions.assertFalse(matcher.find());
    }
  }

  private static Stream<Arguments> getSourceStringAndExpectedSingleMatch() {
    return Stream.of(
        arguments("a test %s s d", "%s", 1),
        arguments("a test %ssd", "%s", 1),
        arguments("another test %ds", "%d", 1),
        arguments("select $(abcs)d", "$(abcs)d", 1),
        arguments("string %d s string", "%d", 1),
        arguments("%d and %d img", "%d", 2),
        arguments("test %(settings)s sd", "%(settings)s", 1),
        arguments("% more values", "", 0),
        arguments("% increase", "", 0),
        arguments("is up 10%. And starting", "", 0),
        arguments("up 10% in 10 days", "", 0),
        arguments("% of something", "", 0),
        arguments("{name}{name2}", "{name}", 2),
        arguments("{{name}}{{name2}}", "{{name}}", 2),
        arguments("test %@a@ b@", "%@", 1),
        arguments("test 1$@ %1$@1$@a", "%1$@", 1),
        arguments("$usd $(strings)sss $s", "$(strings)s", 1),
        arguments("$usd $(counted)dddd $d", "$(counted)d", 1),
        arguments("%f %increase %.f % f % . f", "%.f", 1),
        arguments("$usd %100 %1$d % 100 $d", "%1$d", 1),
        arguments("$usd %100 %1$,d % 100 $d", "%1$,d", 1),
        arguments("$'{}': {}", "{}", 2),
        arguments("{  a {{'{{text}}'}} b }", "{{text}}", 1),
        arguments("get {{ '{{number}}' }} more", "{{number}}", 1));
  }

  @Test
  @Ignore
  public void testPrintEscapedRegex() {
    System.out.println("Raw Regex pattern:");
    System.out.println(
        Arrays.stream(PlaceholderPatternType.values())
            .map(PlaceholderPatternType::getValue)
            .collect(Collectors.joining("|"))
            .replaceAll(",", "\\x{002C}"));
  }
}
