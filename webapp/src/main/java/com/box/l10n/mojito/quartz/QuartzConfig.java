package com.box.l10n.mojito.quartz;

import static com.box.l10n.mojito.quartz.QuartzSchedulerManager.DEFAULT_SCHEDULER_NAME;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class QuartzConfig {

  Logger logger = LoggerFactory.getLogger(QuartzConfig.class);

  public static final String DYNAMIC_GROUP_NAME = "DYNAMIC";

  @Autowired QuartzSchedulerManager schedulerManager;

  @Autowired(required = false)
  List<Trigger> triggers = new ArrayList<>();

  @Autowired(required = false)
  List<JobDetail> jobDetails = new ArrayList<>();

  @Value("${l10n.org.quartz.scheduler.enabled:true}")
  Boolean schedulerEnabled;

  /**
   * Starts the scheduler after having removed outdated trigger/jobs
   *
   * @throws SchedulerException
   */
  @PostConstruct
  void startSchedulers() throws SchedulerException {
    int delay = 2;
    if (schedulerEnabled) {
      removeOutdatedJobs();
      for (Scheduler scheduler : schedulerManager.getSchedulers()) {
        logger.info("Starting scheduler: {}", scheduler.getSchedulerName());
        scheduler.startDelayed(delay);
        // Increment the delay to avoid lock exceptions being thrown as both schedulers try to start
        // concurrently
        delay++;
      }
    }
  }

  void removeOutdatedJobs() throws SchedulerException {
    for (Scheduler scheduler : schedulerManager.getSchedulers()) {
      if (scheduler.getSchedulerName().equals(DEFAULT_SCHEDULER_NAME)) {
        scheduler.unscheduleJobs(new ArrayList<TriggerKey>(getOutdatedTriggerKeys(scheduler)));
      }
      scheduler.deleteJobs(new ArrayList<JobKey>(getOutdatedJobKeys(scheduler)));
    }
  }

  Set<JobKey> getOutdatedJobKeys(Scheduler scheduler) throws SchedulerException {

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

  Set<TriggerKey> getOutdatedTriggerKeys(Scheduler scheduler) throws SchedulerException {

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
