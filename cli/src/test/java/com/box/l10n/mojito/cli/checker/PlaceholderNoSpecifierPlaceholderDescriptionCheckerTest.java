package com.box.l10n.mojito.cli.checker;

import com.box.l10n.mojito.cli.command.checks.PlaceholderNoSpecifierPlaceholderDescriptionChecker;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

public class PlaceholderNoSpecifierPlaceholderDescriptionCheckerTest {

    private PlaceholderNoSpecifierPlaceholderDescriptionChecker placeholderNoSpecifierPlaceholderDescriptionChecker;

    @Before
    public void setup(){
        placeholderNoSpecifierPlaceholderDescriptionChecker = new PlaceholderNoSpecifierPlaceholderDescriptionChecker();
    }

    @Test
    public void testSuccess() {
        String source = "There is %d books on %d shelves";
        String comment = "Test comment 0:The number of books and shelves";
        Set<String> failures = placeholderNoSpecifierPlaceholderDescriptionChecker.checkCommentForDescriptions(source, comment);
        Assert.assertTrue(failures.isEmpty());
    }

    @Test
    public void testFailure() {
        String source = "There is %d books";
        String comment = "Test comment";
        Set<String> failures = placeholderNoSpecifierPlaceholderDescriptionChecker.checkCommentForDescriptions(source, comment);
        Assert.assertTrue(failures.size() == 1);
        Assert.assertTrue(failures.contains("Missing description for placeholder number '0' in comment."));
    }

    @Test
    public void testFailureWithMultiplePlaceholders() {
        String source = "There is %d books and %s shelves";
        String comment = "Test comment 0:The number of books";
        Set<String> failures = placeholderNoSpecifierPlaceholderDescriptionChecker.checkCommentForDescriptions(source, comment);
        Assert.assertTrue(failures.size() == 1);
        Assert.assertTrue(failures.contains("Missing description for placeholder number '1' in comment."));
    }
}
