package com.box.l10n.mojito.cli.command.checks;

import com.box.l10n.mojito.cli.command.CommandException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class CliCheckerExecutor {

    static Logger logger = LoggerFactory.getLogger(CliCheckerExecutor.class);

    private final List<CliChecker> cliCheckerList;

    private final int numOfThreads;

    private String notificationText;

    public CliCheckerExecutor(List<CliChecker> cliCheckerList, int numOfThreads) {
        this.cliCheckerList = cliCheckerList;
        this.numOfThreads = numOfThreads;
    }

    public List<String> executeChecks() {
        ExecutorService executorService = Executors.newFixedThreadPool(numOfThreads);
        try {
            List<Future<CliCheckResult>> futures = startChecks(executorService);
            List<CliCheckResult> failures = retrieveCheckResults(futures).stream()
                    .filter(result -> !result.isSuccessful()).collect(Collectors.toList());
            checkForHardFail(failures);
            notificationText = buildNotificationText(failures);
            return failures.stream()
                    .map(CliCheckResult::getCheckName)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            throw new CommandException("Failed to retrieve results from check executor: " + e.getMessage(), e);
        } finally {
            executorService.shutdown();
        }
    }

    private void checkForHardFail(List<CliCheckResult> failures) {
        for(CliCheckResult failure : failures) {
            if(failure.isHardFail()) {
                throw new CommandException("Check " + failure.getCheckName() + " failed with error: "
                        + System.lineSeparator() + failure.getNotificationText());
            }
        }
    }

    private String buildNotificationText(List<CliCheckResult> failures) {
        StringBuilder notificationTextBuilder = new StringBuilder();
        notificationTextBuilder.append("Checks on new source strings failed.");
        notificationTextBuilder.append(System.lineSeparator());
        failures.stream().forEach(failure -> notificationTextBuilder.append(failure.getNotificationText() + System.lineSeparator()));
        return notificationTextBuilder.toString();
    }

    private List<CliCheckResult> retrieveCheckResults(List<Future<CliCheckResult>> futures) throws InterruptedException, ExecutionException {
        List<CliCheckResult> checkResults = new ArrayList<>();
        for(Future<CliCheckResult> future : futures) {
            while(!future.isDone()) {
                logger.debug("Waiting for checks to complete.");
                Thread.sleep(100);
            }
            checkResults.add(future.get());
        }
        return checkResults;
    }

    private List<Future<CliCheckResult>> startChecks(ExecutorService executorService) {
        List<Future<CliCheckResult>> futures = new ArrayList<>();
        for(CliChecker checker : cliCheckerList) {
            futures.add(executorService.submit(checker));
        }
        return futures;
    }

    public String getNotificationText() {
        return notificationText;
    }
}
