package com.box.l10n.mojito.service.cache;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * Deletes entries from the DatabaseCache for which their expiry date has already occurred.
 *
 * @author garion
 */
@Profile("!disablescheduling")
@Configurable
@DisallowConcurrentExecution
public class DatabaseCacheEvictionJob implements Job {

    static Logger logger = LoggerFactory.getLogger(DatabaseCacheEvictionJob.class);

    @Autowired
    ApplicationCacheRepository applicationCacheRepository;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            clearAllExpired();
        } catch (Exception ex) {
            logger.error("Could not clear expired entries from the cache due to exception: ", ex);
            throw ex;
        }
    }

    @Bean(name = "DatabaseCacheEvictionJob")
    JobDetailFactoryBean jobDatabaseCacheEvictionJob() {
        JobDetailFactoryBean jobDetailFactory = new JobDetailFactoryBean();
        jobDetailFactory.setJobClass(DatabaseCacheEvictionJob.class);
        jobDetailFactory.setDescription("Remove expired entries from the database cache.");
        jobDetailFactory.setDurability(true);
        return jobDetailFactory;
    }

    @Profile("!disablescheduling")
    @Bean
    public SimpleTriggerFactoryBean triggerDatabaseCacheCronEvictionJob(@Qualifier("DatabaseCacheEvictionJob") JobDetail job) {
        logger.info("Configure triggerDatabaseCacheCronEvictionJob");
        SimpleTriggerFactoryBean trigger = new SimpleTriggerFactoryBean();
        trigger.setJobDetail(job);
        trigger.setRepeatInterval(Duration.ofMinutes(5).toMillis());
        trigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
        return trigger;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void clearAllExpired() {
        logger.info("Evicting expired entries from the database cache.");
        applicationCacheRepository.clearAllExpired();
    }
}
