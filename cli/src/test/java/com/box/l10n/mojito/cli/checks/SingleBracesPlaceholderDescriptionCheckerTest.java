package com.box.l10n.mojito.cli.checks;

import com.box.l10n.mojito.cli.command.checks.SingleBracesPlaceholderDescriptionChecker;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

public class SingleBracesPlaceholderDescriptionCheckerTest {

    private SingleBracesPlaceholderDescriptionChecker messageFormatPlaceholderCommentChecker;

    @Before
    public void setup() {
        messageFormatPlaceholderCommentChecker = new SingleBracesPlaceholderDescriptionChecker();

    }

    @Test
    public void testSuccessRun() {
        Set<String> failures = messageFormatPlaceholderCommentChecker.checkCommentForDescriptions("A source string with a single {placeholder}.", "Test comment placeholder:This is a description of a placeholder");
        Assert.assertTrue(failures.isEmpty());
    }

    @Test
    public void testMissingPlaceholderDescriptionInComment() {
        Set<String> failures = messageFormatPlaceholderCommentChecker.checkCommentForDescriptions("A source string with a single {placeholder}.", "Test comment");
        Assert.assertTrue(failures.size() == 1);
        Assert.assertTrue(failures.contains("Missing description for placeholder with name 'placeholder' in comment."));
    }

    @Test
    public void testMultiplePlaceholderDescriptionsInComment() {
        Set<String> failures = messageFormatPlaceholderCommentChecker.checkCommentForDescriptions("A source string with a single {placeholder} and {another} and so {more}.", "Test comment placeholder:description 1,another: description 2,more: description 3");
        Assert.assertTrue(failures.isEmpty());
    }

    @Test
    public void testOneOfMultiplePlaceholderDescriptionsMissingInComment() {
        Set<String> failures = messageFormatPlaceholderCommentChecker.checkCommentForDescriptions("A source string with a single {placeholder} and {another} and some {more}.", "Test comment placeholder:description 1,more: description 3");
        Assert.assertTrue(failures.size() == 1);
        Assert.assertTrue(failures.contains("Missing description for placeholder with name 'another' in comment."));
    }

    @Test
    public void testPluralPlaceholderMissingDescription(){
        String source = "{numFiles, plural, one{# There is one file} other{There are # files}}";
        String comment = "Test comment";
        Set<String> failures = messageFormatPlaceholderCommentChecker.checkCommentForDescriptions(source, comment);
        Assert.assertTrue(failures.size() == 1);
        Assert.assertTrue(failures.contains("Missing description for placeholder with name 'numFiles' in comment."));
    }

    @Test
    public void testNumberedPlaceholder() {
        String source = "A source string with a single {0}.";
        String comment = "Test comment";

        Set<String> failures = messageFormatPlaceholderCommentChecker.checkCommentForDescriptions(source, comment);
        Assert.assertTrue(failures.size() == 1);
        Assert.assertTrue(failures.contains("Missing description for placeholder number '0' in comment."));
    }

    @Test
    public void testOtherRegexInString() {
        String source = "A source string with a different placeholder types %(count)s %d %3.";
        String comment = "Test comment";

        Set<String> failures = messageFormatPlaceholderCommentChecker.checkCommentForDescriptions(source, comment);
        Assert.assertTrue(failures.size() == 0);
    }
    
}
