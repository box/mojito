package com.box.l10n.mojito.service.delta;

import com.box.l10n.mojito.service.assetExtraction.AssetExtractionCleanupJob;
import java.time.Duration;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.stereotype.Component;

/** @author garion */
@Profile("!disablescheduling")
@Configuration
@Component
@DisallowConcurrentExecution
@ConditionalOnProperty(value = "l10n.PushPullRun.cleanup-job.enabled", havingValue = "true")
public class PushPullRunCleanupJob implements Job {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(AssetExtractionCleanupJob.class);

  /** Runs once a day by default. */
  @Value("${l10n.PushPullRun.cleanup-job.cron:}")
  String pushPullCleanupCron = "0 1 * * *";

  @Value("${l10n.PushPullRun.cleanup-job.retentionDuration:}")
  Duration retentionDuration = Duration.ofDays(30);

  @Autowired PushPullRunCleanupService pushPullRunCleanupService;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    logger.debug("Running Push and Pull run retention cleanup job.");
    pushPullRunCleanupService.cleanOldPushPullData(retentionDuration);
  }

  @Bean(name = "jobDetailPushPullRunCleanupJob")
  public JobDetailFactoryBean jobDetailPushPullRunCleanupJob() {
    JobDetailFactoryBean jobDetailFactory = new JobDetailFactoryBean();
    jobDetailFactory.setJobClass(PushPullRunCleanupJob.class);
    jobDetailFactory.setDescription("Delete old Push and Pull run data.");
    jobDetailFactory.setDurability(true);
    return jobDetailFactory;
  }

  @Bean
  public CronTriggerFactoryBean triggerPushPullRunCleanupJob(
      @Qualifier("jobDetailPushPullRunCleanupJob") JobDetail job) {
    CronTriggerFactoryBean trigger = new CronTriggerFactoryBean();
    trigger.setJobDetail(job);
    trigger.setCronExpression(pushPullCleanupCron);
    return trigger;
  }
}
