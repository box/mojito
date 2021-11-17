package com.box.l10n.mojito.cli.checks;

import com.box.l10n.mojito.cli.command.checks.CliCheckResult;
import com.box.l10n.mojito.cli.command.checks.CliCheckerOptions;
import com.box.l10n.mojito.cli.command.checks.RecommendStringIdChecker;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.box.l10n.mojito.regex.PlaceholderRegularExpressions.SINGLE_BRACE_REGEX;

public class RecommendStringIdCheckerTest {

    private RecommendStringIdChecker recommendStringIdChecker;

    @Before
    public void setup() {
        recommendStringIdChecker = new RecommendStringIdChecker();
        List<String> modifiedFiles = new ArrayList<>(Arrays.asList("target/test-classes/com/box/l10n/mojito/cli/phab-string-id-checker/someDir/someOtherDir/evenDeeperDir/test1.txt",
                "target/test-classes/com/box/l10n/mojito/cli/phab-string-id-checker/someDir/someOtherDir/test2.txt"));
        recommendStringIdChecker.setCliCheckerOptions(new CliCheckerOptions(Sets.newHashSet(SINGLE_BRACE_REGEX), Sets.newHashSet(),
                "", ""));
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("A source string with no errors. --- someDir.someSubDir.someStringId");
        assetExtractorTextUnit.setSource("A source string with no errors.");
        assetExtractorTextUnit.setUsages(Sets.newHashSet("someDir/someSubDir/someSourceFile.java"));
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        recommendStringIdChecker.setAssetExtractionDiffs(assetExtractionDiffs);
    }

    @Test
    public void testSuccess() {
        CliCheckResult result = recommendStringIdChecker.run();
        Assert.assertTrue(result.isSuccessful());
        Assert.assertTrue(result.getNotificationText().isEmpty());
    }

    @Test
    public void testFailure() {
        setTextUnitId("incorrect.prefix.someStringId");
        CliCheckResult result = recommendStringIdChecker.run();
        Assert.assertFalse(result.isSuccessful());
        Assert.assertEquals("Recommended id updates for the following strings:" + System.lineSeparator()
                + "* Please update id for string 'A source string with no errors.' to be prefixed with 'someDir.someSubDir.'" + System.lineSeparator(), result.getNotificationText());
    }

    @Test
    public void testEmptyMsgContextPassesWithoutRecommendation() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("A source string with no errors.");
        assetExtractorTextUnit.setSource("A source string with no errors.");
        assetExtractorTextUnit.setUsages(Sets.newHashSet("someDir/someSubDir/anotherLevel/evenDeeper/someSourceFile.java:1497"));
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        recommendStringIdChecker.setAssetExtractionDiffs(assetExtractionDiffs);

        CliCheckResult result = recommendStringIdChecker.run();
        Assert.assertTrue(result.isSuccessful());
        Assert.assertTrue(result.getNotificationText().isEmpty());
    }

    @Test
    public void testRecommendedUpdatesAreLimitedToADepthOfTwo() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("A source string with no errors. --- someStringId");
        assetExtractorTextUnit.setSource("A source string with no errors.");
        assetExtractorTextUnit.setUsages(Sets.newHashSet("someDir/someSubDir/anotherLevel/evenDeeper/someSourceFile.java:1497"));
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        recommendStringIdChecker.setAssetExtractionDiffs(assetExtractionDiffs);

        CliCheckResult result = recommendStringIdChecker.run();
        Assert.assertFalse(result.isSuccessful());
        Assert.assertEquals("Recommended id updates for the following strings:" + System.lineSeparator()
                + "* Please update id for string 'A source string with no errors.' to be prefixed with 'someDir.someSubDir.'" + System.lineSeparator(), result.getNotificationText());
    }

    @Test
    public void testTopLevelFileRecommendedWithRootPrefix() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("A source string with no errors. --- someStringId");
        assetExtractorTextUnit.setSource("A source string with no errors.");
        assetExtractorTextUnit.setUsages(Sets.newHashSet("someSourceFile.java:123"));
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        recommendStringIdChecker.setAssetExtractionDiffs(assetExtractionDiffs);

        CliCheckResult result = recommendStringIdChecker.run();
        Assert.assertFalse(result.isSuccessful());
        Assert.assertEquals("Recommended id updates for the following strings:" + System.lineSeparator()
                + "* Please update id for string 'A source string with no errors.' to be prefixed with 'root.'" + System.lineSeparator(), result.getNotificationText());
    }

    @Test
    public void testEmptyUsagesSetPassesCheck() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("A source string with no errors. --- someDir.someSubDir.someStringId");
        assetExtractorTextUnit.setSource("A source string with no errors.");
        assetExtractorTextUnit.setUsages(Sets.newHashSet());
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        recommendStringIdChecker.setAssetExtractionDiffs(assetExtractionDiffs);

        CliCheckResult result = recommendStringIdChecker.run();
        Assert.assertTrue(result.isSuccessful());
        Assert.assertTrue(result.getNotificationText().isEmpty());
    }

    @Test
    public void testMultipleRecommendations() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("A source string with no errors. --- someStringId");
        assetExtractorTextUnit.setSource("A source string with no errors.");
        assetExtractorTextUnit.setUsages(Sets.newHashSet("someSourceFile.java"));
        addedTUs.add(assetExtractorTextUnit);
        AssetExtractorTextUnit assetExtractorTextUnit2 = new AssetExtractorTextUnit();
        assetExtractorTextUnit2.setName("Another source string with no errors. --- someOtherStringId");
        assetExtractorTextUnit2.setSource("Another source string with no errors.");
        assetExtractorTextUnit2.setUsages(Sets.newHashSet("someDir/someSubDir/someOtherSourceFile.java:2"));
        addedTUs.add(assetExtractorTextUnit2);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        recommendStringIdChecker.setAssetExtractionDiffs(assetExtractionDiffs);

        CliCheckResult result = recommendStringIdChecker.run();
        Assert.assertFalse(result.isSuccessful());
        Assert.assertEquals("Recommended id updates for the following strings:" + System.lineSeparator()
                + "* Please update id for string 'A source string with no errors.' to be prefixed with 'root.'" + System.lineSeparator()
                + "* Please update id for string 'Another source string with no errors.' to be prefixed with 'someDir.someSubDir.'" + System.lineSeparator(), result.getNotificationText());
    }

    private void setTextUnitId(String id) {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("A source string with no errors. --- " + id);
        assetExtractorTextUnit.setSource("A source string with no errors.");
        assetExtractorTextUnit.setUsages(Sets.newHashSet("someDir/someSubDir/evenDeeperSubDir/someSourceFile.java:1111"));
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        recommendStringIdChecker.setAssetExtractionDiffs(assetExtractionDiffs);
    }
}
