package com.box.l10n.mojito.service.blobstorage.database;

import com.box.l10n.mojito.entity.MBlob;
import com.box.l10n.mojito.retry.DataIntegrityViolationExceptionRetryTemplate;
import com.box.l10n.mojito.service.blobstorage.BlobStorage;
import com.box.l10n.mojito.service.blobstorage.Retention;
import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;

/**
 * Implementation that use the database to store the blobs.
 *
 * <p>Expired blobs are cleaned up by {@link DatabaseBlobStorageCleanupJob}, time to live can be
 * configured with {@link DatabaseBlobStorageConfigurationProperties#min1DayTtl}
 *
 * <p>This is implementation should be used only for testing or for deployments with limited load.
 */
public class DatabaseBlobStorage implements BlobStorage {

  static Logger logger = LoggerFactory.getLogger(DatabaseBlobStorage.class);

  DatabaseBlobStorageConfigurationProperties databaseBlobStorageConfigurationProperties;

  MBlobRepository mBlobRepository;

  DataIntegrityViolationExceptionRetryTemplate dataIntegrityViolationExceptionRetryTemplate;

  public DatabaseBlobStorage(
      DatabaseBlobStorageConfigurationProperties databaseBlobStorageConfigurationProperties,
      MBlobRepository mBlobRepository,
      DataIntegrityViolationExceptionRetryTemplate dataIntegrityViolationExceptionRetryTemplate) {

    Preconditions.checkNotNull(mBlobRepository);
    Preconditions.checkNotNull(databaseBlobStorageConfigurationProperties);
    Preconditions.checkNotNull(dataIntegrityViolationExceptionRetryTemplate);

    this.mBlobRepository = mBlobRepository;
    this.databaseBlobStorageConfigurationProperties = databaseBlobStorageConfigurationProperties;
    this.dataIntegrityViolationExceptionRetryTemplate =
        dataIntegrityViolationExceptionRetryTemplate;
  }

  @Override
  public void put(String name, byte[] content, Retention retention) {

    dataIntegrityViolationExceptionRetryTemplate.execute(
        context -> {
          if (context.getRetryCount() > 0) {
            logger.info(
                "Assume concurrent modification happened, retry attempt: {}",
                context.getRetryCount());
          }
          putBase(name, content, retention);
          return null;
        });
  }

  void putBase(String name, byte[] content, Retention retention) {
    MBlob mBlob =
        mBlobRepository
            .findByName(name)
            .orElseGet(
                () -> {
                  MBlob mb = new MBlob();
                  mb.setName(name);
                  return mb;
                });

    mBlob.setContent(content);

    if (Retention.MIN_1_DAY.equals(retention)) {
      mBlob.setExpireAfterSeconds(databaseBlobStorageConfigurationProperties.getMin1DayTtl());
    }

    mBlobRepository.save(mBlob);
  }

  @Override
  public void delete(String name) {
    mBlobRepository.findByName(name).ifPresent(mb -> mBlobRepository.deleteById(mb.getId()));
  }

  @Override
  public boolean exists(String name) {
    return mBlobRepository
        .findByName(name)
        .map(mb -> Boolean.TRUE)
        .orElse(Boolean.FALSE)
        .booleanValue();
  }

  @Override
  public Optional<byte[]> getBytes(String name) {
    byte[] bytes = null;
    return mBlobRepository.findByName(name).map(MBlob::getContent);
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
