package com.box.l10n.mojito.service.blobstorage.database;

import com.box.l10n.mojito.entity.MBlob;
import com.box.l10n.mojito.service.blobstorage.BlobStorage;
import com.box.l10n.mojito.service.blobstorage.Retention;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

/**
 * Implementation that use the database to store the blobs.
 * <p>
 * Expired blobs are cleaned up by {@link DatabaseBlobStorageCleanupJob}, time to live can be configured with
 * {@link DatabaseBlobStorageConfigurationProperties#min1DayTtl}
 * <p>
 * This is implementation should be used only for testing or for deployments with limited load.
 */
public class DatabaseBlobStorage implements BlobStorage {

    static Logger logger = LoggerFactory.getLogger(DatabaseBlobStorage.class);

    DatabaseBlobStorageConfigurationProperties databaseBlobStorageConfigurationProperties;

    MBlobRepository mBlobRepository;

    public DatabaseBlobStorage(DatabaseBlobStorageConfigurationProperties databaseBlobStorageConfigurationProperties,
                               MBlobRepository mBlobRepository) {

        Preconditions.checkNotNull(mBlobRepository);
        Preconditions.checkNotNull(databaseBlobStorageConfigurationProperties);

        this.mBlobRepository = mBlobRepository;
        this.databaseBlobStorageConfigurationProperties = databaseBlobStorageConfigurationProperties;
    }

    @Override
    public void put(String name, byte[] content, Retention retention) {
        MBlob mBlob = mBlobRepository.findByName(name);

        if (mBlob == null) {
            mBlob = new MBlob();
            mBlob.setName(name);
        }

        mBlob.setContent(content);

        if (Retention.MIN_1_DAY.equals(retention)) {
            mBlob.setExpireAfterSeconds(databaseBlobStorageConfigurationProperties.getMin1DayTtl());
        }

        mBlobRepository.save(mBlob);
    }

    @Override
    public Optional<byte[]> getBytes(String name) {
        byte[] bytes = null;

        MBlob mBlob = mBlobRepository.findByName(name);

        if (mBlob != null) {
            bytes = mBlob.getContent();
        }

        return Optional.ofNullable(bytes);
    }

    public void deleteExpired() {
        int deletedCount;
        do {
            PageRequest pageable = PageRequest.of(0, 500);
            List<Long> expired = mBlobRepository.findExpiredBlobIds(pageable);
            if (!expired.isEmpty()) {
                deletedCount = mBlobRepository.deleteByIds(expired);
                logger.debug("Number of Mbob deleted: {}", deletedCount);
            } else {
                logger.debug("Nothing to delete");
                deletedCount = 0;
            }
        } while (deletedCount > 0);
    }
}