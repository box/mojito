package com.box.l10n.mojito.cli.checks;

import com.box.l10n.mojito.cli.command.checks.PrintfLikeVariableTypePlaceholderDescriptionChecker;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

public class PrintfLikeVariableTypePlaceholderDescriptionCheckerTest {

    private PrintfLikeVariableTypePlaceholderDescriptionChecker printfLikeVariableTypePlaceholderDescriptionChecker;

    @Before
    public void setup() {
        printfLikeVariableTypePlaceholderDescriptionChecker = new PrintfLikeVariableTypePlaceholderDescriptionChecker();
    }

    @Test
    public void testSuccess() {
        String source = "There is %(count)d books on %(count)d shelves";
        String comment = "Test comment count:The number of books and shelves";
        Set<String> failures = printfLikeVariableTypePlaceholderDescriptionChecker.checkCommentForDescriptions(source, comment);
        Assert.assertTrue(failures.isEmpty());
    }

    @Test
    public void testFailure() {
        String source = "There is %(count)d books";
        String comment = "Test comment";
        Set<String> failures = printfLikeVariableTypePlaceholderDescriptionChecker.checkCommentForDescriptions(source, comment);
        Assert.assertTrue(failures.size() == 1);
        Assert.assertTrue(failures.contains("Missing description for placeholder with name 'count' in comment."));
    }

    @Test
    public void testFailureWithMultiplePlaceholders() {
        String source = "There is %(count)d books and %(shelf_count)d shelves";
        String comment = "Test comment count:The number of books";
        Set<String> failures = printfLikeVariableTypePlaceholderDescriptionChecker.checkCommentForDescriptions(source, comment);
        Assert.assertTrue(failures.size() == 1);
        Assert.assertTrue(failures.contains("Missing description for placeholder with name 'shelf_count' in comment."));
    }

}
