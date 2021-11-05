package com.box.l10n.mojito.cli.checker;

import com.box.l10n.mojito.cli.command.checks.CliCheckResult;
import com.box.l10n.mojito.cli.command.checks.CliCheckerOptions;
import com.box.l10n.mojito.cli.command.checks.PlaceholderCommentChecker;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.box.l10n.mojito.regex.PlaceholderRegularExpressions.SINGLE_BRACE_REGEX;

public class PlaceholderDescriptionCheckerTest {

    private PlaceholderCommentChecker placeholderCommentChecker;

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
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        placeholderCommentChecker.setAssetExtractionDiffs(assetExtractionDiffs);
    }

    @Test
    public void testSuccessRun() {
        CliCheckResult result = placeholderCommentChecker.run();
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
        placeholderCommentChecker.setAssetExtractionDiffs(assetExtractionDiffs);

        CliCheckResult result = placeholderCommentChecker.run();
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
        placeholderCommentChecker.setAssetExtractionDiffs(assetExtractionDiffs);

        CliCheckResult result = placeholderCommentChecker.run();
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
        placeholderCommentChecker.setAssetExtractionDiffs(assetExtractionDiffs);

        CliCheckResult result = placeholderCommentChecker.run();
        Assert.assertFalse(result.isSuccessful());
        Assert.assertFalse(result.isHardFail());
        Assert.assertEquals("Placeholder description in comment check failed." + System.lineSeparator()
                + System.lineSeparator() +
                "String 'A source string with a single {placeholder} and {another} and some {more}.' failed check:" + System.lineSeparator() +
                "\t* Missing description for placeholder with name 'another' in comment." + System.lineSeparator(), result.getNotificationText());
    }

    //TODO: Add tests for other regex types
}
