package com.box.l10n.mojito.service.blobstorage;

import com.amazonaws.services.s3.AmazonS3;
import com.box.l10n.mojito.service.blobstorage.database.DatabaseBlobStorage;
import com.box.l10n.mojito.service.blobstorage.database.DatabaseBlobStorageCleanupJob;
import com.box.l10n.mojito.service.blobstorage.database.DatabaseBlobStorageConfigurationProperties;
import com.box.l10n.mojito.service.blobstorage.database.MBlobRepository;
import com.box.l10n.mojito.service.blobstorage.s3.S3BlobStorage;
import com.box.l10n.mojito.service.blobstorage.s3.S3BlobStorageConfigurationProperties;
import org.quartz.JobDetail;
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

import java.time.Duration;

/**
 * Configuration for {@link BlobStorage}
 *
 * {@link DatabaseBlobStorage} is the default implementation but it should be use only for testing or deployments with
 * limited load.
 * <p>
 * Consider using {@link S3BlobStorage} for larger deployment. An {@link AmazonS3} client must be configured first,
 * and then the storage enabled with the `l10n.blob-storage.type=s3` property
 */
@Configuration
public class BlobStorageConfiguration {

    static Logger logger = LoggerFactory.getLogger(BlobStorageConfiguration.class);

    @ConditionalOnProperty(value = "l10n.blob-storage.type", havingValue = "s3")
    @Configuration
    static class S3BlobStorageConfigurationConfiguration {

        @Autowired
        AmazonS3 amazonS3;

        @Autowired
        S3BlobStorageConfigurationProperties s3BlobStorageConfigurationProperties;

        @Bean
        public S3BlobStorage s3BlobStorage() {
            logger.info("Configure S3BlobStorage");
            return new S3BlobStorage(amazonS3, s3BlobStorageConfigurationProperties);
        }
    }

    @ConditionalOnProperty(value = "l10n.blob-storage.type", havingValue = "database", matchIfMissing = true)
    static class DatabaseBlobStorageConfiguration {

        @Autowired
        MBlobRepository mBlobRepository;

        @Autowired
        DatabaseBlobStorageConfigurationProperties databaseBlobStorageConfigurationProperties;

        @Bean
        public DatabaseBlobStorage databaseBlobStorage() {
            logger.info("Configure DatabaseBlobStorage");
            return new DatabaseBlobStorage(databaseBlobStorageConfigurationProperties, mBlobRepository);
        }

        @Bean(name = "jobDetailDatabaseBlobStorageCleanupJob")
        public JobDetailFactoryBean jobDetailExpiringBlobCleanup() {
            JobDetailFactoryBean jobDetailFactory = new JobDetailFactoryBean();
            jobDetailFactory.setJobClass(DatabaseBlobStorageCleanupJob.class);
            jobDetailFactory.setDescription("Cleanup expired blobs");
            jobDetailFactory.setDurability(true);
            return jobDetailFactory;
        }

        @Profile("!disablescheduling")
        @Bean
        public SimpleTriggerFactoryBean triggerExpiringBlobCleanup(@Qualifier("jobDetailDatabaseBlobStorageCleanupJob") JobDetail job) {
            logger.info("Configure jobDetailDatabaseBlobStorageCleanupJob");
            SimpleTriggerFactoryBean trigger = new SimpleTriggerFactoryBean();
            trigger.setJobDetail(job);
            trigger.setRepeatInterval(Duration.ofMinutes(5).toMillis());
            trigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
            return trigger;
        }
    }

}
