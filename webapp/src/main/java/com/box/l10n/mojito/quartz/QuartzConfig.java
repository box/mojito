package com.box.l10n.mojito.quartz;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.annotation.PostConstruct;
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

@Component
public class QuartzConfig {

  Logger logger = LoggerFactory.getLogger(QuartzConfig.class);

  public static final String DYNAMIC_GROUP_NAME = "DYNAMIC";

  @Autowired Scheduler scheduler;

  @Autowired(required = false)
  List<Trigger> triggers = new ArrayList<>();

  @Autowired(required = false)
  List<JobDetail> jobDetails = new ArrayList<>();

  @Autowired QuartzPropertiesConfig quartzPropertiesConfig;

  /**
   * Starts the scheduler after having removed outdated trigger/jobs
   *
   * @throws SchedulerException
   */
  @PostConstruct
  void startScheduler() throws SchedulerException {
    Properties quartzProps = quartzPropertiesConfig.getQuartzProperties();
    removeOutdatedJobs();
    if (Boolean.parseBoolean(quartzProps.getProperty("org.quartz.scheduler.enabled", "true"))) {
      logger.info("Starting scheduler");
      scheduler.startDelayed(2);
    }
  }

  void removeOutdatedJobs() throws SchedulerException {
    scheduler.unscheduleJobs(new ArrayList<TriggerKey>(getOutdatedTriggerKeys()));
    scheduler.deleteJobs(new ArrayList<JobKey>(getOutdatedJobKeys()));
  }

  Set<JobKey> getOutdatedJobKeys() throws SchedulerException {

    Set<JobKey> jobKeys =
        scheduler.getJobKeys(GroupMatcher.jobGroupEquals(Scheduler.DEFAULT_GROUP));
    Set<JobKey> newJobKeys = new HashSet<>();

    for (JobDetail jobDetail : jobDetails) {
      // maybe we don't have yet the job to clean up the db
      logger.info("Processing job details: {}", jobDetail.getKey());
      newJobKeys.add(jobDetail.getKey());
    }

    jobKeys.removeAll(newJobKeys);

    return jobKeys;
  }

  Set<TriggerKey> getOutdatedTriggerKeys() throws SchedulerException {

    Set<TriggerKey> triggerKeys =
        scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(Scheduler.DEFAULT_GROUP));
    Set<TriggerKey> newTriggerKeys = new HashSet<>();

    for (Trigger trigger : triggers) {
      newTriggerKeys.add(trigger.getKey());
    }

    triggerKeys.removeAll(newTriggerKeys);

    return triggerKeys;
  }
}
