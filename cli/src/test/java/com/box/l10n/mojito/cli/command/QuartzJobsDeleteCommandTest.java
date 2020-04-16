package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableJob;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.quartz.QuartzService;
import org.joda.time.DateTime;
import org.junit.Test;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class QuartzJobsDeleteCommandTest extends CLITestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(QuartzJobsDeleteCommandTest.class);

    @Autowired
    QuartzPollableTaskScheduler quartzPollableTaskScheduler;

    @Autowired
    QuartzService quartzService;

    @Test
    public void testDeleteAllDynamicJobs() throws Exception {
        waitForCondition("Should be no dynamic jobs", () -> {
            boolean result = false;
            try {
                result = quartzService.getDynamicJobs().isEmpty();
            } catch (SchedulerException e) {
            }
            return result;
        });

        getL10nJCommander().run("quartz-jobs-view");
        assertTrue("Should show no jobs", outputCapture.toString().contains("None"));
        assertTrue(quartzService.getDynamicJobs().isEmpty());

        String testJobName1 = testIdWatcher.getEntityName("1");
        String testJobName2 = testIdWatcher.getEntityName("2");

        quartzPollableTaskScheduler.scheduleJob(QuartzJobInfo.newBuilder(TestJob.class).withTriggerStartDate(DateTime.now().plus(100000).toDate()).withUniqueId(testJobName1).build());
        quartzPollableTaskScheduler.scheduleJob(QuartzJobInfo.newBuilder(TestJob.class).withTriggerStartDate(DateTime.now().plus(100000).toDate()).withUniqueId(testJobName2).build());

        getL10nJCommander().run("quartz-jobs-view");
        assertTrue("Should show 1 job", outputCapture.toString().contains("TestJob_" + testJobName1));
        assertTrue("Should show 1 job", outputCapture.toString().contains("TestJob_" + testJobName2));
        assertEquals(2L, quartzService.getDynamicJobs().size());

        getL10nJCommander().run("quartz-jobs-delete");
        getL10nJCommander().run("quartz-jobs-view");
        assertEquals(0L, quartzService.getDynamicJobs().size());
    }

    public static class TestJob extends QuartzPollableJob<Void, Void> {

        @Override
        public Void call(Void input) throws Exception {
            logger.debug("do nothing, test");
            return null;
        }
    }

}