package com.box.l10n.mojito.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.quartz.JobBuilder.newJob;

import com.google.common.util.concurrent.Uninterruptibles;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.RequiredSearch;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

public class QuartzMetricsReportingJobListenerTest {

  static final int JOB_SLEEP_TIME = 25;

  Scheduler scheduler;
  QuartzMetricsReportingJobListener jobListener;
  MeterRegistry meterRegistry;

  @Before
  public void setUp() throws Exception {
    meterRegistry = new SimpleMeterRegistry();
    jobListener = new QuartzMetricsReportingJobListener(meterRegistry);
    scheduler = StdSchedulerFactory.getDefaultScheduler();
    scheduler.getListenerManager().addJobListener(jobListener);
    scheduler.start();
  }

  @After
  public void tearDown() throws Exception {
    scheduler.shutdown(true);
  }

  @Test
  public void testTimersAreUpdatedUponExecution() throws Exception {

    for (int i = 0; i < 5; i++) {
      JobDetail job = newJob(TestJob.class).withIdentity("TestJob_" + i, "DEFAULT").build();
      scheduler.scheduleJob(job, TriggerBuilder.newTrigger().forJob(job).startNow().build());
    }

    await().until(() -> scheduler.isStarted());

    RequiredSearch timerSearch =
        meterRegistry
            .get("quartz.jobs.execution")
            .tag("jobClass", "TestJob")
            .tag("jobGroup", "DEFAULT");

    await().untilAsserted(() -> assertThat(timerSearch.timers()).isNotEmpty());

    assertThat(timerSearch.timer().takeSnapshot().count()).isGreaterThanOrEqualTo(5);
    assertThat(timerSearch.timer().takeSnapshot().mean()).isGreaterThanOrEqualTo(JOB_SLEEP_TIME);
  }

  public static class TestJob implements Job {
    @Override
    public void execute(JobExecutionContext context) {
      Uninterruptibles.sleepUninterruptibly(JOB_SLEEP_TIME, TimeUnit.MILLISECONDS);
    }
  }
}
