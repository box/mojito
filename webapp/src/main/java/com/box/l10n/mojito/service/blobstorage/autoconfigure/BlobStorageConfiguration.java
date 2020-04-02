package com.box.l10n.mojito.service.blobstorage.autoconfigure;

import com.box.l10n.mojito.service.blobstorage.BlobStorage;
import com.box.l10n.mojito.service.blobstorage.database.DatabaseBlobStorage;
import com.box.l10n.mojito.service.blobstorage.database.DatabaseBlobStorageCleanupJob;
import com.box.l10n.mojito.service.blobstorage.database.DatabaseBlobStorageConfigurationProperties;
import com.box.l10n.mojito.service.blobstorage.database.MBlobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for {@link BlobStorage}
 *
 * {@link DatabaseBlobStorage} is the default implementation but it should be use only for testing or deployments with
 * limited load.
 */
@Configuration
public class BlobStorageConfiguration {

    @ConditionalOnProperty(value = "l10n.blob-storage.type", havingValue = "database", matchIfMissing = true)
    static class DatabaseBlobStorageConfiguration {

        @Autowired
        MBlobRepository mBlobRepository;

        @Autowired
        DatabaseBlobStorageConfigurationProperties databaseBlobStorageConfigurationProperties;

        @Bean
        public BlobStorage databaseBlobStorage() {
            return new DatabaseBlobStorage(databaseBlobStorageConfigurationProperties, mBlobRepository);
        }

        @Bean
        public DatabaseBlobStorageCleanupJob databaseBlobStorageCleanupJob() {
            return new DatabaseBlobStorageCleanupJob();
        }
    }
}
