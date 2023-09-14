package com.box.l10n.mojito.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.concurrent.TimeUnit;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(value = "l10n.management.metrics.quartz.enabled", havingValue = "true")
@Component
public class QuartzMetricsReportingJobListener implements JobListener {

  static Logger logger = LoggerFactory.getLogger(QuartzMetricsReportingJobListener.class);

  private MeterRegistry meterRegistry;

  public QuartzMetricsReportingJobListener(@Autowired MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @Override
  public String getName() {
    return getClass().getSimpleName();
  }

  @Override
  public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
    Tags timerTags =
        Tags.of(
            "jobGroup", context.getJobDetail().getKey().getGroup(),
            "jobClass", context.getJobDetail().getJobClass().getSimpleName(),
            "schedulerName", getSchedulerName(context));

    meterRegistry
        .timer("quartz.jobs.execution", timerTags)
        .record(context.getJobRunTime(), TimeUnit.MILLISECONDS);

    meterRegistry
        .timer("quartz.jobs.waiting", timerTags)
        .record(getJobWaitingTime(context), TimeUnit.MILLISECONDS);
  }

  @Override
  public void jobToBeExecuted(JobExecutionContext context) {}

  @Override
  public void jobExecutionVetoed(JobExecutionContext context) {}

  private static String getSchedulerName(JobExecutionContext context) {
    String schedulerName = "unknown";
    if (context.getScheduler() != null) {
      try {
        schedulerName = context.getScheduler().getSchedulerName();
      } catch (SchedulerException e) {
        logger.debug("Can't get scheduler name", e);
      }
    }
    return schedulerName;
  }

  private static long getJobWaitingTime(JobExecutionContext context) {
    long waiting =
        System.currentTimeMillis()
            - context.getScheduledFireTime().getTime()
            - context.getJobRunTime();

    return waiting > 0 ? waiting : 0;
  }
}
