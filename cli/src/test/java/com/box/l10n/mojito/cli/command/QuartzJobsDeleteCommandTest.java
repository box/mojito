package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableJob;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.quartz.QuartzService;
import java.time.ZonedDateTime;
import java.util.function.Predicate;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class QuartzJobsDeleteCommandTest extends CLITestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(QuartzJobsDeleteCommandTest.class);

  @Autowired QuartzPollableTaskScheduler quartzPollableTaskScheduler;

  @Autowired QuartzService quartzService;

  @Test
  public void testDeleteDynamicJobs() throws Exception {
    String testJobName1 = testIdWatcher.getEntityName("1");
    String testJobName2 = testIdWatcher.getEntityName("2");

    quartzPollableTaskScheduler.scheduleJob(
        QuartzJobInfo.newBuilder(AJob.class)
            .withTriggerStartDate(JSR310Migration.dateTimePlusAsDate(ZonedDateTime.now(), 100000))
            .withUniqueId(testJobName1)
            .build());
    quartzPollableTaskScheduler.scheduleJob(
        QuartzJobInfo.newBuilder(AJob.class)
            .withTriggerStartDate(JSR310Migration.dateTimePlusAsDate(ZonedDateTime.now(), 100000))
            .withUniqueId(testJobName2)
            .build());

    getL10nJCommander().run("quartz-jobs-view");
    assertTrue("Should show 1 job", outputCapture.toString().contains("AJob_" + testJobName1));
    assertTrue("Should show 1 job", outputCapture.toString().contains("AJob_" + testJobName2));

    Predicate<String> jobsFilter =
        job -> job.contains("AJob_" + testJobName1) || job.contains("AJob_" + testJobName2);

    assertEquals(2L, quartzService.getDynamicJobs().stream().filter(jobsFilter).count());
    getL10nJCommander().run("quartz-jobs-delete");
    // Although all jobs should be deleted, they must be filtered as the dynamic
    // RepositoryStatistics job could have started after the deletion which would make this test
    // flaky
    assertEquals(0L, quartzService.getDynamicJobs().stream().filter(jobsFilter).count());
  }

  public static class AJob extends QuartzPollableJob<Void, Void> {

    @Override
    public Void call(Void input) throws Exception {
      logger.debug("do nothing, test");
      return null;
    }
  }
}
