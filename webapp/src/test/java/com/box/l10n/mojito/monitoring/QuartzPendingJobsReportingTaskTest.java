package com.box.l10n.mojito.monitoring;

import static com.box.l10n.mojito.monitoring.QuartzPendingJobsReportingTask.extractClassName;
import static com.box.l10n.mojito.quartz.QuartzConfig.DYNAMIC_GROUP_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.quartz.JobBuilder.newJob;

import com.box.l10n.mojito.quartz.QuartzSchedulerManager;
import com.box.l10n.mojito.service.DBUtils;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.test.TestIdWatcher;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.core.instrument.search.RequiredSearch;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;
import javax.sql.DataSource;
import org.awaitility.core.ConditionFactory;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class QuartzPendingJobsReportingTaskTest extends ServiceTestBase {

  public static final String MY_GROUP = "MY_GROUP";

  @Autowired MeterRegistry meterRegistry;

  @Autowired DataSource dataSource;

  @Autowired QuartzSchedulerManager schedulerManager;

  @Autowired DBUtils dbUtils;

  @Value("${l10n.management.metrics.quartz.sql-queue-monitoring.enabled:false}")
  Boolean monitoringEnabled;

  QuartzPendingJobsReportingTask task;

  Scheduler scheduler;

  /*
   * This sets up the condition we'll use for await() later on. Given jobs can take some time
   * to start and be processed, we wait at most 5 seconds and we ignore exceptions of Meters not
   * found while we search for them (at t0 we might not have a specific meter given by the
   * RequiredSearch conditions, but then at t1 the process that populates that
   * Meter might have completed and populated that search)
   */
  ConditionFactory await =
      await().atMost(Duration.ofSeconds(5)).ignoreException(MeterNotFoundException.class);

  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  @Before
  public void setUp() throws SchedulerException {

    Assume.assumeTrue(dbUtils.isMysql() && monitoringEnabled);
    task = new QuartzPendingJobsReportingTask(dataSource, meterRegistry);
    scheduler = schedulerManager.getScheduler(QuartzSchedulerManager.DEFAULT_SCHEDULER_NAME);

    scheduler.clear();
  }

  @Test
  public void testGaugesCountPendingJobsForNonDynamicGroup() throws Exception {

    JobDetail job;
    TriggerBuilder<Trigger> builder;

    job =
        newJob(Test1Job.class)
            .withIdentity(testIdWatcher.getEntityName("Test1Job"), MY_GROUP)
            .build();
    builder =
        TriggerBuilder.newTrigger()
            .forJob(job)
            .startAt(Date.from(ZonedDateTime.now().plusMinutes(5).toInstant()));
    scheduler.scheduleJob(job, builder.build());

    job =
        newJob(Test2Job.class)
            .withIdentity(testIdWatcher.getEntityName("Test2Job"), MY_GROUP)
            .build();
    builder =
        TriggerBuilder.newTrigger()
            .forJob(job)
            .startAt(Date.from(ZonedDateTime.now().plusMinutes(5).toInstant()));
    scheduler.scheduleJob(job, builder.build());

    task.reportPendingJobs();

    RequiredSearch search1 =
        meterRegistry
            .get("quartz.pending.jobs")
            .tag("jobGroup", MY_GROUP)
            .tag("jobClass", "QuartzPendingJobsReportingTaskTest$Test1Job");
    RequiredSearch search2 =
        meterRegistry
            .get("quartz.pending.jobs")
            .tag("jobGroup", MY_GROUP)
            .tag("jobClass", "QuartzPendingJobsReportingTaskTest$Test2Job");

    await.untilAsserted(() -> assertThat(search1.gauge().value()).isEqualTo(1));
    await.untilAsserted(() -> assertThat(search2.gauge().value()).isEqualTo(1));
  }

  @Test
  public void testGaugesCountPendingJobsForDynamicGroup() throws Exception {

    JobDetail job;
    TriggerBuilder<Trigger> builder;

    // First run: We have 5 jobs of each type

    for (int i = 1; i <= 5; i++) {
      job =
          newJob(Test1Job.class)
              .withIdentity(testIdWatcher.getEntityName("Test1Job_" + i), DYNAMIC_GROUP_NAME)
              .build();
      builder =
          TriggerBuilder.newTrigger()
              .forJob(job)
              .startAt(Date.from(ZonedDateTime.now().plusHours(i).toInstant()));
      scheduler.scheduleJob(job, builder.build());

      job =
          newJob(EmptyTestJob.class)
              .withIdentity(testIdWatcher.getEntityName("EmptyJob_" + i), DYNAMIC_GROUP_NAME)
              .build();
      builder =
          TriggerBuilder.newTrigger()
              .forJob(job)
              .startAt(Date.from(ZonedDateTime.now().plusHours(i).toInstant()));
      scheduler.scheduleJob(job, builder.build());
    }

    task.reportPendingJobs();

    RequiredSearch test1Gauges =
        meterRegistry
            .get("quartz.pending.jobs")
            .tag("jobClass", "QuartzPendingJobsReportingTaskTest$Test1Job")
            .tag("jobGroup", DYNAMIC_GROUP_NAME);

    RequiredSearch test2Gauges =
        meterRegistry
            .get("quartz.pending.jobs")
            .tag("jobClass", "EmptyTestJob")
            .tag("jobGroup", DYNAMIC_GROUP_NAME);

    await.untilAsserted(() -> assertThat(test1Gauges.gauges()).isNotEmpty());
    await.untilAsserted(() -> assertThat(test2Gauges.gauges()).isNotEmpty());
    assertThat(test1Gauges.gauge().value()).isEqualTo(5);
    assertThat(test2Gauges.gauge().value()).isEqualTo(5);

    // Second run: We have 4 new jobs of each type

    for (int i = 6; i <= 9; i++) {
      job =
          newJob(Test1Job.class)
              .withIdentity(testIdWatcher.getEntityName("Test1Job_" + i), DYNAMIC_GROUP_NAME)
              .build();
      builder =
          TriggerBuilder.newTrigger()
              .forJob(job)
              .startAt(Date.from(ZonedDateTime.now().plusHours(i).toInstant()));
      scheduler.scheduleJob(job, builder.build());

      job =
          newJob(EmptyTestJob.class)
              .withIdentity(testIdWatcher.getEntityName("EmptyJob_" + i), DYNAMIC_GROUP_NAME)
              .build();
      builder =
          TriggerBuilder.newTrigger()
              .forJob(job)
              .startAt(Date.from(ZonedDateTime.now().plusHours(i).toInstant()));
      scheduler.scheduleJob(job, builder.build());
    }

    task.reportPendingJobs();

    assertThat(test1Gauges.gauge().value()).isEqualTo(9);
    assertThat(test2Gauges.gauge().value()).isEqualTo(9);

    // Third run: We add 11 new jobs of the Test1Job class

    for (int i = 11; i <= 21; i++) {
      job =
          newJob(Test1Job.class)
              .withIdentity(testIdWatcher.getEntityName("Test1Job_" + i), DYNAMIC_GROUP_NAME)
              .build();
      builder =
          TriggerBuilder.newTrigger()
              .forJob(job)
              .startAt(Date.from(ZonedDateTime.now().plusHours(i).toInstant()));
      scheduler.scheduleJob(job, builder.build());
    }

    task.reportPendingJobs();

    assertThat(test1Gauges.gauge().value()).isEqualTo(20);
    assertThat(test2Gauges.gauge().value()).isEqualTo(9);

    // Fourth run:
    //  a) All jobs that were scheduled previously have completed (simulated through
    // scheduler.clear())
    //  b) We add 5 jobs of type EmptyJob

    scheduler.clear();

    for (int i = 1; i <= 5; i++) {
      job =
          newJob(EmptyTestJob.class)
              .withIdentity(testIdWatcher.getEntityName("EmptyJob_" + i), DYNAMIC_GROUP_NAME)
              .build();
      builder =
          TriggerBuilder.newTrigger()
              .forJob(job)
              .startAt(Date.from(ZonedDateTime.now().plusHours(i).toInstant()));
      scheduler.scheduleJob(job, builder.build());
    }

    task.reportPendingJobs();

    assertThat(test1Gauges.gauge().value()).isEqualTo(0);
    assertThat(test2Gauges.gauge().value()).isEqualTo(5);
  }

  @Test
  public void testFetchResults() throws Exception {

    JobDetail job;
    TriggerBuilder<Trigger> builder;
    QuartzPendingJobsReportingTask.PendingJob pendingJob1, pendingJob2;
    String key1 = "QuartzPendingJobsReportingTaskTest$Test1Job-" + DYNAMIC_GROUP_NAME;
    String key2 = "EmptyTestJob-" + DYNAMIC_GROUP_NAME;

    // First run: We have 5 jobs of each type

    for (int i = 1; i <= 5; i++) {
      job =
          newJob(Test1Job.class)
              .withIdentity(testIdWatcher.getEntityName("Test1Job_" + i), DYNAMIC_GROUP_NAME)
              .build();
      builder =
          TriggerBuilder.newTrigger()
              .forJob(job)
              .startAt(Date.from(ZonedDateTime.now().plusHours(i).toInstant()));
      scheduler.scheduleJob(job, builder.build());

      job =
          newJob(EmptyTestJob.class)
              .withIdentity(testIdWatcher.getEntityName("EmptyJob_" + i), DYNAMIC_GROUP_NAME)
              .build();
      builder =
          TriggerBuilder.newTrigger()
              .forJob(job)
              .startAt(Date.from(ZonedDateTime.now().plusHours(i).toInstant()));
      scheduler.scheduleJob(job, builder.build());
    }

    Map<String, QuartzPendingJobsReportingTask.PendingJob> pendingJobs = task.fetchResults();

    assertThat(pendingJobs).hasSize(2);
    assertThat(pendingJobs).containsKey(key1);
    assertThat(pendingJobs).containsKey(key2);

    pendingJob1 = pendingJobs.get(key1);
    pendingJob2 = pendingJobs.get(key2);

    assertThat(pendingJob1.jobClass).isEqualTo("QuartzPendingJobsReportingTaskTest$Test1Job");
    assertThat(pendingJob1.jobGroup).isEqualTo(DYNAMIC_GROUP_NAME);
    assertThat(pendingJob1.count).isEqualTo(5L);
    assertThat(pendingJob2.jobClass).isEqualTo("EmptyTestJob");
    assertThat(pendingJob2.jobGroup).isEqualTo(DYNAMIC_GROUP_NAME);
    assertThat(pendingJob2.count).isEqualTo(5L);

    // Second run: We have 4 new jobs of each type

    for (int i = 6; i <= 9; i++) {
      job =
          newJob(Test1Job.class)
              .withIdentity(testIdWatcher.getEntityName("Test1Job_" + i), DYNAMIC_GROUP_NAME)
              .build();
      builder =
          TriggerBuilder.newTrigger()
              .forJob(job)
              .startAt(Date.from(ZonedDateTime.now().plusHours(i).toInstant()));
      scheduler.scheduleJob(job, builder.build());

      job =
          newJob(EmptyTestJob.class)
              .withIdentity(testIdWatcher.getEntityName("EmptyJob_" + i), DYNAMIC_GROUP_NAME)
              .build();
      builder =
          TriggerBuilder.newTrigger()
              .forJob(job)
              .startAt(Date.from(ZonedDateTime.now().plusHours(i).toInstant()));
      scheduler.scheduleJob(job, builder.build());
    }

    pendingJobs = task.fetchResults();

    assertThat(pendingJobs).hasSize(2);
    assertThat(pendingJobs).containsKey(key1);
    assertThat(pendingJobs).containsKey(key2);

    pendingJob1 = pendingJobs.get(key1);
    pendingJob2 = pendingJobs.get(key2);

    assertThat(pendingJob1.jobClass).isEqualTo("QuartzPendingJobsReportingTaskTest$Test1Job");
    assertThat(pendingJob1.jobGroup).isEqualTo(DYNAMIC_GROUP_NAME);
    assertThat(pendingJob1.count).isEqualTo(9L);
    assertThat(pendingJob2.jobClass).isEqualTo("EmptyTestJob");
    assertThat(pendingJob2.jobGroup).isEqualTo(DYNAMIC_GROUP_NAME);
    assertThat(pendingJob2.count).isEqualTo(9L);

    // Third run: We add 11 new jobs of the Test1Job class

    for (int i = 11; i <= 21; i++) {
      job =
          newJob(Test1Job.class)
              .withIdentity(testIdWatcher.getEntityName("Test1Job_" + i), DYNAMIC_GROUP_NAME)
              .build();
      builder =
          TriggerBuilder.newTrigger()
              .forJob(job)
              .startAt(Date.from(ZonedDateTime.now().plusHours(i).toInstant()));
      scheduler.scheduleJob(job, builder.build());
    }

    pendingJobs = task.fetchResults();

    assertThat(pendingJobs).hasSize(2);
    assertThat(pendingJobs).containsKey(key1);
    assertThat(pendingJobs).containsKey(key2);

    pendingJob1 = pendingJobs.get(key1);
    pendingJob2 = pendingJobs.get(key2);

    assertThat(pendingJob1.jobClass).isEqualTo("QuartzPendingJobsReportingTaskTest$Test1Job");
    assertThat(pendingJob1.jobGroup).isEqualTo(DYNAMIC_GROUP_NAME);
    assertThat(pendingJob1.count).isEqualTo(20L);
    assertThat(pendingJob2.jobClass).isEqualTo("EmptyTestJob");
    assertThat(pendingJob2.jobGroup).isEqualTo(DYNAMIC_GROUP_NAME);
    assertThat(pendingJob2.count).isEqualTo(9L);

    scheduler.clear();

    for (int i = 1; i <= 5; i++) {
      job =
          newJob(EmptyTestJob.class)
              .withIdentity(testIdWatcher.getEntityName("EmptyJob_" + i), DYNAMIC_GROUP_NAME)
              .build();
      builder =
          TriggerBuilder.newTrigger()
              .forJob(job)
              .startAt(Date.from(ZonedDateTime.now().plusHours(i).toInstant()));
      scheduler.scheduleJob(job, builder.build());
    }

    pendingJobs = task.fetchResults();

    assertThat(pendingJobs).hasSize(1);
    assertThat(pendingJobs).doesNotContainKey(key1);
    assertThat(pendingJobs).containsKey(key2);

    pendingJob2 = pendingJobs.get(key2);

    assertThat(pendingJob2.jobClass).isEqualTo("EmptyTestJob");
    assertThat(pendingJob2.jobGroup).isEqualTo(DYNAMIC_GROUP_NAME);
    assertThat(pendingJob2.count).isEqualTo(5L);
  }

  @Test
  public void testExtractClassName() {
    assertThat(extractClassName(".")).isEqualTo("");
    assertThat(extractClassName("")).isEqualTo("");
    assertThat(extractClassName("-")).isEqualTo("-");
    assertThat(extractClassName("ClassName")).isEqualTo("ClassName");
    assertThat(extractClassName("org.package.ClassName")).isEqualTo("ClassName");
    assertThat(extractClassName(EmptyTestJob.class.getName())).isEqualTo("EmptyTestJob");
    assertThat(extractClassName(getClass().getCanonicalName()))
        .isEqualTo("QuartzPendingJobsReportingTaskTest");
    assertThat(extractClassName(Test1Job.class.getName()))
        .isEqualTo("QuartzPendingJobsReportingTaskTest$Test1Job");
  }

  public static class Test1Job implements Job {
    @Override
    public void execute(JobExecutionContext context) {}
  }

  public static class Test2Job implements Job {
    @Override
    public void execute(JobExecutionContext context) {}
  }
}
