package com.box.l10n.mojito.service.blobstorage.autoconfigure;

import com.amazonaws.services.s3.AmazonS3;
import com.box.l10n.mojito.service.blobstorage.BlobStorage;
import com.box.l10n.mojito.service.blobstorage.database.DatabaseBlobStorage;
import com.box.l10n.mojito.service.blobstorage.database.DatabaseBlobStorageCleanupJob;
import com.box.l10n.mojito.service.blobstorage.database.DatabaseBlobStorageConfigurationProperties;
import com.box.l10n.mojito.service.blobstorage.database.MBlobRepository;
import com.box.l10n.mojito.service.blobstorage.s3.S3BlobStorage;
import com.box.l10n.mojito.service.blobstorage.s3.S3BlobStorageConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

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

    @ConditionalOnProperty(value = "l10n.blob-storage.type", havingValue = "s3")
    @Configuration
    static class S3BlobStorageConfigurationConfiguration {

        @Autowired
        AmazonS3 amazonS3;

        @Autowired
        S3BlobStorageConfigurationProperties s3BlobStorageConfigurationProperties;

        @Bean
        public S3BlobStorage s3BlobStorage() {
            return new S3BlobStorage(amazonS3, s3BlobStorageConfigurationProperties);
        }
    }

    @ConditionalOnProperty(value = "l10n.blob-storage.type", havingValue = "database", matchIfMissing = true)
    @Import(DatabaseBlobStorageCleanupJob.class)
    static class DatabaseBlobStorageConfiguration {

        @Autowired
        MBlobRepository mBlobRepository;

        @Autowired
        DatabaseBlobStorageConfigurationProperties databaseBlobStorageConfigurationProperties;

        @Bean
        public DatabaseBlobStorage databaseBlobStorage() {
            return new DatabaseBlobStorage(databaseBlobStorageConfigurationProperties, mBlobRepository);
        }
    }
}
