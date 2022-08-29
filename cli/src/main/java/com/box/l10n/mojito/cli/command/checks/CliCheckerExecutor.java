package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import java.util.List;
import java.util.stream.Collectors;

public class CliCheckerExecutor {

  private final List<AbstractCliChecker> cliCheckerList;

  public CliCheckerExecutor(List<AbstractCliChecker> cliCheckerList) {
    this.cliCheckerList = cliCheckerList;
  }

  public List<CliCheckResult> executeChecks(List<AssetExtractionDiff> assetExtractionDiffs) {
    return cliCheckerList.stream()
        .map(check -> check.run(assetExtractionDiffs))
        .collect(Collectors.toList());
  }
}
