package com.box.l10n.mojito.monitoring;

import com.box.l10n.mojito.service.DBUtils;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.test.TestIdWatcher;

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
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.core.instrument.search.RequiredSearch;

import static com.box.l10n.mojito.monitoring.QuartzPendingJobsReportingTask.extractClassName;
import static com.box.l10n.mojito.quartz.QuartzConfig.DYNAMIC_GROUP_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.quartz.JobBuilder.newJob;

public class QuartzPendingJobsReportingTaskTest extends ServiceTestBase {

    public static final String MY_GROUP = "MY_GROUP";

    @Autowired
    MeterRegistry meterRegistry;

    @Autowired
    DataSource dataSource;

    @Autowired
    Scheduler scheduler;

    @Autowired
    DBUtils dbUtils;

    @Value("${l10n.management.metrics.quartz.sql-queue-monitoring.enabled:false}")
    Boolean monitoringEnabled;

    QuartzPendingJobsReportingTask task;

    /*
     * This sets up the condition we'll use for await() later on. Given jobs can take some time
     * to start and be processed, we wait at most 5 seconds and we ignore exceptions of Meters not
     * found while we search for them (at t0 we might not have a specific meter given by the
     * RequiredSearch conditions, but then at t1 the process that populates that
     * Meter might have completed and populated that search)
     */
    ConditionFactory await = await().atMost(Duration.ofSeconds(5))
                                    .ignoreException(MeterNotFoundException.class);

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    @Before
    public void setUp() throws SchedulerException {


        task = new QuartzPendingJobsReportingTask(dataSource, meterRegistry);

        scheduler.clear();
    }

    @Test
    public void testGaugesCountPendingJobsForNonDynamicGroup() throws Exception {

        Assume.assumeTrue(dbUtils.isMysql() && monitoringEnabled);

        JobDetail job;
        TriggerBuilder<Trigger> builder;

        job = newJob(Test1Job.class).withIdentity(testIdWatcher.getEntityName("Test1Job"), MY_GROUP).build();
        builder = TriggerBuilder.newTrigger()
                                .forJob(job)
                                .startAt(Date.from(ZonedDateTime.now().plusMinutes(5).toInstant()));
        scheduler.scheduleJob(job, builder.build());

        job = newJob(Test2Job.class).withIdentity(testIdWatcher.getEntityName("Test2Job"), MY_GROUP).build();
        builder = TriggerBuilder.newTrigger()
                                .forJob(job)
                                .startAt(Date.from(ZonedDateTime.now().plusMinutes(5).toInstant()));
        scheduler.scheduleJob(job, builder.build());

        task.reportPendingJobs();

        RequiredSearch search1 = meterRegistry.get("quartz.pending.jobs")
                                              .tag("jobGroup", MY_GROUP)
                                              .tag("jobClass", "QuartzPendingJobsReportingTaskTest$Test1Job");
        RequiredSearch search2 = meterRegistry.get("quartz.pending.jobs")
                                              .tag("jobGroup", MY_GROUP)
                                              .tag("jobClass", "QuartzPendingJobsReportingTaskTest$Test2Job");


        await.untilAsserted(() -> assertThat(search1.gauge().value()).isEqualTo(1));
        await.untilAsserted(() -> assertThat(search2.gauge().value()).isEqualTo(1));
    }

    @Test
    public void testGaugesCountPendingJobsForDynamicGroup() throws Exception {

        Assume.assumeTrue(dbUtils.isMysql() && monitoringEnabled);

        JobDetail job;
        TriggerBuilder<Trigger> builder;

        for (int i = 1; i <= 5; i++) {
            job = newJob(Test1Job.class).withIdentity(testIdWatcher.getEntityName("Test1Job_" + i), DYNAMIC_GROUP_NAME)
                                        .build();
            builder = TriggerBuilder.newTrigger()
                                    .forJob(job)
                                    .startAt(Date.from(ZonedDateTime.now().plusHours(i).toInstant()));
            scheduler.scheduleJob(job, builder.build());

            job = newJob(EmptyTestJob.class).withIdentity(testIdWatcher.getEntityName("EmptyJob_" + i), DYNAMIC_GROUP_NAME)
                                            .build();
            builder = TriggerBuilder.newTrigger()
                                    .forJob(job)
                                    .startAt(Date.from(ZonedDateTime.now().plusHours(i).toInstant()));
            scheduler.scheduleJob(job, builder.build());
        }

        task.reportPendingJobs();

        RequiredSearch test1Gauges = meterRegistry.get("quartz.pending.jobs")
                                                  .tag("jobClass", "QuartzPendingJobsReportingTaskTest$Test1Job")
                                                  .tag("jobGroup", DYNAMIC_GROUP_NAME);

        RequiredSearch test2Gauges = meterRegistry.get("quartz.pending.jobs")
                                                  .tag("jobClass", "EmptyTestJob")
                                                  .tag("jobGroup", DYNAMIC_GROUP_NAME);

        await.untilAsserted(() -> assertThat(test1Gauges.gauges()).isNotEmpty());
        await.untilAsserted(() -> assertThat(test2Gauges.gauges()).isNotEmpty());
        await.untilAsserted(() -> assertThat(test1Gauges.gauge().value()).isEqualTo(5));
        await.untilAsserted(() -> assertThat(test2Gauges.gauge().value()).isEqualTo(5));
    }

    @Test
    public void testFetchResults() throws Exception {

        Assume.assumeTrue(dbUtils.isMysql() && monitoringEnabled);

        JobDetail job;
        TriggerBuilder<Trigger> builder;

        for (int i = 1; i <= 5; i++) {
            job = newJob(Test1Job.class).withIdentity(testIdWatcher.getEntityName("Test1Job_" + i), DYNAMIC_GROUP_NAME)
                                        .build();
            builder = TriggerBuilder.newTrigger()
                                    .forJob(job)
                                    .startAt(Date.from(ZonedDateTime.now().plusHours(i).toInstant()));
            scheduler.scheduleJob(job, builder.build());

            job = newJob(EmptyTestJob.class).withIdentity(testIdWatcher.getEntityName("EmptyJob_" + i), DYNAMIC_GROUP_NAME)
                                            .build();
            builder = TriggerBuilder.newTrigger()
                                    .forJob(job)
                                    .startAt(Date.from(ZonedDateTime.now().plusHours(i).toInstant()));
            scheduler.scheduleJob(job, builder.build());
        }

        List<QuartzPendingJobsReportingTask.PendingJob> pendingJobs = task.fetchResults();

        assertThat(pendingJobs).hasSize(2);
        assertThat(pendingJobs).extracting("jobClass")
                               .containsExactlyInAnyOrder("QuartzPendingJobsReportingTaskTest$Test1Job", "EmptyTestJob");
        assertThat(pendingJobs).extracting("jobGroup")
                               .contains(DYNAMIC_GROUP_NAME);
        assertThat(pendingJobs).extracting("count")
                               .containsExactlyInAnyOrder((long) 5, (long) 5);
    }

    @Test
    public void testExtractClassName() {
        assertThat(extractClassName(".")).isEqualTo("");
        assertThat(extractClassName("")).isEqualTo("");
        assertThat(extractClassName("-")).isEqualTo("-");
        assertThat(extractClassName("ClassName")).isEqualTo("ClassName");
        assertThat(extractClassName("org.package.ClassName")).isEqualTo("ClassName");
        assertThat(extractClassName(EmptyTestJob.class.getName())).isEqualTo("EmptyTestJob");
        assertThat(extractClassName(getClass().getCanonicalName())).isEqualTo("QuartzPendingJobsReportingTaskTest");
        assertThat(extractClassName(Test1Job.class.getName())).isEqualTo("QuartzPendingJobsReportingTaskTest$Test1Job");
    }

    public static class Test1Job implements Job {
        @Override
        public void execute(JobExecutionContext context) {
        }
    }

    public static class Test2Job implements Job {
        @Override
        public void execute(JobExecutionContext context) {
        }
    }
}
