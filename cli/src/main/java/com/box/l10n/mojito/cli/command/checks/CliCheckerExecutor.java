package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.cli.command.CommandException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class CliCheckerExecutor {

    static Logger logger = LoggerFactory.getLogger(CliCheckerExecutor.class);

    private final List<CliChecker> cliCheckerList;

    private String notificationText;

    public CliCheckerExecutor(List<CliChecker> cliCheckerList) {
        this.cliCheckerList = cliCheckerList;
    }

    public List<String> executeChecks() {
        List<CliCheckResult> results = runChecks();
        notificationText = buildNotificationText(results);
        checkForHardFail(results);
        return results.stream()
                .filter(result -> !result.isSuccessful())
                .map(CliCheckResult::getCheckName)
                .collect(Collectors.toList());
    }

    private void checkForHardFail(List<CliCheckResult> results) {
        AtomicBoolean hardFail = new AtomicBoolean(false);
        StringBuilder hardFailureListString = new StringBuilder();
        hardFailureListString.append("The following checks had hard failures:" + System.lineSeparator());
        results.stream().filter(result -> !result.isSuccessful() && result.isHardFail()).map(CliCheckResult::getCheckName).forEach(failure -> {
            hardFail.set(true);
            hardFailureListString.append("\t * " + failure);
            hardFailureListString.append(System.lineSeparator());
        });
        if(hardFail.get()) {
            logger.debug(hardFailureListString.toString());
            throw new CommandException(hardFailureListString
                    + System.lineSeparator() + System.lineSeparator() +
                    notificationText);
        }
    }

    private String buildNotificationText(List<CliCheckResult> results) {
        StringBuilder notificationTextBuilder = new StringBuilder();
        notificationTextBuilder.append("Checks on new source strings failed.");
        notificationTextBuilder.append(System.lineSeparator());
        results.stream().forEach(result -> notificationTextBuilder.append(result.getNotificationText() + System.lineSeparator()));
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
