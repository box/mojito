package com.box.l10n.mojito.service.branch.notification.job;

import com.box.l10n.mojito.quartz.QuartzPollableJob;
import com.box.l10n.mojito.service.branch.notification.BranchNotificationService;
import org.quartz.DisallowConcurrentExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Job that sends notifications for a branch. */
@Component
@DisallowConcurrentExecution
public class BranchNotificationJob extends QuartzPollableJob<BranchNotificationJobInput, Void> {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(BranchNotificationJob.class);

  @Autowired BranchNotificationService branchNotificationService;

  @Override
  public Void call(BranchNotificationJobInput input) throws Exception {
    Long branchId = input.getBranchId();
    logger.debug("execute for branchId: {}", branchId);
    branchNotificationService.sendNotificationsForBranch(branchId);
    return null;
  }
}
