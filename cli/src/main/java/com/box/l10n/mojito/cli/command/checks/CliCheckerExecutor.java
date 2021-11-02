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

public class CliCheckerExecutor {

    static Logger logger = LoggerFactory.getLogger(CliCheckerExecutor.class);

    private final List<CliChecker> cliCheckerList;

    private final ExecutorService executorService;

    private String notificationText;

    public CliCheckerExecutor(List<CliChecker> cliCheckerList, int numOfThreads) {
        this.cliCheckerList = cliCheckerList;
        this.executorService = Executors.newFixedThreadPool(numOfThreads);
    }

    public List<String> executeChecks() {
        try {
            List<Future<CliCheckResult>> futures = startChecks(executorService);
            return analyzeResults(retrieveCheckResults(futures));
        } catch (InterruptedException | ExecutionException e) {
            throw new CommandException("Failed to retrieve results from check executor: " + e.getMessage(), e);
        } finally {
            executorService.shutdown();
        }
    }

    private List<String> analyzeResults(List<CliCheckResult> results) {
        List<String> failedChecks = new ArrayList<>();
        StringBuilder notificationTextBuilder = new StringBuilder();
        notificationTextBuilder.append("Checks on new source strings failed.");
        notificationTextBuilder.append(System.lineSeparator());
        for(CliCheckResult result : results) {
            if(!result.isSuccessful()) {
                failedChecks.add(result.getCheckName());
                if (result.isHardFail()) {
                    throw new CommandException("Check " + result.getCheckName() + " failed with error: "
                            + System.lineSeparator() + result.getNotificationText());
                }
                notificationTextBuilder.append(result.getNotificationText());
                notificationTextBuilder.append(System.lineSeparator());
            }
        }
        notificationText = notificationTextBuilder.toString();
        return failedChecks;
    }

    private List<CliCheckResult> retrieveCheckResults(List<Future<CliCheckResult>> futures) throws InterruptedException, ExecutionException {
        List<CliCheckResult> checkResults = new ArrayList<>();
        for(Future<CliCheckResult> future : futures) {
            while(!future.isDone()) {
                logger.debug("Waiting for checks to complete.");
                Thread.sleep(200);
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
