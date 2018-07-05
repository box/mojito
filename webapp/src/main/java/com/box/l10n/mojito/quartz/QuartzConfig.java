package com.box.l10n.mojito.quartz;

import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Component
public class QuartzConfig {

    Logger logger = LoggerFactory.getLogger(QuartzConfig.class);

    public static final String DYNAMIC_GROUP_NAME = "DYNAMIC";

    @Autowired
    Scheduler scheduler;

    @Autowired
    Trigger[] triggers;

    @Autowired
    JobDetail[] jobDetails;

    /**
     * Starts the scheduler after having removed outdated trigger/jobs
     * @throws SchedulerException
     */
    @PostConstruct
    void startScheduler() throws SchedulerException {
        scheduler.unscheduleJobs(new ArrayList<TriggerKey>(getOutdatedTriggerKeys()));
        scheduler.deleteJobs(new ArrayList<JobKey>(getOutdatedJobKeys()));
        scheduler.startDelayed(2);
    }

    Set<JobKey> getOutdatedJobKeys() throws SchedulerException {

        Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(Scheduler.DEFAULT_GROUP));
        Set<JobKey> newJobKeys = new HashSet<>();

        for (JobDetail jobDetail : jobDetails) {
            newJobKeys.add(jobDetail.getKey());
        }

        jobKeys.removeAll(newJobKeys);

        return jobKeys;
    }

    Set<TriggerKey> getOutdatedTriggerKeys() throws SchedulerException {

        Set<TriggerKey> triggerKeys = scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(Scheduler.DEFAULT_GROUP));
        Set<TriggerKey> newTriggerKeys = new HashSet<>();

        for (Trigger trigger : triggers) {
            newTriggerKeys.add(trigger.getKey());
        }

        triggerKeys.removeAll(newTriggerKeys);

        return triggerKeys;
    }
}