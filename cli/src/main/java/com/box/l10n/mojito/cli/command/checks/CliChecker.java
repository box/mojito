package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;

import java.io.IOException;
import java.util.List;

public interface CliChecker {

    CliCheckResult run();

    void setCliCheckerOptions(CliCheckerOptions options);

    void setAssetExtractionDiffs(List<AssetExtractionDiff> assetExtractionDiffs);
}
