package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableJob;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.quartz.QuartzService;
import java.time.ZonedDateTime;
import org.junit.Test;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class QuartzJobsDeleteCommandTest extends CLITestBase {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(QuartzJobsDeleteCommandTest.class);

  @Autowired QuartzPollableTaskScheduler quartzPollableTaskScheduler;

  @Autowired QuartzService quartzService;

  /**
   * {@code quartz-jobs-delete} clears the shared Quartz DYNAMIC group, but other tests and the
   * webapp keep scheduling jobs there (for example RepositoryStatisticsJob). Asserting that the
   * whole group is empty is therefore prone to race conditions. This test only tracks its own
   * uniquely named AJob entries before and after the delete command so that the test is not
   * affected by other tests or the webapp and is therefore not flaky.
   */
  @Test
  public void testDeleteAllDynamicJobs() throws Exception {
    String testJobName1 = testIdWatcher.getEntityName("1");
    String testJobName2 = testIdWatcher.getEntityName("2");

    waitForCondition(
        "This test's jobs should not already be scheduled",
        () -> !hasTestJob(testJobName1) && !hasTestJob(testJobName2));

    getL10nJCommander().run("quartz-jobs-view");
    assertFalse(
        "Should not show this test's jobs yet",
        outputCapture.toString().contains("AJob_" + testJobName1));
    assertFalse(
        "Should not show this test's jobs yet",
        outputCapture.toString().contains("AJob_" + testJobName2));

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

    waitForCondition(
        "This test's jobs should be scheduled",
        () -> hasTestJob(testJobName1) && hasTestJob(testJobName2));

    getL10nJCommander().run("quartz-jobs-view");
    assertTrue("Should show 1 job", outputCapture.toString().contains("AJob_" + testJobName1));
    assertTrue("Should show 1 job", outputCapture.toString().contains("AJob_" + testJobName2));

    getL10nJCommander().run("quartz-jobs-delete");

    waitForCondition(
        "This test's jobs should be deleted",
        () -> !hasTestJob(testJobName1) && !hasTestJob(testJobName2));

    getL10nJCommander().run("quartz-jobs-view");
  }

  /**
   * Job keys are {@code canonicalClassName + "_" + uniqueId}. Match the same substring used by
   * {@code quartz-jobs-view} assertions so leftover jobs from other tests are ignored.
   */
  private boolean hasTestJob(String uniqueId) {
    try {
      String needle = "AJob_" + uniqueId;
      return quartzService.getDynamicJobs().stream().anyMatch(name -> name.contains(needle));
    } catch (SchedulerException e) {
      return false;
    }
  }

  public static class AJob extends QuartzPollableJob<Void, Void> {

    @Override
    public Void call(Void input) throws Exception {
      logger.debug("do nothing, test");
      return null;
    }
  }
}
