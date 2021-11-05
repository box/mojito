package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.cli.command.CommandException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class CliCheckerExecutor {

    static Logger logger = LoggerFactory.getLogger(CliCheckerExecutor.class);

    private final List<CliChecker> cliCheckerList;

    private String notificationText;

    public CliCheckerExecutor(List<CliChecker> cliCheckerList) {
        this.cliCheckerList = cliCheckerList;
    }

    public List<String> executeChecks() {
        List<CliCheckResult> failures = runChecks().stream()
                .filter(result -> !result.isSuccessful())
                .collect(Collectors.toList());
        checkForHardFail(failures);
        notificationText = buildNotificationText(failures);
        return failures.stream()
                .map(CliCheckResult::getCheckName)
                .collect(Collectors.toList());
    }

    private void checkForHardFail(List<CliCheckResult> failures) {
        failures.stream().filter(result -> result.isHardFail()).findFirst().map(hardFail -> {
            logger.debug("Hard failure occurred for cli check {} with error {}", hardFail.getCheckName(), hardFail.getNotificationText());
            throw new CommandException("Check " + hardFail.getCheckName() + " failed with error: "
                    + System.lineSeparator() + hardFail.getNotificationText());
        });
    }

    private String buildNotificationText(List<CliCheckResult> failures) {
        StringBuilder notificationTextBuilder = new StringBuilder();
        notificationTextBuilder.append("Checks on new source strings failed.");
        notificationTextBuilder.append(System.lineSeparator());
        failures.stream().forEach(failure -> notificationTextBuilder.append(failure.getNotificationText() + System.lineSeparator()));
        return notificationTextBuilder.toString();
    }

    private List<CliCheckResult> runChecks() {
        return cliCheckerList.stream()
                .map(check -> check.run())
                .collect(Collectors.toList());
    }

    public String getNotificationText() {
        return notificationText;
    }
}
