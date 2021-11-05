package com.box.l10n.mojito.cli.checker;

import com.box.l10n.mojito.cli.command.checks.PrintfLikeVariableTypePlaceholderDescriptionChecker;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class PrintfLikeVariableTypePlaceholderDescriptionCheckerTest {

    private PrintfLikeVariableTypePlaceholderDescriptionChecker printfLikeVariableTypePlaceholderDescriptionChecker;

    @Before
    public void setup() {
        printfLikeVariableTypePlaceholderDescriptionChecker = new PrintfLikeVariableTypePlaceholderDescriptionChecker();
    }

    @Test
    public void testSuccess() {
        String source = "There is %(count)d books";
        String comment = "Test comment count:The number of books";
        List<String> failures = printfLikeVariableTypePlaceholderDescriptionChecker.checkCommentForDescriptions(source, comment);
        Assert.assertTrue(failures.isEmpty());
    }

    @Test
    public void testFailure() {
        String source = "There is %(count)d books";
        String comment = "Test comment";
        List<String> failures = printfLikeVariableTypePlaceholderDescriptionChecker.checkCommentForDescriptions(source, comment);
        Assert.assertTrue(failures.size() == 1);
        Assert.assertEquals("Missing description for placeholder with name 'count' in comment.", failures.get(0));
    }

    @Test
    public void testFailureWithMultiplePlaceholders() {
        String source = "There is %(count)d books and %(shelf_count)d shelves";
        String comment = "Test comment count:The number of books";
        List<String> failures = printfLikeVariableTypePlaceholderDescriptionChecker.checkCommentForDescriptions(source, comment);
        Assert.assertTrue(failures.size() == 1);
        Assert.assertEquals("Missing description for placeholder with name 'shelf_count' in comment.", failures.get(0));
    }

    @Test
    public void testFailureWithNoNames() {
        String source = "There is %d books";
        String comment = "Test comment";
        List<String> failures = printfLikeVariableTypePlaceholderDescriptionChecker.checkCommentForDescriptions(source, comment);
        Assert.assertTrue(failures.size() == 1);
        Assert.assertEquals("Missing description for placeholder number '0' in comment.", failures.get(0));
    }

    @Test
    public void testFailureWithMultipleNoNames() {
        String source = "There is %d books, %d shelves and %d librarians";
        String comment = "Test comment 0:The number of books,2:The number of librarians";
        List<String> failures = printfLikeVariableTypePlaceholderDescriptionChecker.checkCommentForDescriptions(source, comment);
        Assert.assertTrue(failures.size() == 1);
        Assert.assertEquals("Missing description for placeholder number '1' in comment.", failures.get(0));
    }
}
