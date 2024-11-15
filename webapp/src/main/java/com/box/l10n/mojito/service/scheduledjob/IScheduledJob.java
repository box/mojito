package com.box.l10n.mojito.service.scheduledjob;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public interface IScheduledJob extends Job {
  void onSuccess(JobExecutionContext context);

  void onFailure(JobExecutionContext context, JobExecutionException jobException);
}
