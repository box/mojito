package com.box.l10n.mojito.service.scheduler;

import com.box.l10n.mojito.aspect.StopWatch;
import org.joda.time.DateTime;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;

import static com.box.l10n.mojito.quartz.QuartzConfig.DYNAMIC_GROUP_NAME;

/**
 * A Quartz job that can also be simply scheduled.
 *
 * Each job should have a typed schedule interface. This base class provides a generic schedule method that it not
 * intended to be called directly in the code.
 */
public abstract class SchedulableJob implements Job {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(SchedulableJob.class);

    @Autowired
    Scheduler scheduler;

    protected abstract String getDescription();

    protected void schedule(JobDataMap jobDataMap, String... keys) {
        schedule(jobDataMap, new Date(), keys);
    }

    @StopWatch
    protected void schedule(JobDataMap jobDataMap, Date triggerStartDate, String... keys) {

        Class clazz = this.getClass();
        String uniqueId = getUniqueId(jobDataMap, keys);

        String keyName = clazz.getSimpleName() + "_" + uniqueId;

        try {
            TriggerKey triggerKey = new TriggerKey(keyName, DYNAMIC_GROUP_NAME);
            JobKey jobKey = new JobKey(keyName, DYNAMIC_GROUP_NAME);

            JobDetail jobDetail = scheduler.getJobDetail(jobKey);

            if (jobDetail == null) {
                logger.debug("Job doesn't exist, create for key: {}", keyName);
                jobDetail = JobBuilder.newJob().ofType(clazz)
                        .withIdentity(keyName, DYNAMIC_GROUP_NAME)
                        .withDescription(getDescription())
                        .storeDurably()
                        .build();

                scheduler.addJob(jobDetail, true);
            }

            logger.debug("Schedule a job for key: {}", keyName);
            Trigger trigger = TriggerBuilder.newTrigger()
                    .startAt(triggerStartDate)
                    .forJob(jobDetail)
                    .usingJobData(jobDataMap)
                    .withIdentity(triggerKey).build();

            if (!scheduler.checkExists(triggerKey)) {
                scheduler.scheduleJob(trigger);
            } else {
                logger.debug("Job already scheduled for key: {}", keyName);
                scheduler.rescheduleJob(triggerKey, trigger);
            }
        } catch (SchedulerException se) {
            logger.error("Couldn't schedule a job for key: " + keyName, se);
        }
    }


    String getUniqueId(JobDataMap jobDataMap, String... keys) {
        return Arrays.stream(keys).map(key -> jobDataMap.get(key).toString()).collect(Collectors.joining("_"));
    }
}
