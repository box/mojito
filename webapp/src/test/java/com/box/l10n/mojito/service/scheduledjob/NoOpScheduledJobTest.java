package com.box.l10n.mojito.service.scheduledjob;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

@Component
public class NoOpScheduledJobTest implements IScheduledJob {
  public static boolean throwException = false;
  public static boolean successEvent = false;
  public static boolean failureEvent = false;

  @Override
  public void onSuccess(JobExecutionContext context) {
    successEvent = true;
  }

  @Override
  public void onFailure(JobExecutionContext context, JobExecutionException jobException) {
    failureEvent = true;
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

    if (throwException) {
      throw new JobExecutionException();
    }

    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
