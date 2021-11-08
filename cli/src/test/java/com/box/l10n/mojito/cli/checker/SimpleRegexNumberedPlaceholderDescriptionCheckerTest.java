package com.box.l10n.mojito.cli.checker;

import com.box.l10n.mojito.cli.command.checks.SimpleRegexNumberedPlaceholderDescriptionChecker;
import com.box.l10n.mojito.regex.PlaceholderRegularExpressions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;
import java.util.regex.Pattern;

public class SimpleRegexNumberedPlaceholderDescriptionCheckerTest {

    private SimpleRegexNumberedPlaceholderDescriptionChecker simpleRegexNumberedPlaceholderDescriptionChecker;

    @Before
    public void setup(){
        simpleRegexNumberedPlaceholderDescriptionChecker = new SimpleRegexNumberedPlaceholderDescriptionChecker(PlaceholderRegularExpressions.SIMPLE_PRINTF_REGEX);
    }

    @Test
    public void testSuccess() {
        String source = "There is %1 books on %1 shelves";
        String comment = "Test comment %1:The number of books and shelves";
        Set<String> failures = simpleRegexNumberedPlaceholderDescriptionChecker.checkCommentForDescriptions(source, comment);
        Assert.assertTrue(failures.isEmpty());
    }

    @Test
    public void testFailure() {
        String source = "There is %1 books";
        String comment = "Test comment";
        Set<String> failures = simpleRegexNumberedPlaceholderDescriptionChecker.checkCommentForDescriptions(source, comment);
        Assert.assertTrue(failures.size() == 1);
        Assert.assertTrue(failures.contains("Missing description for placeholder with name '%1' in comment."));
    }

    @Test
    public void testFailureWithMultiplePlaceholders() {
        String source = "There is %1 books and %2 shelves";
        String comment = "Test comment %1:The number of books";
        Set<String> failures = simpleRegexNumberedPlaceholderDescriptionChecker.checkCommentForDescriptions(source, comment);
        Assert.assertTrue(failures.size() == 1);
        Assert.assertTrue(failures.contains("Missing description for placeholder with name '%2' in comment."));
    }

    @Test
    public void testNoSpecifierRegexSuccess() {
        String source = "There is %d books on %d shelves";
        String comment = "Test comment %d:The number of books and shelves";
        simpleRegexNumberedPlaceholderDescriptionChecker.setPattern(Pattern.compile(PlaceholderRegularExpressions.PLACEHOLDER_NO_SPECIFIER_REGEX.getRegex()));
        Set<String> failures = simpleRegexNumberedPlaceholderDescriptionChecker.checkCommentForDescriptions(source, comment);
        Assert.assertTrue(failures.isEmpty());
    }

    @Test
    public void testNoSpecifierRegexFailure() {
        String source = "There is %d books on %d shelves";
        String comment = "Test comment";
        simpleRegexNumberedPlaceholderDescriptionChecker.setPattern(Pattern.compile(PlaceholderRegularExpressions.PLACEHOLDER_NO_SPECIFIER_REGEX.getRegex()));
        Set<String> failures = simpleRegexNumberedPlaceholderDescriptionChecker.checkCommentForDescriptions(source, comment);
        Assert.assertTrue(failures.size() == 1);
        Assert.assertTrue(failures.contains("Missing description for placeholder with name '%d' in comment."));
    }

    @Test
    public void testNoSpecifierRegexFailureWithMultiplePlaceholders() {
        String source = "There is %d books and %s shelves";
        String comment = "Test comment %d:The number of books";
        simpleRegexNumberedPlaceholderDescriptionChecker.setPattern(Pattern.compile(PlaceholderRegularExpressions.PLACEHOLDER_NO_SPECIFIER_REGEX.getRegex()));
        Set<String> failures = simpleRegexNumberedPlaceholderDescriptionChecker.checkCommentForDescriptions(source, comment);
        Assert.assertTrue(failures.size() == 1);
        Assert.assertTrue(failures.contains("Missing description for placeholder with name '%s' in comment."));
    }
}
