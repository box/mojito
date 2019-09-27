package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.quartz.QuartzPollableJob;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.quartz.QuartzService;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.scheduler.SchedulableJob;
import org.joda.time.DateTime;
import org.junit.Test;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Random;

import static org.junit.Assert.*;

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
        getL10nJCommander().run("quartz-jobs-view");
        assertTrue("Should show no jobs", outputCapture.toString().contains("None"));
        assertTrue(quartzService.getDynamicJobs().isEmpty());

        String testJobName1 = testIdWatcher.getEntityName("1");
        String testJobName2 = testIdWatcher.getEntityName("2");

        TestJob testJob = new TestJob();
        testJob.schedule(DateTime.now().plus(100000).toDate(), testJobName1);
        testJob.schedule(DateTime.now().plus(100000).toDate(), testJobName2);

        getL10nJCommander().run("quartz-jobs-view");
        assertTrue("Should show 1 job", outputCapture.toString().contains("TestJob_" + testJobName1));
        assertTrue("Should show 1 job", outputCapture.toString().contains("TestJob_" + testJobName2));
        assertEquals(2L, quartzService.getDynamicJobs().size());

        getL10nJCommander().run("quartz-jobs-delete");
        getL10nJCommander().run("quartz-jobs-view");
        assertEquals(0L, quartzService.getDynamicJobs().size());
    }

    @Configurable
    static class TestJob extends SchedulableJob {

        @Override
        protected String getDescription() {
            return "test job";
        }

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            logger.debug("do nothing, test");
        }

        public void schedule(Date date, String name) {
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("name", name);
            schedule(jobDataMap, date, "name");
        }
    }

}