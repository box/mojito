package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

public class ControlCharacterCheckerTest {

    ControlCharacterChecker controlCharacterChecker;
    private List<AssetExtractionDiff> assetExtractionDiffs;

    @Before
    public void setup() {
        controlCharacterChecker = new ControlCharacterChecker();
        controlCharacterChecker.setCliCheckerOptions(new CliCheckerOptions(Sets.newHashSet(), Sets.newHashSet(), ImmutableMap.<String, String>builder().build()));
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setSource("\u0010Text containing a control character.");
        addedTUs.add(assetExtractorTextUnit);
        assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
    }

    @Test
    public void testFailure() {
        CliCheckResult result = controlCharacterChecker.run(assetExtractionDiffs);
        assertFalse(result.isSuccessful());
        assertTrue(result.getNotificationText().contains("\u0010Text containing a control character."));
        assertTrue(result.getNotificationText().contains(" at index 0."));
    }

    @Test
    public void testMultipleControlCharactersInString() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setSource("\u0010Text containing multiple \u0000 control characters.\u0007");
        addedTUs.add(assetExtractorTextUnit);
        assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);

        CliCheckResult result = controlCharacterChecker.run(assetExtractionDiffs);
        assertFalse(result.isSuccessful());
        assertTrue(result.getNotificationText().contains("\u0010Text containing multiple \u0000 control characters.\u0007"));
        assertTrue(result.getNotificationText().contains(" at index 0, 26, 47."));
    }

    @Test
    public void testNegatedCharactersInString() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setSource("\tText containing multiple \n control characters.\r");
        addedTUs.add(assetExtractorTextUnit);
        assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);

        CliCheckResult result = controlCharacterChecker.run(assetExtractionDiffs);
        assertTrue(result.isSuccessful());
    }

    @Test
    public void testSuccess() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setSource("Text containing no control character.");
        addedTUs.add(assetExtractorTextUnit);
        assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);

        CliCheckResult result = controlCharacterChecker.run(assetExtractionDiffs);
        assertTrue(result.isSuccessful());
    }
}
