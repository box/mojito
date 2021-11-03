package com.box.l10n.mojito.cli.checker;

import com.box.l10n.mojito.cli.command.checks.CliCheckResult;
import com.box.l10n.mojito.cli.command.checks.CliCheckerOptions;
import com.box.l10n.mojito.cli.command.checks.EmptyPlaceholderChecker;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.box.l10n.mojito.regex.PlaceholderRegularExpressions.SINGLE_BRACE_REGEX;

public class EmptyPlaceholderCheckerTest {

    private EmptyPlaceholderChecker emptyPlaceholderChecker;

    @Before
    public void setup() {
        emptyPlaceholderChecker = new EmptyPlaceholderChecker();
        emptyPlaceholderChecker.setCliCheckerOptions(new CliCheckerOptions(Sets.newHashSet(SINGLE_BRACE_REGEX.getRegex()), Sets.newHashSet(), "", ""));
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("Some string id --- Test context");
        assetExtractorTextUnit.setSource("A source string with a single {placeholder}.");
        assetExtractorTextUnit.setComments("Test comment");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        emptyPlaceholderChecker.setAssetExtractionDiffs(assetExtractionDiffs);
    }

    @Test
    public void testNoEmptyPlaceholders() {
        CliCheckResult result = emptyPlaceholderChecker.call();
        Assert.assertTrue(result.isSuccessful());
        Assert.assertFalse(result.isHardFail());
    }

    @Test
    public void testEmptyPlaceholder() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("Some string id --- Test context");
        assetExtractorTextUnit.setSource("A source string with a empty placeholder {}.");
        assetExtractorTextUnit.setComments("Test comment");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        emptyPlaceholderChecker.setAssetExtractionDiffs(assetExtractionDiffs);

        CliCheckResult result = emptyPlaceholderChecker.call();
        Assert.assertFalse(result.isSuccessful());
        Assert.assertFalse(result.isHardFail());
        Assert.assertEquals("Found empty placeholders in the following source strings:" + System.lineSeparator() +
                "\t* 'A source string with a empty placeholder {}.'" + System.lineSeparator() +
                "Please remove or update placeholders to contain a descriptive name." + System.lineSeparator(), result.getNotificationText());
    }

    @Test
    public void testMultipleEmptyPlaceholdersInString() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("Some string id --- Test context");
        assetExtractorTextUnit.setSource("A source string with two {} empty placeholders {}.");
        assetExtractorTextUnit.setComments("Test comment");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        emptyPlaceholderChecker.setAssetExtractionDiffs(assetExtractionDiffs);

        CliCheckResult result = emptyPlaceholderChecker.call();
        Assert.assertFalse(result.isSuccessful());
        Assert.assertFalse(result.isHardFail());
        Assert.assertEquals("Found empty placeholders in the following source strings:" + System.lineSeparator() +
                "\t* 'A source string with two {} empty placeholders {}.'" + System.lineSeparator() +
                "Please remove or update placeholders to contain a descriptive name." + System.lineSeparator(), result.getNotificationText());
    }

    @Test
    public void testMixOfStringsWithEmptyAndNonEmptyPlaceholders() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("Some string id --- Test context");
        assetExtractorTextUnit.setSource("A source string with two {} empty placeholders {}.");
        assetExtractorTextUnit.setComments("Test comment");
        addedTUs.add(assetExtractorTextUnit);
        AssetExtractorTextUnit assetExtractorTextUnit2 = new AssetExtractorTextUnit();
        assetExtractorTextUnit2.setName("Another string id --- Another Test context");
        assetExtractorTextUnit2.setSource("Another source string with two {name1} placeholders {name2}.");
        assetExtractorTextUnit2.setComments("Another comment");
        addedTUs.add(assetExtractorTextUnit2);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        emptyPlaceholderChecker.setAssetExtractionDiffs(assetExtractionDiffs);

        CliCheckResult result = emptyPlaceholderChecker.call();
        Assert.assertFalse(result.isSuccessful());
        Assert.assertFalse(result.isHardFail());
        Assert.assertEquals("Found empty placeholders in the following source strings:" + System.lineSeparator() +
                "\t* 'A source string with two {} empty placeholders {}.'" + System.lineSeparator() +
                "Please remove or update placeholders to contain a descriptive name." + System.lineSeparator(), result.getNotificationText());
    }

    @Test
    public void testStringContainingEmptyAndNamedPlaceholders() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("Some string id --- Test context");
        assetExtractorTextUnit.setSource("A source string with two {} placeholders {name}.");
        assetExtractorTextUnit.setComments("Test comment");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        emptyPlaceholderChecker.setAssetExtractionDiffs(assetExtractionDiffs);

        CliCheckResult result = emptyPlaceholderChecker.call();
        Assert.assertFalse(result.isSuccessful());
        Assert.assertFalse(result.isHardFail());
        Assert.assertEquals("Found empty placeholders in the following source strings:" + System.lineSeparator() +
                "\t* 'A source string with two {} placeholders {name}.'" + System.lineSeparator() +
                "Please remove or update placeholders to contain a descriptive name." + System.lineSeparator(), result.getNotificationText());
    }

    @Test
    public void testMultipleStringsWithEmptyPlaceholders() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("Some string id --- Test context");
        assetExtractorTextUnit.setSource("A source string with two {} empty placeholders {}.");
        assetExtractorTextUnit.setComments("Test comment");
        addedTUs.add(assetExtractorTextUnit);
        AssetExtractorTextUnit assetExtractorTextUnit2 = new AssetExtractorTextUnit();
        assetExtractorTextUnit2.setName("Another string id --- Another Test context");
        assetExtractorTextUnit2.setSource("Another source string with two {} empty placeholders {}.");
        assetExtractorTextUnit2.setComments("Another comment");
        addedTUs.add(assetExtractorTextUnit2);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        emptyPlaceholderChecker.setAssetExtractionDiffs(assetExtractionDiffs);

        CliCheckResult result = emptyPlaceholderChecker.call();
        Assert.assertFalse(result.isSuccessful());
        Assert.assertFalse(result.isHardFail());
        Assert.assertEquals("Found empty placeholders in the following source strings:" + System.lineSeparator() +
                "\t* 'A source string with two {} empty placeholders {}.'" + System.lineSeparator() +
                "\t* 'Another source string with two {} empty placeholders {}.'" + System.lineSeparator() +
                "Please remove or update placeholders to contain a descriptive name." + System.lineSeparator(), result.getNotificationText());
    }

    //TODO: Test other regex options
}
