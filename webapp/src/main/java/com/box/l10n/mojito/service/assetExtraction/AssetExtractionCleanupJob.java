package com.box.l10n.mojito.service.assetExtraction;

import java.time.Duration;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import org.springframework.stereotype.Component;

/**
 * @author aloison
 */
@Profile("!disablescheduling")
@Configuration
@Component
@DisallowConcurrentExecution
@ConditionalOnProperty(value = "l10n.asset-extraction.cleanup-job.enabled", havingValue = "true")
public class AssetExtractionCleanupJob implements Job {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(AssetExtractionCleanupJob.class);

  @Autowired AssetExtractionCleanupService assetExtractionCleanupService;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    logger.debug("Cleanup asset extraction");
    assetExtractionCleanupService.cleanupOldAssetExtractions();
  }

  @Bean(name = "jobDetailAssetExtractionCleanup")
  public JobDetailFactoryBean jobDetailAssetExtractionCleanup() {
    JobDetailFactoryBean jobDetailFactory = new JobDetailFactoryBean();
    jobDetailFactory.setJobClass(AssetExtractionCleanupJob.class);
    jobDetailFactory.setDescription("Cleanup old asset extraction");
    jobDetailFactory.setDurability(true);
    return jobDetailFactory;
  }

  @Bean
  public SimpleTriggerFactoryBean triggerAssetExtractionCleanup(
      @Qualifier("jobDetailAssetExtractionCleanup") JobDetail job) {
    SimpleTriggerFactoryBean trigger = new SimpleTriggerFactoryBean();
    trigger.setJobDetail(job);
    trigger.setRepeatInterval(Duration.ofMinutes(5).toMillis());
    trigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
    return trigger;
  }
}
