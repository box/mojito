package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.cli.command.checks.CliCheckResult;
import com.box.l10n.mojito.cli.command.checks.CliCheckerOptions;
import com.box.l10n.mojito.cli.command.checks.CliCheckerType;
import com.box.l10n.mojito.cli.command.checks.ContextAndCommentCliChecker;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.box.l10n.mojito.regex.PlaceholderRegularExpressions.SINGLE_BRACE_REGEX;

public class ContextAndCommentCliCheckerTest {

    private ContextAndCommentCliChecker contextAndCommentCliChecker;
    private List<AssetExtractionDiff> assetExtractionDiffs;

    @Before
    public void setup() {
        contextAndCommentCliChecker = new ContextAndCommentCliChecker();
        contextAndCommentCliChecker.setCliCheckerOptions(new CliCheckerOptions(Sets.newHashSet(SINGLE_BRACE_REGEX), Sets.newHashSet(), ImmutableMap.<String, String>builder().build()));
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("Some string id --- Test context");
        assetExtractorTextUnit.setSource("A source string with no errors.");
        assetExtractorTextUnit.setComments("Test comment");
        addedTUs.add(assetExtractorTextUnit);
        assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);

    }

    @Test
    public void testContextAndCommentPreset() {
        CliCheckResult result = contextAndCommentCliChecker.run(assetExtractionDiffs);
        Assert.assertTrue(result.isSuccessful());
        Assert.assertFalse(result.isHardFail());
    }

    @Test
    public void testCommentIsEmpty() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("Some string id --- Test context");
        assetExtractorTextUnit.setSource("A source string with no errors.");
        assetExtractorTextUnit.setComments(null);
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);

        CliCheckResult result = contextAndCommentCliChecker.run(assetExtractionDiffs);
        Assert.assertFalse(result.isSuccessful());
        Assert.assertFalse(result.isHardFail());
        Assert.assertEquals("Context and comment check found failures:" + System.lineSeparator() +
                "* Source string 'A source string with no errors.' failed check with error: Comment string is empty."
                + System.lineSeparator(), result.getNotificationText());
    }

    @Test
    public void testContextIsEmpty() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("Some string id");
        assetExtractorTextUnit.setSource("A source string with no errors.");
        assetExtractorTextUnit.setComments("Test comment");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);

        CliCheckResult result = contextAndCommentCliChecker.run(assetExtractionDiffs);
        Assert.assertFalse(result.isSuccessful());
        Assert.assertFalse(result.isHardFail());
        Assert.assertEquals("Context and comment check found failures:" + System.lineSeparator() +
                "* Source string 'A source string with no errors.' failed check with error: Context string is empty."
                + System.lineSeparator(), result.getNotificationText());
    }

    @Test
    public void testBothCommentAndContextAreEmpty() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("");
        assetExtractorTextUnit.setSource("A source string with no errors.");
        assetExtractorTextUnit.setComments(null);
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);

        CliCheckResult result = contextAndCommentCliChecker.run(assetExtractionDiffs);
        Assert.assertFalse(result.isSuccessful());
        Assert.assertFalse(result.isHardFail());
        Assert.assertEquals("Context and comment check found failures:" + System.lineSeparator() +
                "* Source string 'A source string with no errors.' failed check with error: Context and comment strings are both empty."
                + System.lineSeparator(), result.getNotificationText());
    }

    @Test
    public void testIdenticalContextAndCommentStrings() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("Some id --- Identical string");
        assetExtractorTextUnit.setSource("A source string with no errors.");
        assetExtractorTextUnit.setComments("Identical string");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);

        CliCheckResult result = contextAndCommentCliChecker.run(assetExtractionDiffs);
        Assert.assertFalse(result.isSuccessful());
        Assert.assertFalse(result.isHardFail());
        Assert.assertEquals("Context and comment check found failures:" + System.lineSeparator() +
                "* Source string 'A source string with no errors.' failed check with error: Context & comment strings should not be identical."
                + System.lineSeparator(), result.getNotificationText());
    }

    @Test
    public void testHardFailure() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("");
        assetExtractorTextUnit.setSource("A source string with no errors.");
        assetExtractorTextUnit.setComments(null);
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        contextAndCommentCliChecker.setCliCheckerOptions(new CliCheckerOptions(Sets.newHashSet(SINGLE_BRACE_REGEX), Sets.newHashSet(CliCheckerType.CONTEXT_COMMENT_CHECKER), ImmutableMap.<String, String>builder().build()));

        CliCheckResult result = contextAndCommentCliChecker.run(assetExtractionDiffs);
        Assert.assertFalse(result.isSuccessful());
        Assert.assertTrue(result.isHardFail());
    }

    @Test
    public void testStringsOnlyContainingWhitespace() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("Some string id --- ");
        assetExtractorTextUnit.setSource("A source string with no errors.");
        assetExtractorTextUnit.setComments(" ");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);

        CliCheckResult result = contextAndCommentCliChecker.run(assetExtractionDiffs);
        Assert.assertFalse(result.isSuccessful());
        Assert.assertFalse(result.isHardFail());
        Assert.assertEquals("Context and comment check found failures:" + System.lineSeparator() +
                "* Source string 'A source string with no errors.' failed check with error: Context and comment strings are both empty."
                + System.lineSeparator(), result.getNotificationText());
    }
}
