package com.box.l10n.mojito.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;

public class PlaceholderRegexTest {
    public String commaUnicodeCodePoint = "\\x{002C}"; //  \x{002C} == ,
    public String OLD_PLACEHOLDER_REGEX_STRING = "\\{\\{\\}\\}|\\{\\{?.+?\\}\\}?|\\%\\%\\(.+?\\)s|\\%\\(.+?\\)s|\\%\\(.+?\\)d|\\%\\%s|\\%s";
    public String PLACEHOLDER_REGEX_STRING =
            String.join("|",
                    "%s",
                    "%@",
                    "%"+commaUnicodeCodePoint+"d",
                    "\\{\\}",
                    "\\{\\{\\}\\}",
                    "%(\\d*?)d",
                    "([$%])\\(+(.*?)\\)+(s|d)",
                    "\\{\\{?[A-Za-z0-9_ .\\[\\]]+?\\}\\}?",
                    "%(\\d+)\\$(@|i|s)",
                    "%\\.(\\d*)f",
                    "%(\\d+)\\$"+commaUnicodeCodePoint+"?d"
            );
    public Pattern PLACEHOLDER_REGEX_PATTERN = Pattern.compile(PLACEHOLDER_REGEX_STRING);

    @ParameterizedTest
    @ValueSource(strings = {
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
            "{{curlyBrackets}}",
            "{{ curlyBrackets}}",
            "{{curlyBrackets }}",
            "{{ curlyBrackets }}",
            "{{ curly Brackets }}",
            "{{curly_Brackets}}",
            "{{curly.Brackets}}",

            // %(\d+)\$(@|i|s)
            "%1$@",
            "%2$@",
            "%3$@",
            "%4$@",
            "%20$@",
            "%1$i",
            "%2$i",
            "%3$i",
            "%4$i",
            "%20$i",
            "%1$s",
            "%2$s",
            "%3$s",
            "%4$s",
            "%20$s",

            // %\.(\d*)f
            "%.f",
            "%.2f",

            // %(\d+)\$,?d
            "%1$d",
            "%1$,d",
    })
    public void testMatchFullText(String sourceString) {
        Matcher matcher = PLACEHOLDER_REGEX_PATTERN.matcher(sourceString);
        Assertions.assertTrue(matcher.matches());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "%(tes",
            "%d s",
            "%d and %d images",
            "%(s",
            "% in 30 days"
    })
    public void testDontMatchFullText(String sourceString) {
        Matcher matcher = PLACEHOLDER_REGEX_PATTERN.matcher(sourceString);
        Assertions.assertFalse(matcher.matches());
    }

    @ParameterizedTest
    @MethodSource(value = "getSourceStringAndExpectedSingleMatch")
    void testSubstringMatch(String sourceString, String expectedFirstMatch, Integer expectedMatchCount) {
        Matcher matcher = PLACEHOLDER_REGEX_PATTERN.matcher(sourceString);

        if( expectedMatchCount != null && expectedMatchCount > 0) {
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
                arguments("get {{ '{{number}}' }} more", "{{number}}", 1)
        );
    }

    @Test
    public void testPrintRegex() {
        System.out.println("Raw Regex pattern:");
        System.out.println(PLACEHOLDER_REGEX_STRING);
    }
}
