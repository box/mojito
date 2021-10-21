package com.box.l10n.mojito.cli.command.checks;

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

    public void executeChecks() {
        ExecutorService executorService = Executors.newFixedThreadPool(cliCheckerList.size());
        List<Future<CliCheckResult>> futures = new ArrayList<>();
        List<CliCheckResult> checkResults = new ArrayList<>();

        //TODO: Refactor below into separate methods
        for(CliChecker checker : cliCheckerList) {
            futures.add(executorService.submit(checker));
        }

        try {
            for(Future<CliCheckResult> future : futures) {
                while(!future.isDone()) {
                    logger.debug("Waiting for checks to complete.");
                    Thread.sleep(500);
                }
                checkResults.add(future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new CliCheckerException(e.getMessage(), true, e);
        }

        for(CliCheckResult result : checkResults) {
            if(!result.isSuccessful()) {
                //TODO: Collate all failures into notificationText & throw CommandError if hard failure exists
            }
        }

    }

    public String getNotificationText() {
        return notificationText;
    }
}
