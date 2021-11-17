package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.box.l10n.mojito.regex.PlaceholderRegularExpressions.DOUBLE_BRACE_REGEX;
import static com.box.l10n.mojito.regex.PlaceholderRegularExpressions.PLACEHOLDER_NO_SPECIFIER_REGEX;
import static com.box.l10n.mojito.regex.PlaceholderRegularExpressions.PRINTF_LIKE_VARIABLE_TYPE_REGEX;
import static com.box.l10n.mojito.regex.PlaceholderRegularExpressions.SINGLE_BRACE_REGEX;

public class PlaceholderCommentCheckerTest {

    private PlaceholderCommentChecker placeholderCommentChecker;

    private List<AssetExtractionDiff> assetExtractionDiffs;

    @Before
    public void setup() {
        placeholderCommentChecker = new PlaceholderCommentChecker();
        placeholderCommentChecker.setCliCheckerOptions(new CliCheckerOptions(Sets.newHashSet(SINGLE_BRACE_REGEX), Sets.newHashSet(), "", ""));
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
                "String 'A source string with a single {placeholder} and {another} and some {more}.' failed check:" + System.lineSeparator() +
                "* Missing description for placeholder with name 'another' in comment. Please add a description in the string comment in the form another:<description>", result.getNotificationText());
    }

    @Test
    public void testMultipleRegexsChecked() {
        placeholderCommentChecker.setCliCheckerOptions(new CliCheckerOptions(Sets.newHashSet(DOUBLE_BRACE_REGEX, PRINTF_LIKE_VARIABLE_TYPE_REGEX, PLACEHOLDER_NO_SPECIFIER_REGEX), Sets.newHashSet(), "", ""));
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
        placeholderCommentChecker.setCliCheckerOptions(new CliCheckerOptions(Sets.newHashSet(DOUBLE_BRACE_REGEX, PRINTF_LIKE_VARIABLE_TYPE_REGEX, PLACEHOLDER_NO_SPECIFIER_REGEX), Sets.newHashSet(), "", ""));
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
        Assert.assertTrue(result.getNotificationText().contains("* Missing description for placeholder with name 'placeholder2' in comment."));
        Assert.assertTrue(result.getNotificationText().contains("* Missing description for placeholder with name '%d' in comment."));
    }

    @Test
    public void testNullComment() {
        placeholderCommentChecker.setCliCheckerOptions(new CliCheckerOptions(Sets.newHashSet(DOUBLE_BRACE_REGEX, PRINTF_LIKE_VARIABLE_TYPE_REGEX, PLACEHOLDER_NO_SPECIFIER_REGEX), Sets.newHashSet(), "", ""));
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
