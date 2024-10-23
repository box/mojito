package com.box.l10n.mojito.cli.command.checks;

import static com.box.l10n.mojito.cli.command.extractioncheck.ExtractionCheckNotificationSender.QUOTE_MARKER;

import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SingleBracesPlaceholderDescriptionCheckerTest {

  private SingleBracesPlaceholderDescriptionChecker messageFormatPlaceholderCommentChecker;

  @Before
  public void setup() {
    messageFormatPlaceholderCommentChecker = new SingleBracesPlaceholderDescriptionChecker();
  }

  @Test
  public void testSuccessRun() {
    Set<String> failures =
        messageFormatPlaceholderCommentChecker.checkCommentForDescriptions(
            "A source string with a single {placeholder}.",
            "Test comment placeholder:This is a description of a placeholder");
    Assert.assertTrue(failures.isEmpty());
  }

  @Test
  public void testMissingPlaceholderDescriptionInComment() {
    Set<String> failures =
        messageFormatPlaceholderCommentChecker.checkCommentForDescriptions(
            "A source string with a single {placeholder}.", "Test comment");
    Assert.assertTrue(failures.size() == 1);
    Assert.assertTrue(
        failures.contains(
            "Missing description for placeholder with name "
                + QUOTE_MARKER
                + "placeholder"
                + QUOTE_MARKER
                + " in comment. Please add a description in the string comment in the form `placeholder:<description>`"));
  }

  @Test
  public void testNullComment() {
    Set<String> failures =
        messageFormatPlaceholderCommentChecker.checkCommentForDescriptions(
            "A source string with a single {placeholder}.", null);
    Assert.assertTrue(failures.size() == 1);
    Assert.assertTrue(
        failures.contains(
            "Missing description for placeholder with name "
                + QUOTE_MARKER
                + "placeholder"
                + QUOTE_MARKER
                + " in comment. Please add a description in the string comment in the form `placeholder:<description>`"));
  }

  @Test
  public void testMultiplePlaceholderDescriptionsInComment() {
    Set<String> failures =
        messageFormatPlaceholderCommentChecker.checkCommentForDescriptions(
            "A source string with a single {placeholder} and {another} and so {more}.",
            "Test comment placeholder:description 1,another: description 2,more: description 3");
    Assert.assertTrue(failures.isEmpty());
  }

  @Test
  public void testOneOfMultiplePlaceholderDescriptionsMissingInComment() {
    Set<String> failures =
        messageFormatPlaceholderCommentChecker.checkCommentForDescriptions(
            "A source string with a single {placeholder} and {another} and some {more}.",
            "Test comment placeholder:description 1,more: description 3");
    Assert.assertTrue(failures.size() == 1);
    Assert.assertTrue(
        failures.contains(
            "Missing description for placeholder with name "
                + QUOTE_MARKER
                + "another"
                + QUOTE_MARKER
                + " in comment. Please add a description in the string comment in the form `another:<description>`"));
  }

  @Test
  public void testPluralPlaceholderMissingDescription() {
    String source = "{numFiles, plural, one{# There is one file} other{There are # files}}";
    String comment = "Test comment";
    Set<String> failures =
        messageFormatPlaceholderCommentChecker.checkCommentForDescriptions(source, comment);
    Assert.assertTrue(failures.size() == 1);
    Assert.assertTrue(
        failures.contains(
            "Missing description for placeholder with name "
                + QUOTE_MARKER
                + "numFiles"
                + QUOTE_MARKER
                + " in comment. Please add a description in the string comment in the form `numFiles:<description>`"));
  }

  @Test
  public void testNumberedPlaceholder() {
    String source = "A source string with a single {0}.";
    String comment = "Test comment";

    Set<String> failures =
        messageFormatPlaceholderCommentChecker.checkCommentForDescriptions(source, comment);
    Assert.assertTrue(failures.size() == 1);
    Assert.assertTrue(
        failures.contains(
            "Missing description for placeholder number "
                + QUOTE_MARKER
                + "0"
                + QUOTE_MARKER
                + " in comment. Please add a description in the string comment in the form `0:<description>`"));
  }

  @Test
  public void testOtherRegexInString() {
    String source = "A source string with a different placeholder types %(count)s %d %3.";
    String comment = "Test comment";

    Set<String> failures =
        messageFormatPlaceholderCommentChecker.checkCommentForDescriptions(source, comment);
    Assert.assertTrue(failures.size() == 0);
  }
}
