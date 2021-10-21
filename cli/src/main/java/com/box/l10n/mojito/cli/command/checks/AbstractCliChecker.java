package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;

import java.util.List;

public abstract class AbstractCliChecker implements CliChecker {

    protected CliCheckerOptions cliCheckerOptions;

    protected List<AssetExtractionDiff> assetExtractionDiffs;

    protected boolean hardFail;

    public boolean isHardFail() {
        return hardFail;
    }

    public void setCliCheckerOptions(CliCheckerOptions options) {
        this.cliCheckerOptions = options;
    }

    public void setAssetExtractionDiffs(List<AssetExtractionDiff> assetExtractionDiffs) {
        this.assetExtractionDiffs = assetExtractionDiffs;
    }

}
