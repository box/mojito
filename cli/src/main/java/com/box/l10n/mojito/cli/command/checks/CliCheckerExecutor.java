package com.box.l10n.mojito.cli.command.checks;

import java.util.List;
import java.util.stream.Collectors;

public class CliCheckerExecutor {

    private final List<CliChecker> cliCheckerList;

    public CliCheckerExecutor(List<CliChecker> cliCheckerList) {
        this.cliCheckerList = cliCheckerList;
    }

    public List<CliCheckResult> executeChecks() {
        return cliCheckerList.stream()
                .map(check -> check.run())
                .collect(Collectors.toList());
    }
}
