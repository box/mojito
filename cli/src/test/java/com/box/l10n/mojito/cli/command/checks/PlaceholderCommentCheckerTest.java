package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.box.l10n.mojito.cli.command.extractioncheck.ExtractionCheckNotificationSender.QUOTE_MARKER;
import static com.box.l10n.mojito.regex.PlaceholderRegularExpressions.DOUBLE_BRACE_REGEX;
import static com.box.l10n.mojito.regex.PlaceholderRegularExpressions.PLACEHOLDER_NO_SPECIFIER_REGEX;
import static com.box.l10n.mojito.regex.PlaceholderRegularExpressions.PRINTF_LIKE_IOS_REGEX;
import static com.box.l10n.mojito.regex.PlaceholderRegularExpressions.PRINTF_LIKE_VARIABLE_TYPE_REGEX;
import static com.box.l10n.mojito.regex.PlaceholderRegularExpressions.SINGLE_BRACE_REGEX;

public class PlaceholderCommentCheckerTest {

    private PlaceholderCommentChecker placeholderCommentChecker;

    private List<AssetExtractionDiff> assetExtractionDiffs;

    @Before
    public void setup() {
        placeholderCommentChecker = new PlaceholderCommentChecker();
        placeholderCommentChecker.setCliCheckerOptions(new CliCheckerOptions(Sets.newHashSet(SINGLE_BRACE_REGEX), Sets.newHashSet(), ImmutableMap.<String, String>builder().build()));
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("Some string id --- Test context");
        assetExtractorTextUnit.setSource("A source string with a single {placeholder}.");
        assetExtractorTextUnit.setComments("Test comment placeholder:This is a description of a placeholder");
        addedTUs.add(assetExtractorTextUnit);
        assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
    }

    @Test
    public void testSuccessRun() {
        CliCheckResult result = placeholderCommentChecker.run(assetExtractionDiffs);
        Assert.assertTrue(result.isSuccessful());
        Assert.assertFalse(result.isHardFail());
    }

    @Test
    public void testMissingPlaceholderDescriptionInComment() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("Some string id --- Test context");
        assetExtractorTextUnit.setSource("A source string with a single {placeholder}.");
        assetExtractorTextUnit.setComments("Test comment ");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);

        CliCheckResult result = placeholderCommentChecker.run(assetExtractionDiffs);
        Assert.assertFalse(result.isSuccessful());
        Assert.assertFalse(result.isHardFail());
    }

    @Test
    public void testMissingIdenticalPlaceholderDescriptionInMultipleString() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("Some string id --- Test context");
        assetExtractorTextUnit.setSource("A source string with a single {placeholder}.");
        assetExtractorTextUnit.setComments("Test comment ");
        addedTUs.add(assetExtractorTextUnit);
        AssetExtractorTextUnit assetExtractorTextUnit2 = new AssetExtractorTextUnit();
        assetExtractorTextUnit2.setName("Some string id --- Test context");
        assetExtractorTextUnit2.setSource("A source string with a single {placeholder}.");
        assetExtractorTextUnit2.setComments("Test comment ");
        addedTUs.add(assetExtractorTextUnit2);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);

        CliCheckResult result = placeholderCommentChecker.run(assetExtractionDiffs);
        Assert.assertFalse(result.isSuccessful());
        Assert.assertFalse(result.isHardFail());
    }

    @Test
    public void testMultiplePlaceholderDescriptionsInComment() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("Some string id --- Test context");
        assetExtractorTextUnit.setSource("A source string with a single {placeholder} and {another} and so {more}.");
        assetExtractorTextUnit.setComments("Test comment placeholder:description 1,another: description 2,more: description 3");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);

        CliCheckResult result = placeholderCommentChecker.run(assetExtractionDiffs);
        Assert.assertTrue(result.isSuccessful());
        Assert.assertFalse(result.isHardFail());
    }

    @Test
    public void testOneOfMultiplePlaceholderDescriptionsMissingInComment() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("Some string id --- Test context");
        assetExtractorTextUnit.setSource("A source string with a single {placeholder} and {another} and some {more}.");
        assetExtractorTextUnit.setComments("Test comment placeholder:description 1,more: description 3");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);

        CliCheckResult result = placeholderCommentChecker.run(assetExtractionDiffs);
        Assert.assertFalse(result.isSuccessful());
        Assert.assertFalse(result.isHardFail());
        Assert.assertEquals("Placeholder description in comment check failed." + System.lineSeparator()
                + System.lineSeparator() +
                "String " + QUOTE_MARKER + "A source string with a single {placeholder} and {another} and some {more}." + QUOTE_MARKER + " failed check:" + System.lineSeparator() +
                "* Missing description for placeholder with name " + QUOTE_MARKER + "another" + QUOTE_MARKER + " in comment. Please add a description in the string comment in the form another:<description>", result.getNotificationText());
    }

    @Test
    public void testMultipleRegexsChecked() {
        placeholderCommentChecker.setCliCheckerOptions(new CliCheckerOptions(Sets.newHashSet(DOUBLE_BRACE_REGEX, PRINTF_LIKE_VARIABLE_TYPE_REGEX, PLACEHOLDER_NO_SPECIFIER_REGEX), Sets.newHashSet(), ImmutableMap.<String, String>builder().build()));
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setSource("A source string with different {placeholder} %(placeholder2)s {{placeholder3}} %d");
        assetExtractorTextUnit.setComments("Test comment placeholder:A description of placeholder,placeholder2: Another description,%d:some more,placeholder3: another description");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);

        CliCheckResult result = placeholderCommentChecker.run(assetExtractionDiffs);
        Assert.assertTrue(result.isSuccessful());
    }

    @Test
    public void testMultipleRegexsCheckedFailure() {
        placeholderCommentChecker.setCliCheckerOptions(new CliCheckerOptions(Sets.newHashSet(DOUBLE_BRACE_REGEX, PRINTF_LIKE_VARIABLE_TYPE_REGEX, PLACEHOLDER_NO_SPECIFIER_REGEX), Sets.newHashSet(), ImmutableMap.<String, String>builder().build()));
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setSource("A source string with different {placeholder} %(placeholder2)s {{placeholder3}} %d");
        assetExtractorTextUnit.setComments("Test comment placeholder:A description of placeholder,placeholder3: another description");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);

        CliCheckResult result = placeholderCommentChecker.run(assetExtractionDiffs);
        Assert.assertFalse(result.isSuccessful());
        Assert.assertTrue(result.getNotificationText().contains("* Missing description for placeholder with name " + QUOTE_MARKER + "placeholder2" + QUOTE_MARKER + " in comment."));
        Assert.assertTrue(result.getNotificationText().contains("* Missing description for placeholder with name " + QUOTE_MARKER + "%d" + QUOTE_MARKER + " in comment."));
    }

    @Test
    public void testIOSPlaceholder() {
        placeholderCommentChecker.setCliCheckerOptions(new CliCheckerOptions(Sets.newHashSet(PRINTF_LIKE_IOS_REGEX), Sets.newHashSet(), ImmutableMap.<String, String>builder().build()));
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("Some string id --- Test context");
        assetExtractorTextUnit.setSource("A source string with a single %1$@");
        assetExtractorTextUnit.setComments("Test comment ");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);

        CliCheckResult result = placeholderCommentChecker.run(assetExtractionDiffs);
        Assert.assertFalse(result.isSuccessful());
        Assert.assertFalse(result.isHardFail());
    }

    @Test
    public void testMultipleIOSPlaceholderTypesFails() {
        placeholderCommentChecker.setCliCheckerOptions(new CliCheckerOptions(Sets.newHashSet(PRINTF_LIKE_IOS_REGEX), Sets.newHashSet(), ImmutableMap.<String, String>builder().build()));
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("Some string id --- Test context");
        assetExtractorTextUnit.setSource("A source string with a single %1$@ %@ %2$@ld");
        assetExtractorTextUnit.setComments("Test comment ");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);

        CliCheckResult result = placeholderCommentChecker.run(assetExtractionDiffs);
        Assert.assertFalse(result.isSuccessful());
        Assert.assertFalse(result.isHardFail());
        Assert.assertTrue(result.getNotificationText().contains("Please add a description in the string comment in the form %1$@:<description>"));
        Assert.assertTrue(result.getNotificationText().contains("Please add a description in the string comment in the form %@:<description>"));
        Assert.assertTrue(result.getNotificationText().contains("Please add a description in the string comment in the form %2$@ld:<description>"));
    }

    @Test
    public void testMultipleIOSPlaceholderTypes() {
        placeholderCommentChecker.setCliCheckerOptions(new CliCheckerOptions(Sets.newHashSet(PRINTF_LIKE_IOS_REGEX), Sets.newHashSet(), ImmutableMap.<String, String>builder().build()));
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("Some string id --- Test context");
        assetExtractorTextUnit.setSource("A source string with a single %1$@ %@ %2$@ld");
        assetExtractorTextUnit.setComments("Test comment %1$@:A placeholder description, %@:Another placeholder description, %2$@ld: And another description");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);

        CliCheckResult result = placeholderCommentChecker.run(assetExtractionDiffs);
        Assert.assertTrue(result.isSuccessful());
        Assert.assertFalse(result.isHardFail());
        Assert.assertFalse(result.getNotificationText().contains("Please add a description in the string comment in the form %1$@:<description>"));
        Assert.assertFalse(result.getNotificationText().contains("Please add a description in the string comment in the form %@:<description>"));
        Assert.assertFalse(result.getNotificationText().contains("Please add a description in the string comment in the form %2$@ld:<description>"));
    }

    @Test
    public void testNullComment() {
        placeholderCommentChecker.setCliCheckerOptions(new CliCheckerOptions(Sets.newHashSet(DOUBLE_BRACE_REGEX, PRINTF_LIKE_VARIABLE_TYPE_REGEX, PLACEHOLDER_NO_SPECIFIER_REGEX), Sets.newHashSet(), ImmutableMap.<String, String>builder().build()));
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setSource("A source string with different {placeholder} %(placeholder2)s {{placeholder3}} %d");
        assetExtractorTextUnit.setComments(null);
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);

        CliCheckResult result = placeholderCommentChecker.run(assetExtractionDiffs);
        Assert.assertFalse(result.isSuccessful());
        Assert.assertTrue(result.getNotificationText().contains("Comment is empty."));
    }

}
