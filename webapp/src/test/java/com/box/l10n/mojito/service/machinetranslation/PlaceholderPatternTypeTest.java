package com.box.l10n.mojito.service.machinetranslation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderPatternTypeTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "%s",
            "%s ",
            " %s ",
            " test %s test",
            " test \n\t %s "
    })
    public void testGettextStringPattern(String text) {
        Pattern pattern = Pattern.compile(PlaceholderPatternType.GETTEXT_STRING.getValue());
        Matcher matcher = pattern.matcher(text);
        Assertions.assertTrue(matcher.find());
        Assertions.assertEquals("%s", matcher.group());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "%@",
            "%@ ",
            " %@ ",
            " test %@ test",
            " test \n\t %@ "
    })
    public void testIOSStringPattern(String text) {
        Pattern pattern = Pattern.compile(PlaceholderPatternType.IOS_STRING.getValue());
        Matcher matcher = pattern.matcher(text);
        Assertions.assertTrue(matcher.find());
        Assertions.assertEquals("%@", matcher.group());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "%,d",
            "%,d ",
            " %,d ",
            " test %,d test",
            " test \n\t %,d \n\t test "
    })
    public void testGetTextNumberPattern(String text) {
        Pattern pattern = Pattern.compile(PlaceholderPatternType.GETTEXT_NUMBER.getValue());
        Matcher matcher = pattern.matcher(text);
        Assertions.assertTrue(matcher.find());
        Assertions.assertEquals("%,d", matcher.group());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{} ",
            " {} ",
            " test {} test",
            " test \n\t {} \n\t test "
    })
    public void testMessageFormatEmptyPattern(String text) {
        Pattern pattern = Pattern.compile(PlaceholderPatternType.MESSAGE_FORMAT_EMPTY.getValue());
        Matcher matcher = pattern.matcher(text);
        Assertions.assertTrue(matcher.find());
        Assertions.assertEquals("{}", matcher.group());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{{}}",
            "{{}} ",
            " {{}} ",
            " test {{}} test",
            " test \n\t {{}} \n\t test "
    })
    public void testMessageFormatDoubleEmptyPattern(String text) {
        Pattern pattern = Pattern.compile(PlaceholderPatternType.MESSAGE_FORMAT_DOUBLE_EMPTY.getValue());
        Matcher matcher = pattern.matcher(text);
        Assertions.assertTrue(matcher.find());
        Assertions.assertEquals("{{}}", matcher.group());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "%d",
            "%d numbers",
            " %1d first placeholder",
            " test %2d test",
            " test \n\t  %20d \n\t test "
    })
    public void testGetTextPositionalNumberPattern(String text) {
        Pattern pattern = Pattern.compile(PlaceholderPatternType.GETTEXT_POSITIONAL_NUMBER.getValue());
        Matcher matcher = pattern.matcher(text);
        Assertions.assertTrue(matcher.find());
        Assertions.assertTrue(matcher.group().startsWith("%"));
        Assertions.assertTrue(matcher.group().endsWith("d"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "%(text)s",
            "%(number)d",
            "$(text)s",
            "$(number)d",
            "first named %(text)s placeholder",
            "first named %(number)d placeholder",
            " test \n\t %(text)s \n\t test ",
            " test \n\t %(number)d \n\t test "
    })
    public void testGetTextNamedPattern(String text) {
        Pattern pattern = Pattern.compile(PlaceholderPatternType.GETTEXT_NAMED.getValue());
        Matcher matcher = pattern.matcher(text);
        Assertions.assertTrue(matcher.find());
        Assertions.assertTrue(matcher.group().startsWith("%") || matcher.group().startsWith("$"));
        Assertions.assertTrue(matcher.group().endsWith("s") || matcher.group().endsWith("d"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{var1}",
            "{{var_2}}",
            "var {name}",
            "number {{0}}",
            "{{ 0 }}",
            "{users[2].name}",
            "test {{ curlyBrackets }}",
            "space within {{ curly Brackets }}",
            "underscore {{curly_Brackets}}",
            "array access {{curlyBrackets[2].element}}",
            "dot syntax {{curly.Brackets}}"
    })
    public void testMessageFormatNamedPattern(String text) {
        Pattern pattern = Pattern.compile(PlaceholderPatternType.MESSAGE_FORMAT_NAMED.getValue());
        Matcher matcher = pattern.matcher(text);
        Assertions.assertTrue(matcher.find());
        Assertions.assertTrue(matcher.group().startsWith("{"));
        Assertions.assertTrue(matcher.group().endsWith("}"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
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
            "test \n\t %2$@ \n\t test",
            "test \n\t %2$i \n\t test",
            "test \n\t %2$s \n\t test"
    })
    public void testIOSPositionalPattern(String text) {
        Pattern pattern = Pattern.compile(PlaceholderPatternType.IOS_POSITIONAL.getValue());
        Matcher matcher = pattern.matcher(text);
        Assertions.assertTrue(matcher.find());
        Assertions.assertTrue(matcher.group().startsWith("%"));
        Assertions.assertTrue(matcher.group().endsWith("@")
                || matcher.group().endsWith("i")
                || matcher.group().endsWith("s"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "%.f",
            "%.2f",
            "test \n\t %.3f \n\t test",
    })
    public void testIOSFloatPattern(String text) {
        Pattern pattern = Pattern.compile(PlaceholderPatternType.IOS_FLOAT.getValue());
        Matcher matcher = pattern.matcher(text);
        Assertions.assertTrue(matcher.find());
        Assertions.assertTrue(matcher.group().startsWith("%"));
        Assertions.assertTrue(matcher.group().endsWith("f"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "%1$d",
            "%1$,d",
            "test \n\t %3$,d \n\t test",
    })
    public void testIOSDoublePattern(String text) {
        Pattern pattern = Pattern.compile(PlaceholderPatternType.IOS_DOUBLE.getValue());
        Matcher matcher = pattern.matcher(text);
        Assertions.assertTrue(matcher.find());
        Assertions.assertTrue(matcher.group().startsWith("%"));
        Assertions.assertTrue(matcher.group().endsWith("d"));
    }
}