package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.box.l10n.mojito.cli.command.checks.CliCheckerParameters.RECOMMEND_STRING_ID_LABEL_IGNORE_PATTERN_KEY;
import static com.box.l10n.mojito.cli.command.extractioncheck.ExtractionCheckNotificationSender.QUOTE_MARKER;
import static com.box.l10n.mojito.regex.PlaceholderRegularExpressions.SINGLE_BRACE_REGEX;

public class RecommendStringIdCheckerTest {

    private RecommendStringIdChecker recommendStringIdChecker;

    private List<AssetExtractionDiff> assetExtractionDiffs;

    @Before
    public void setup() {
        recommendStringIdChecker = new RecommendStringIdChecker();
        List<String> modifiedFiles = new ArrayList<>(Arrays.asList("someDir/someOtherDir/evenDeeperDir/test1.txt", "someDir/someOtherDir/test2.txt"));
        recommendStringIdChecker.setCliCheckerOptions(new CliCheckerOptions(Sets.newHashSet(SINGLE_BRACE_REGEX), Sets.newHashSet(),
                ImmutableMap.<String, String>builder().build()));
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("A source string with no errors. --- someDir.someSubDir.someStringId");
        assetExtractorTextUnit.setSource("A source string with no errors.");
        assetExtractorTextUnit.setUsages(Sets.newHashSet("someDir/someSubDir/someSourceFile.java"));
        addedTUs.add(assetExtractorTextUnit);
        assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);

    }

    @Test
    public void testSuccess() {
        CliCheckResult result = recommendStringIdChecker.run(assetExtractionDiffs);
        Assert.assertTrue(result.isSuccessful());
        Assert.assertTrue(result.getNotificationText().isEmpty());
    }

    @Test
    public void testFailure() {
        CliCheckResult result = recommendStringIdChecker.run(setTextUnitId("incorrect.prefix.someStringId"));
        Assert.assertFalse(result.isSuccessful());
        Assert.assertEquals("Recommended id updates for the following strings:" + System.lineSeparator()
                + "* Please update id " + QUOTE_MARKER + "incorrect.prefix.someStringId" + QUOTE_MARKER + " for string " + QUOTE_MARKER + "A source string with no errors." + QUOTE_MARKER + " to be prefixed with 'someDir.someSubDir.'" + System.lineSeparator(), result.getNotificationText());
    }

    @Test
    public void testMultipleUsagesProvided() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("A source string with no errors.  --- anotherDir.someSubDir.stringId");
        assetExtractorTextUnit.setSource("A source string with no errors.");
        assetExtractorTextUnit.setUsages(Sets.newHashSet("someDir/someSubDir/anotherLevel/evenDeeper/someSourceFile.java:1497", "anotherDir/someSubDir/anotherLevel/evenDeeper/someSourceFile.java:1497"));
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);

        CliCheckResult result = recommendStringIdChecker.run(assetExtractionDiffs);
        Assert.assertTrue(result.isSuccessful());
        Assert.assertTrue(result.getNotificationText().isEmpty());
    }

    @Test
    public void testRecommendationWhenMultipleUsagesProvided() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("A source string with no errors. --- aDifferentDir.someSubDir.stringId");
        assetExtractorTextUnit.setSource("A source string with no errors.");
        assetExtractorTextUnit.setUsages(Sets.newHashSet("someDir/someSubDir/anotherLevel/evenDeeper/someSourceFile.java:1497", "anotherDir/someSubDir/anotherLevel/evenDeeper/someSourceFile.java:1497"));
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);

        CliCheckResult result = recommendStringIdChecker.run(assetExtractionDiffs);
        Assert.assertFalse(result.isSuccessful());
        Assert.assertTrue(result.getNotificationText().contains("someDir.someSubDir.") || result.getNotificationText().contains("anotherDir.someSubDir."));
    }

    @Test
    public void testEmptyMsgContextReceivesRecommendation() {
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

        CliCheckResult result = recommendStringIdChecker.run(assetExtractionDiffs);
        Assert.assertFalse(result.isSuccessful());
        Assert.assertTrue(result.getNotificationText().contains("someDir.someSubDir."));
    }

    @Test
    public void testAbsolutePathAsUsage() {
        String cwd = Paths.get(".").toAbsolutePath().toString();
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("A source string with no errors. --- someDir.someSubDir.someStringId");
        assetExtractorTextUnit.setSource("A source string with no errors.");
        assetExtractorTextUnit.setUsages(Sets.newHashSet(cwd.replace(".", "") + "someDir/someSubDir/anotherLevel/evenDeeper/someSourceFile.java:1497"));
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);

        CliCheckResult result = recommendStringIdChecker.run(assetExtractionDiffs);
        Assert.assertTrue(result.isSuccessful());
        Assert.assertTrue(result.getNotificationText().isEmpty());
    }

    @Test
    public void testAbsolutePathAsUsageWithNoContext() {
        String cwd = Paths.get(".").toAbsolutePath().toString();
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("A source string with no errors. ");
        assetExtractorTextUnit.setSource("A source string with no errors.");
        assetExtractorTextUnit.setUsages(Sets.newHashSet(cwd.replace(".", "") + "someDir/someSubDir/anotherLevel/evenDeeper/someSourceFile.java:1497"));
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);

        CliCheckResult result = recommendStringIdChecker.run(assetExtractionDiffs);
        Assert.assertFalse(result.isSuccessful());
        Assert.assertTrue(result.getNotificationText().contains("someDir.someSubDir."));
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

        CliCheckResult result = recommendStringIdChecker.run(assetExtractionDiffs);
        Assert.assertFalse(result.isSuccessful());
        Assert.assertEquals("Recommended id updates for the following strings:" + System.lineSeparator()
                + "* Please update id " + QUOTE_MARKER + "someStringId" + QUOTE_MARKER + " for string " + QUOTE_MARKER + "A source string with no errors." + QUOTE_MARKER + " to be prefixed with 'someDir.someSubDir.'" + System.lineSeparator(), result.getNotificationText());
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

        CliCheckResult result = recommendStringIdChecker.run(assetExtractionDiffs);
        Assert.assertFalse(result.isSuccessful());
        Assert.assertEquals("Recommended id updates for the following strings:" + System.lineSeparator()
                + "* Please update id " + QUOTE_MARKER + "someStringId" + QUOTE_MARKER + " for string " + QUOTE_MARKER + "A source string with no errors." + QUOTE_MARKER + " to be prefixed with 'root.'" + System.lineSeparator(), result.getNotificationText());
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

        CliCheckResult result = recommendStringIdChecker.run(assetExtractionDiffs);
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

        CliCheckResult result = recommendStringIdChecker.run(assetExtractionDiffs);
        Assert.assertFalse(result.isSuccessful());
        Assert.assertEquals("Recommended id updates for the following strings:" + System.lineSeparator()
                + "* Please update id " + QUOTE_MARKER + "someStringId" + QUOTE_MARKER + " for string " + QUOTE_MARKER + "A source string with no errors." + QUOTE_MARKER + " to be prefixed with 'root.'" + System.lineSeparator()
                + "* Please update id " + QUOTE_MARKER + "someOtherStringId" + QUOTE_MARKER + " for string " + QUOTE_MARKER + "Another source string with no errors." + QUOTE_MARKER + " to be prefixed with 'someDir.someSubDir.'" + System.lineSeparator(), result.getNotificationText());
    }

    @Test
    public void testLabelIsIgnored() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("A source string with no errors. --- [aLabel] someStringId");
        assetExtractorTextUnit.setSource("A source string with no errors.");
        assetExtractorTextUnit.setUsages(Sets.newHashSet("someSourceFile.java"));
        addedTUs.add(assetExtractorTextUnit);
        AssetExtractorTextUnit assetExtractorTextUnit2 = new AssetExtractorTextUnit();
        assetExtractorTextUnit2.setName("Another source string with no errors. --- [aLabel] someOtherStringId [aLabel]");
        assetExtractorTextUnit2.setSource("Another source string with no errors.");
        assetExtractorTextUnit2.setUsages(Sets.newHashSet("someDir/someSubDir/someOtherSourceFile.java:2"));
        addedTUs.add(assetExtractorTextUnit2);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);

        recommendStringIdChecker.setCliCheckerOptions(new CliCheckerOptions(Sets.newHashSet(SINGLE_BRACE_REGEX), Sets.newHashSet(),
                ImmutableMap.<String, String>builder().put(RECOMMEND_STRING_ID_LABEL_IGNORE_PATTERN_KEY.getKey(), "\\[\\w+\\]").build()));

        CliCheckResult result = recommendStringIdChecker.run(assetExtractionDiffs);
        Assert.assertFalse(result.isSuccessful());
        Assert.assertFalse(result.getNotificationText().contains("[aLabel]"));
        Assert.assertEquals("Recommended id updates for the following strings:" + System.lineSeparator()
                + "* Please update id " + QUOTE_MARKER + "someStringId" + QUOTE_MARKER + " for string " + QUOTE_MARKER + "A source string with no errors." + QUOTE_MARKER + " to be prefixed with 'root.'" + System.lineSeparator()
                + "* Please update id " + QUOTE_MARKER + "someOtherStringId" + QUOTE_MARKER + " for string " + QUOTE_MARKER + "Another source string with no errors." + QUOTE_MARKER + " to be prefixed with 'someDir.someSubDir.'" + System.lineSeparator(), result.getNotificationText());
    }

    private List<AssetExtractionDiff> setTextUnitId(String id) {
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
        return assetExtractionDiffs;
    }
}
