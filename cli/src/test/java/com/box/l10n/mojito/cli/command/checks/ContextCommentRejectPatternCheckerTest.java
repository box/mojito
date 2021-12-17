package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.cli.command.CommandException;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.box.l10n.mojito.regex.PlaceholderRegularExpressions.PLACEHOLDER_NO_SPECIFIER_REGEX;

public class ContextCommentRejectPatternCheckerTest {

    private ContextCommentRejectPatternChecker contextCommentRejectPatternChecker;

    private List<AssetExtractionDiff> assetExtractionDiffs;

    @Before
    public void setup() {
        this.contextCommentRejectPatternChecker = new ContextCommentRejectPatternChecker();
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("A source string with no errors. --- a context string");
        assetExtractorTextUnit.setSource("A source string with no errors.");
        assetExtractorTextUnit.setComments("A comment string");
        addedTUs.add(assetExtractorTextUnit);
        assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        contextCommentRejectPatternChecker.setCliCheckerOptions(new CliCheckerOptions(Sets.newHashSet(PLACEHOLDER_NO_SPECIFIER_REGEX), Sets.newHashSet(), "", "", null, null,"^ *- *$|^ *-- *$"));
    }

    @Test
    public void testSuccess() {
        CliCheckResult result = contextCommentRejectPatternChecker.run(assetExtractionDiffs);
        Assert.assertTrue(result.isSuccessful());
        Assert.assertTrue(result.getNotificationText().isEmpty());
    }

    @Test
    public void testFailure() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("A source string with no errors. --- - ");
        assetExtractorTextUnit.setSource("A source string with no errors.");
        assetExtractorTextUnit.setComments(" -- ");
        addedTUs.add(assetExtractorTextUnit);
        assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        CliCheckResult result = contextCommentRejectPatternChecker.run(assetExtractionDiffs);
        Assert.assertFalse(result.isSuccessful());
        Assert.assertFalse(result.getNotificationText().isEmpty());
        Assert.assertTrue(result.getNotificationText().contains("Context and Comment Pattern check failed for regex '^ *- *$|^ *-- *$':"));
        Assert.assertTrue(result.getNotificationText().contains("* Source string 'A source string with no errors.' has an invalid context or comment string."));
    }

    @Test(expected = CommandException.class)
    public void testEmptyPattern() {
        contextCommentRejectPatternChecker.setCliCheckerOptions(new CliCheckerOptions(Sets.newHashSet(PLACEHOLDER_NO_SPECIFIER_REGEX), Sets.newHashSet(), "", "", null, null,null));
        contextCommentRejectPatternChecker.run(assetExtractionDiffs);
    }

    @Test
    public void testNullComment() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("A source string with no errors. --- a context string");
        assetExtractorTextUnit.setSource("A source string with no errors.");
        assetExtractorTextUnit.setComments(null);
        addedTUs.add(assetExtractorTextUnit);
        assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        CliCheckResult result = contextCommentRejectPatternChecker.run(assetExtractionDiffs);
        Assert.assertTrue(result.isSuccessful());
        Assert.assertTrue(result.getNotificationText().isEmpty());
    }

    @Test
    public void testNoContext() {
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("A source string with no errors.");
        assetExtractorTextUnit.setSource("A source string with no errors.");
        assetExtractorTextUnit.setComments("A comment string");
        addedTUs.add(assetExtractorTextUnit);
        assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        CliCheckResult result = contextCommentRejectPatternChecker.run(assetExtractionDiffs);
        Assert.assertTrue(result.isSuccessful());
        Assert.assertTrue(result.getNotificationText().isEmpty());
    }
}
