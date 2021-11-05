package com.box.l10n.mojito.cli.checker;

import com.box.l10n.mojito.cli.command.checks.DoubleBracesPlaceholderDescriptionChecker;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class DoubleBracesPlaceholderDescriptionCheckerTest {

    private DoubleBracesPlaceholderDescriptionChecker doubleBracesPlaceholderCommentChecker;

    @Before
    public void setup() {
        doubleBracesPlaceholderCommentChecker = new DoubleBracesPlaceholderDescriptionChecker();

    }

    @Test
    public void testSuccessRun() {
        List<String> failures = doubleBracesPlaceholderCommentChecker.checkCommentForDescriptions("A source string with a single {{placeholder}}.", "Test comment placeholder:This is a description of a placeholder");
        Assert.assertTrue(failures.isEmpty());
    }

    @Test
    public void testMissingPlaceholderDescriptionInComment() {
        List<String> failures = doubleBracesPlaceholderCommentChecker.checkCommentForDescriptions("A source string with a single {{placeholder}}.", "Test comment");
        Assert.assertTrue(failures.size() == 1);
        Assert.assertTrue(failures.get(0).equals("Missing description for placeholder with name 'placeholder' in comment."));
    }

    @Test
    public void testMultiplePlaceholderDescriptionsInComment() {
        List<String> failures = doubleBracesPlaceholderCommentChecker.checkCommentForDescriptions("A source string with a single {{placeholder}} and {another} and so {more}.", "Test comment placeholder:description 1,another: description 2,more: description 3");
        Assert.assertTrue(failures.isEmpty());
    }

    @Test
    public void testOneOfMultiplePlaceholderDescriptionsMissingInComment() {
        List<String> failures = doubleBracesPlaceholderCommentChecker.checkCommentForDescriptions("A source string with a single {{placeholder}} and {another} and some {more}.", "Test comment placeholder:description 1,more: description 3");
        Assert.assertTrue(failures.size() == 1);
        Assert.assertTrue(failures.get(0).equals("Missing description for placeholder with name 'another' in comment."));
    }

    @Test
    public void testPluralPlaceholderMissingDescription(){
        String source = "{{numFiles, plural, one{{# There is one file}} other{{There are # files}}}}";
        String comment = "Test comment";
        List<String> failures = doubleBracesPlaceholderCommentChecker.checkCommentForDescriptions(source, comment);
        Assert.assertTrue(failures.size() == 1);
        Assert.assertEquals("Missing description for placeholder with name 'numFiles' in comment.", failures.get(0));
    }

    @Test
    public void testNumberedPlaceholder() {
        String source = "A source string with a single {{0}}.";
        String comment = "Test comment";
        List<String> failures = doubleBracesPlaceholderCommentChecker.checkCommentForDescriptions(source, comment);
        Assert.assertTrue(failures.size() == 1);
        Assert.assertEquals("Missing description for placeholder number '0' in comment.", failures.get(0));
    }

}
