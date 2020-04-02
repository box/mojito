package com.box.l10n.mojito.service.blobstorage.database;

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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import org.springframework.stereotype.Component;

@Profile("!disablescheduling")
@DisallowConcurrentExecution
public class DatabaseBlobStorageCleanupJob implements Job {

    static Logger logger = LoggerFactory.getLogger(DatabaseBlobStorageCleanupJob.class);

    @Autowired
    DatabaseBlobStorage databaseBlobStorage;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.debug("Cleanup expired blobs");
        databaseBlobStorage.deleteExpired();
    }

    @Bean(name = "jobDetailDatabaseBlobStorageCleanupJob")
    public JobDetailFactoryBean jobDetailExpiringBlobCleanup() {
        JobDetailFactoryBean jobDetailFactory = new JobDetailFactoryBean();
        jobDetailFactory.setJobClass(DatabaseBlobStorageCleanupJob.class);
        jobDetailFactory.setDescription("Cleanup expired blobs");
        jobDetailFactory.setDurability(true);
        return jobDetailFactory;
    }

    @Bean
    public SimpleTriggerFactoryBean triggerExpiringBlobCleanup(@Qualifier("jobDetailDatabaseBlobStorageCleanupJob") JobDetail job) {
        SimpleTriggerFactoryBean trigger = new SimpleTriggerFactoryBean();
        trigger.setJobDetail(job);
        trigger.setRepeatInterval(300000);
        trigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
        return trigger;
    }
}
