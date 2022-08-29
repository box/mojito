package com.box.l10n.mojito.cli.command.checks;

import static com.box.l10n.mojito.cli.command.extractioncheck.ExtractionCheckNotificationSender.QUOTE_MARKER;

import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PrintfLikeVariableTypePlaceholderDescriptionCheckerTest {

  private PrintfLikeVariableTypePlaceholderDescriptionChecker
      printfLikeVariableTypePlaceholderDescriptionChecker;

  @Before
  public void setup() {
    printfLikeVariableTypePlaceholderDescriptionChecker =
        new PrintfLikeVariableTypePlaceholderDescriptionChecker();
  }

  @Test
  public void testSuccess() {
    String source = "There is %(count)d books on %(count)d shelves";
    String comment = "Test comment count:The number of books and shelves";
    Set<String> failures =
        printfLikeVariableTypePlaceholderDescriptionChecker.checkCommentForDescriptions(
            source, comment);
    Assert.assertTrue(failures.isEmpty());
  }

  @Test
  public void testSuccessWithBraces() {
    String source = "There is %{count}d books on %{count}d shelves";
    String comment = "Test comment count:The number of books and shelves";
    Set<String> failures =
        printfLikeVariableTypePlaceholderDescriptionChecker.checkCommentForDescriptions(
            source, comment);
    Assert.assertTrue(failures.isEmpty());
  }

  @Test
  public void testFailure() {
    String source = "There is %(count)d books";
    String comment = "Test comment";
    Set<String> failures =
        printfLikeVariableTypePlaceholderDescriptionChecker.checkCommentForDescriptions(
            source, comment);
    Assert.assertTrue(failures.size() == 1);
    Assert.assertTrue(
        failures.contains(
            "Missing description for placeholder with name "
                + QUOTE_MARKER
                + "count"
                + QUOTE_MARKER
                + " in comment. Please add a description in the string comment in the form count:<description>"));
  }

  @Test
  public void testFailureWithBraces() {
    String source = "There is %{count}d books";
    String comment = "Test comment";
    Set<String> failures =
        printfLikeVariableTypePlaceholderDescriptionChecker.checkCommentForDescriptions(
            source, comment);
    Assert.assertTrue(failures.size() == 1);
    Assert.assertTrue(
        failures.contains(
            "Missing description for placeholder with name "
                + QUOTE_MARKER
                + "count"
                + QUOTE_MARKER
                + " in comment. Please add a description in the string comment in the form count:<description>"));
  }

  @Test
  public void testNullComment() {
    String source = "There is %(count)d books";
    String comment = null;
    Set<String> failures =
        printfLikeVariableTypePlaceholderDescriptionChecker.checkCommentForDescriptions(
            source, comment);
    Assert.assertTrue(failures.size() == 1);
    Assert.assertTrue(
        failures.contains(
            "Missing description for placeholder with name "
                + QUOTE_MARKER
                + "count"
                + QUOTE_MARKER
                + " in comment. Please add a description in the string comment in the form count:<description>"));
  }

  @Test
  public void testFailureWithMultiplePlaceholders() {
    String source = "There is %(count)d books and %(shelf_count)d shelves";
    String comment = "Test comment count:The number of books";
    Set<String> failures =
        printfLikeVariableTypePlaceholderDescriptionChecker.checkCommentForDescriptions(
            source, comment);
    Assert.assertTrue(failures.size() == 1);
    Assert.assertTrue(
        failures.contains(
            "Missing description for placeholder with name "
                + QUOTE_MARKER
                + "shelf_count"
                + QUOTE_MARKER
                + " in comment. Please add a description in the string comment in the form shelf_count:<description>"));
  }

  @Test
  public void testSuccessWithMultiplePlaceholders() {
    String source = "There is %(count)d books and %(shelf_count)d shelves";
    String comment = "Test comment count:The number of books,shelf_count:The number of shelves";
    Set<String> failures =
        printfLikeVariableTypePlaceholderDescriptionChecker.checkCommentForDescriptions(
            source, comment);
    Assert.assertTrue(failures.size() == 0);
  }
}
