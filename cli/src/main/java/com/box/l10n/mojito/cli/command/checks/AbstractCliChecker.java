package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public abstract class AbstractCliChecker implements CliChecker {

    protected CliCheckerOptions cliCheckerOptions;

    protected List<AssetExtractionDiff> assetExtractionDiffs;

    public boolean isHardFail() {
        return cliCheckerOptions.getHardFailureSet().contains(this.getClass().getName());
    }

    public void setCliCheckerOptions(CliCheckerOptions options) {
        this.cliCheckerOptions = options;
    }

    public void setAssetExtractionDiffs(List<AssetExtractionDiff> assetExtractionDiffs) {
        this.assetExtractionDiffs = assetExtractionDiffs;
    }

    protected List<AssetExtractorTextUnit> getAddedTextUnits() {
        List<AssetExtractorTextUnit> addedTextUnits = new ArrayList<>();
        assetExtractionDiffs.stream().forEach(assetExtractionDiff -> addedTextUnits.addAll(assetExtractionDiff.getAddedTextunits()));
        return addedTextUnits;
    }

    protected List<Pattern> getPatterns() {
        List<Pattern> patterns = new ArrayList<>();
        cliCheckerOptions.getParameterRegexSet().stream().forEach(regex -> patterns.add(Pattern.compile(regex)));
        return patterns;
    }
}
