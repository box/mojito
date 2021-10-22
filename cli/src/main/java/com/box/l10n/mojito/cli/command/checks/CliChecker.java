package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.cli.console.ConsoleWriter;

import java.util.List;
import java.util.concurrent.Callable;

public interface CliChecker extends Callable<CliCheckResult> {

    void setCliCheckerOptions(CliCheckerOptions options);

    void setAssetExtractionDiffs(List<AssetExtractionDiff> assetExtractionDiffs);
}
