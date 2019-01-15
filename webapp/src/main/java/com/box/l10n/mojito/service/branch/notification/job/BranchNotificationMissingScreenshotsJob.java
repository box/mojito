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

import java.util.Date;

/**
 * Send the screenshot missing notification for a branch if needed.
 *
 * @author jaurambault
 */
@Component
@DisallowConcurrentExecution
public class BranchNotificationMissingScreenshotsJob extends SchedulableJob {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(BranchNotificationMissingScreenshotsJob.class);

    static final String BRANCH_ID = "branchId";

    @Autowired
    BranchNotificationService branchNotificationService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long branchId = context.getMergedJobDataMap().getLong("branchId");
        logger.debug("execute for branchId: {}", branchId);
        branchNotificationService.sendMissingScreenshotNotificationForBranch(branchId);
    }

    @Override
    protected String getDescription() {
        return "Send the screenshot missing notification for a branch if applicable";
    }

    public void schedule(Long branchId, Date triggerStartDate) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(BRANCH_ID, branchId.toString());
        schedule(jobDataMap, triggerStartDate, BRANCH_ID);
    }
}
