package com.box.l10n.mojito.cli.checker;

import com.box.l10n.mojito.cli.command.checks.CliCheckerOptions;
import com.box.l10n.mojito.cli.command.checks.PhabricatorRecommendStringIdChecker;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.box.l10n.mojito.regex.PlaceholderRegularExpressions.SINGLE_BRACE_REGEX;

public class PhabricatorRecommendStringIdCheckerTest {

    private PhabricatorRecommendStringIdChecker phabricatorRecommendStringIdChecker;

    @Before
    public void setup() {
        phabricatorRecommendStringIdChecker = new PhabricatorRecommendStringIdChecker();
        phabricatorRecommendStringIdChecker.setCliCheckerOptions(new CliCheckerOptions(Sets.newHashSet(SINGLE_BRACE_REGEX), Sets.newHashSet(), "", ""));
        List<AssetExtractorTextUnit> addedTUs = new ArrayList<>();
        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setSource("A source string with no errors. --- someDir.somefile.testId");
        addedTUs.add(assetExtractorTextUnit);
        List<AssetExtractionDiff> assetExtractionDiffs = new ArrayList<>();
        AssetExtractionDiff assetExtractionDiff = new AssetExtractionDiff();
        assetExtractionDiff.setAddedTextunits(addedTUs);
        assetExtractionDiffs.add(assetExtractionDiff);
        phabricatorRecommendStringIdChecker.setAssetExtractionDiffs(assetExtractionDiffs);
    }

//    @Test
//    public void testSuccess() {
//        phabricatorRecommendStringIdChecker.run();
//    }
}
