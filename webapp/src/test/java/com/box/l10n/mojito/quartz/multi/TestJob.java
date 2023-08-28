package com.box.l10n.mojito.quartz.multi;

import org.quartz.Job;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestJob implements Job {

  private static volatile String executingScheduler;

  Logger logger = LoggerFactory.getLogger(TestJob.class);

  @Override
  public void execute(org.quartz.JobExecutionContext context)
      throws org.quartz.JobExecutionException {
    logger.info("Executing test job");
    try {
      executingScheduler = context.getScheduler().getSchedulerName();
    } catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getExecutingScheduler() {
    return executingScheduler;
  }
}
