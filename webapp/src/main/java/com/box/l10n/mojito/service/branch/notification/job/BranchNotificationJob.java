package com.box.l10n.mojito.service.branch.notification.job;

import com.box.l10n.mojito.service.branch.notification.BranchNotificationService;
import com.box.l10n.mojito.service.scheduler.SchedulableJob;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Job that sends notifications for a branch.
 */
@Component
@DisallowConcurrentExecution
public class BranchNotificationJob extends SchedulableJob {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(BranchNotificationJob.class);

    static final String BRANCH_ID = "branchId";

    @Autowired
    BranchNotificationService branchNotificationService;

    @Override
    protected String getDescription() {
        return "Sends notifications for a branch";
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long branchId = context.getMergedJobDataMap().getLong(BRANCH_ID);
        logger.debug("execute for branchId: {}", branchId);
        branchNotificationService.sendNotificationsForBranch(branchId);
    }

    public void schedule(Long branchId) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(BRANCH_ID, branchId.toString());
        schedule(jobDataMap, BRANCH_ID);
    }
}
