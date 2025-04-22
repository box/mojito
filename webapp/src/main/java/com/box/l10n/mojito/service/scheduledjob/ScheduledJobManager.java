package com.box.l10n.mojito.service.scheduledjob;

import static com.box.l10n.mojito.quartz.QuartzSchedulerManager.DEFAULT_SCHEDULER_NAME;

import com.box.l10n.mojito.entity.ScheduledJob;
import com.box.l10n.mojito.quartz.QuartzSchedulerManager;
import com.box.l10n.mojito.retry.DeadLockLoserExceptionRetryTemplate;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.scheduledjob.jobs.ScheduledThirdPartySyncProperties;
import com.box.l10n.mojito.service.thirdparty.ThirdPartySyncJobConfig;
import com.box.l10n.mojito.service.thirdparty.ThirdPartySyncJobsConfig;
import com.google.common.base.Strings;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Component for scheduling jobs inside the scheduled jobs table. Currently, jobs are pulled from
 * the application.properties and pushed to the scheduled_job table.
 *
 * @author mattwilshire
 */
@Configuration
@Component
@ConditionalOnProperty(value = "l10n.scheduledJobs.enabled", havingValue = "true")
public class ScheduledJobManager {
  static Logger logger = LoggerFactory.getLogger(ScheduledJobManager.class);

  private final ThirdPartySyncJobsConfig thirdPartySyncJobsConfig;
  private final QuartzSchedulerManager schedulerManager;
  private final ScheduledJobRepository scheduledJobRepository;
  private final ScheduledJobStatusRepository scheduledJobStatusRepository;
  private final ScheduledJobTypeRepository scheduledJobTypeRepository;
  private final RepositoryRepository repositoryRepository;
  private final DeadLockLoserExceptionRetryTemplate deadlockRetryTemplate;

  /* Quartz scheduler dedicated to scheduled jobs using in memory data source.
   * The value is also set using equals as this is not managed by Spring Boot in the tests. */
  @Value("${l10n.scheduledJobs.quartz.schedulerName:" + DEFAULT_SCHEDULER_NAME + "}")
  private String schedulerName = DEFAULT_SCHEDULER_NAME;

  public HashSet<String> uuidPool = new HashSet<>();

  private static String DEFAULT_PLURAL_SEPARATOR = " _";

  @Autowired
  public ScheduledJobManager(
      ThirdPartySyncJobsConfig thirdPartySyncJobConfig,
      QuartzSchedulerManager schedulerManager,
      ScheduledJobRepository scheduledJobRepository,
      ScheduledJobStatusRepository scheduledJobStatusRepository,
      ScheduledJobTypeRepository scheduledJobTypeRepository,
      RepositoryRepository repositoryRepository,
      DeadLockLoserExceptionRetryTemplate deadlockRetryTemplate) {
    this.thirdPartySyncJobsConfig = thirdPartySyncJobConfig;
    this.schedulerManager = schedulerManager;
    this.scheduledJobRepository = scheduledJobRepository;
    this.scheduledJobStatusRepository = scheduledJobStatusRepository;
    this.scheduledJobTypeRepository = scheduledJobTypeRepository;
    this.repositoryRepository = repositoryRepository;
    this.deadlockRetryTemplate = deadlockRetryTemplate;
  }

  @PostConstruct
  public void init() throws ClassNotFoundException, SchedulerException {
    logger.info("Scheduled Job Manager started.");
    // Add Job Listener
    getScheduler()
        .getListenerManager()
        .addJobListener(
            new ScheduledJobListener(
                scheduledJobRepository, scheduledJobStatusRepository, deadlockRetryTemplate));
    // Add Trigger Listener
    getScheduler()
        .getListenerManager()
        .addTriggerListener(new ScheduledJobTriggerListener(scheduledJobRepository));

    pushJobsToDB();
    cleanQuartzJobs();
    scheduleAllJobs();
  }

  /** Schedule all the jobs in the scheduled_job table with their cron expression. */
  public void scheduleAllJobs() throws ClassNotFoundException, SchedulerException {
    List<ScheduledJob> scheduledJobs = scheduledJobRepository.findAll();

    for (ScheduledJob scheduledJob : scheduledJobs) {
      JobKey jobKey = getJobKey(scheduledJob);
      TriggerKey triggerKey = getTriggerKey(scheduledJob);

      // Retrieve job class from enum value e.g. THIRD_PARTY_SYNC -> ScheduledThirdPartySync
      Class<? extends IScheduledJob> jobType =
          loadJobClass(scheduledJob.getJobType().getEnum().getJobClassName());

      JobDetail job = JobBuilder.newJob(jobType).withIdentity(jobKey).build();
      Trigger trigger = buildTrigger(jobKey, scheduledJob.getCron(), triggerKey);

      if (getScheduler().checkExists(jobKey)) {
        // The cron could have changed, reschedule the job
        getScheduler().rescheduleJob(triggerKey, trigger);
      } else {
        getScheduler().scheduleJob(job, trigger);
      }

      deadlockRetryTemplate.execute(
          c -> {
            scheduledJobRepository.save(scheduledJob);
            return null;
          });
    }
  }

  /** Push the jobs defined in the application.properties to the jobs table. */
  public void pushJobsToDB() {
    for (ThirdPartySyncJobConfig syncJobConfig :
        thirdPartySyncJobsConfig.getThirdPartySyncJobs().values()) {
      if (Strings.isNullOrEmpty(syncJobConfig.getUuid())
          || Strings.isNullOrEmpty(syncJobConfig.getCron())) {
        logger.info(
            "UUID or cron expression not defined for repository {}, skipping this third party sync job.",
            syncJobConfig.getRepository());
        continue;
      }

      if (uuidPool.contains(syncJobConfig.getUuid())) {
        throw new RuntimeException(
            "Duplicate UUID found for scheduled job: "
                + syncJobConfig.getUuid()
                + " please change this UUID to be unique.");
      }

      uuidPool.add(syncJobConfig.getUuid());

      Optional<ScheduledJob> optScheduledJob =
          scheduledJobRepository.findByUuid(syncJobConfig.getUuid());
      ScheduledJob scheduledJob = optScheduledJob.orElseGet(ScheduledJob::new);

      scheduledJob.setUuid(syncJobConfig.getUuid());
      scheduledJob.setCron(syncJobConfig.getCron());
      scheduledJob.setRepository(repositoryRepository.findByName(syncJobConfig.getRepository()));
      scheduledJob.setJobType(
          scheduledJobTypeRepository.findByEnum(syncJobConfig.getScheduledJobType()));
      scheduledJob.setProperties(getScheduledThirdPartySyncProperties(syncJobConfig));

      if (scheduledJob.getJobStatus() == null) {
        scheduledJob.setJobStatus(
            scheduledJobStatusRepository.findByEnum(ScheduledJobStatus.SCHEDULED));
      }

      try {
        scheduledJobRepository.save(scheduledJob);
      } catch (DataIntegrityViolationException e) {
        // Attempted to insert another scheduled job into the table with the same repo and job type,
        // this can happen in a clustered quartz environment, don't need to display the error.
      }
    }
  }

  /**
   * Remove jobs defined under this custom scheduler that are not listed in the application
   * properties but are present in the DB table.
   */
  public void cleanQuartzJobs() throws SchedulerException {
    // Clean Quartz jobs that don't exist in the UUID pool
    logger.info("Performing Quartz scheduled jobs clean up");
    for (JobKey jobKey : getAllJobKeys()) {
      if (!uuidPool.contains(jobKey.getName())) {
        if (getScheduler().checkExists(jobKey)) {
          getScheduler().deleteJob(jobKey);
        }

        deadlockRetryTemplate.execute(
            c -> {
              scheduledJobRepository
                  .findByUuid(jobKey.getName())
                  .ifPresent(scheduledJobRepository::delete);
              return null;
            });

        logger.info(
            "Removed job with id: '{}' as it is no longer in the data source.", jobKey.getName());
      }
    }
  }

  // v1
  private ScheduledThirdPartySyncProperties getScheduledThirdPartySyncProperties(
      ThirdPartySyncJobConfig jobConfig) {
    ScheduledThirdPartySyncProperties thirdPartySyncProperties =
        new ScheduledThirdPartySyncProperties();
    thirdPartySyncProperties.setThirdPartyProjectId(jobConfig.getThirdPartyProjectId());
    thirdPartySyncProperties.setActions(jobConfig.getActions());
    thirdPartySyncProperties.setPluralSeparator(
        Objects.equals(jobConfig.getPluralSeparator(), "")
            ? DEFAULT_PLURAL_SEPARATOR
            : jobConfig.getPluralSeparator());
    thirdPartySyncProperties.setLocaleMapping(jobConfig.getLocaleMapping());
    thirdPartySyncProperties.setSkipTextUnitsWithPattern(jobConfig.getSkipTextUnitsWithPattern());
    thirdPartySyncProperties.setSkipAssetsWithPathPattern(jobConfig.getSkipAssetsWithPathPattern());
    thirdPartySyncProperties.setIncludeTextUnitsWithPattern(
        jobConfig.getIncludeTextUnitsWithPattern());

    thirdPartySyncProperties.setOptions(
        jobConfig.getOptions() != null ? jobConfig.getOptions() : new ArrayList<>());

    return thirdPartySyncProperties;
  }

  public Trigger buildTrigger(JobKey jobKey, String cronExpression, TriggerKey triggerKey) {
    // Misfires should only occur when the job was triggerred manually and missed its schedule,
    // the default misfire policy requires the job to miss its trigger time to miss by 60s to
    // classify as a misfire
    return TriggerBuilder.newTrigger()
        .withSchedule(
            CronScheduleBuilder.cronSchedule(cronExpression)
                .withMisfireHandlingInstructionFireAndProceed())
        .withIdentity(triggerKey)
        .forJob(jobKey)
        .build();
  }

  public JobKey getJobKey(ScheduledJob scheduledJob) {
    // name = UUID, group = THIRD_PARTY_SYNC
    return new JobKey(scheduledJob.getUuid(), scheduledJob.getJobType().getEnum().toString());
  }

  private TriggerKey getTriggerKey(ScheduledJob scheduledJob) {
    // name = trigger_UUID, group = THIRD_PARTY_SYNC
    return new TriggerKey(
        "trigger_" + scheduledJob.getUuid(), scheduledJob.getJobType().getEnum().toString());
  }

  public Scheduler getScheduler() {
    return schedulerManager.getScheduler(schedulerName);
  }

  /**
   * Retrieve all jobs where the group is in the ScheduledJobType enums. Cannot assume all jobs
   * under the scheduler is a scheduled job as the scheduler could be the default scheduler.
   */
  public List<JobKey> getAllJobKeys() throws SchedulerException {
    List<String> jobTypes = Arrays.stream(ScheduledJobType.values()).map(Enum::toString).toList();
    return getScheduler().getJobKeys(GroupMatcher.anyGroup()).stream()
        .filter(jobKey -> jobTypes.contains(jobKey.getGroup()))
        .collect(Collectors.toList());
  }

  public List<String> getAllJobNames() throws SchedulerException {
    return getAllJobKeys().stream().map(JobKey::getName).collect(Collectors.toList());
  }

  public Class<? extends IScheduledJob> loadJobClass(String className)
      throws ClassNotFoundException {
    Class<?> clazz = Class.forName(className);
    // Reflection to check if the class implements the IScheduledJob interface
    if (!IScheduledJob.class.isAssignableFrom(clazz)) {
      throw new IllegalArgumentException(
          "Class " + className + " does not implement IScheduledJob interface.");
    }
    return clazz.asSubclass(IScheduledJob.class);
  }
}
