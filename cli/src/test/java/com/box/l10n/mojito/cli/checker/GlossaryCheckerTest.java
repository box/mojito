package com.box.l10n.mojito.cli.checker;

import com.box.l10n.mojito.cli.command.checks.CliCheckResult;
import com.box.l10n.mojito.cli.command.checks.CliCheckerOptions;
import com.box.l10n.mojito.cli.command.checks.GlossaryChecker;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class GlossaryCheckerTest {

    private GlossaryChecker glossaryChecker;

    @Before
    public void setup() {
        glossaryChecker = new GlossaryChecker();
        glossaryChecker.setCliCheckerOptions(new CliCheckerOptions(Sets.newHashSet(), Sets.newHashSet(), "", "target/test-classes/com/box/l10n/mojito/cli/glossarychecker/glossary.json"));
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setSource("A source string with Pinterest in it.");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        glossaryChecker.setAssetExtractionDiffs(assetExtractionDiffs);
    }

    @Test
    public void testSuccess() {
        CliCheckResult result = glossaryChecker.run();
        Assert.assertTrue(result.isSuccessful());
    }

    @Test
    public void testFailure() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setSource("A source string pinterest in it.");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        glossaryChecker.setAssetExtractionDiffs(assetExtractionDiffs);
        CliCheckResult result = glossaryChecker.run();
        Assert.assertFalse(result.isSuccessful());
    }

    @Test
    public void testMajorErrorContainsMajorInNotificationString() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setSource("A source string with pinterest in it.");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        glossaryChecker.setAssetExtractionDiffs(assetExtractionDiffs);
        CliCheckResult result = glossaryChecker.run();
        Assert.assertFalse(result.isSuccessful());
        Assert.assertTrue(result.getNotificationText().contains("MAJOR"));
    }

    @Test
    public void testMultipleGlossaryTermsInString() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setSource("A source string with pinterest and ads Manager in it.");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        glossaryChecker.setAssetExtractionDiffs(assetExtractionDiffs);
        CliCheckResult result = glossaryChecker.run();
        Assert.assertFalse(result.isSuccessful());
        Assert.assertEquals("Glossary check failures:"
                + System.lineSeparator() + "\t* MAJOR: String 'A source string with pinterest and ads Manager in it.' contains glossary term 'Pinterest' which must match case exactly." + System.lineSeparator()
                + "\t* WARN: String 'A source string with pinterest and ads Manager in it.' contains glossary term 'Ads Manager' but does not exactly match the glossary term case."
                + System.lineSeparator() + System.lineSeparator(), result.getNotificationText());
    }

    @Test
    public void testGlossaryTermInStringWithAdditionalSpaces() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setSource("A source string with ads                Manager in it.");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        glossaryChecker.setAssetExtractionDiffs(assetExtractionDiffs);
        CliCheckResult result = glossaryChecker.run();
        Assert.assertEquals("Glossary check failures:"
                + System.lineSeparator() + "\t* WARN: String 'A source string with ads                Manager in it.' contains glossary term 'Ads Manager' but does not exactly match the glossary term case."
                + System.lineSeparator() + System.lineSeparator(), result.getNotificationText());
    }

    @Test
    public void testStringWithNoGlossaryTerms() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setSource("A source string with no glossary terms in it.");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        glossaryChecker.setAssetExtractionDiffs(assetExtractionDiffs);
        CliCheckResult result = glossaryChecker.run();
        Assert.assertTrue(result.isSuccessful());
    }

    @Test
    public void testMinorFailuresDontCauseOverallResultFail() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setSource("A source string with ads Manager in it.");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        glossaryChecker.setAssetExtractionDiffs(assetExtractionDiffs);
        CliCheckResult result = glossaryChecker.run();
        Assert.assertTrue(result.isSuccessful());
        Assert.assertEquals("Glossary check failures:"
                + System.lineSeparator() + "\t* WARN: String 'A source string with ads Manager in it.' contains glossary term 'Ads Manager' but does not exactly match the glossary term case."
                + System.lineSeparator() + System.lineSeparator(), result.getNotificationText());
    }

}
