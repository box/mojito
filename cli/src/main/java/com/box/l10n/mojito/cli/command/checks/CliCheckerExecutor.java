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

    private String notificationText;

    public CliCheckerExecutor(List<CliChecker> cliCheckerList) {
        this.cliCheckerList = cliCheckerList;
    }

    public boolean executeChecks() {
        ExecutorService executorService = Executors.newFixedThreadPool(cliCheckerList.size());
        List<Future<CliCheckResult>> futures = new ArrayList<>();
        List<CliCheckResult> checkResults = new ArrayList<>();
        try {
            startChecks(executorService, futures);
            retrieveCheckResults(futures, checkResults);
            return analyzeResults(checkResults);
        } catch (InterruptedException | ExecutionException e) {
            throw new CommandException("Failed to retrieve results from check executor: " + e.getMessage(), e);
        } finally {
            executorService.shutdown();
        }
    }

    private boolean analyzeResults(List<CliCheckResult> results) {
        boolean successful = true;
        StringBuilder notificationTextBuilder = new StringBuilder();
        notificationTextBuilder.append("Checks on new source strings failed.");
        notificationTextBuilder.append(System.lineSeparator());
        for(CliCheckResult result : results) {
            if(!result.isSuccessful()) {
                successful = false;
                if (result.isHardFail()) {
                    throw new CommandException("Check " + result.getCheckName() + " failed with error: "
                            + System.lineSeparator() + result.getNotificationText());
                }
                notificationTextBuilder.append(result.getNotificationText());
                notificationTextBuilder.append(System.lineSeparator());
            }
        }
        notificationText = notificationTextBuilder.toString();
        return successful;
    }

    private void retrieveCheckResults(List<Future<CliCheckResult>> futures, List<CliCheckResult> checkResults) throws InterruptedException, ExecutionException {
        for(Future<CliCheckResult> future : futures) {
            while(!future.isDone()) {
                logger.debug("Waiting for checks to complete.");
                Thread.sleep(200);
            }
            checkResults.add(future.get());
        }
    }

    private void startChecks(ExecutorService executorService, List<Future<CliCheckResult>> futures) {
        for(CliChecker checker : cliCheckerList) {
            futures.add(executorService.submit(checker));
        }
    }

    public String getNotificationText() {
        return notificationText;
    }
}
