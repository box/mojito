package com.box.l10n.mojito.service.blobstorage.gcs;

import com.box.l10n.mojito.service.blobstorage.BlobStorage;
import com.box.l10n.mojito.service.blobstorage.Retention;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.common.base.Preconditions;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation that uses Google Cloud Storage to store blobs.
 *
 * <p>Rely on S3 lifecyle rules to clean up expired blobs. This must be set up on the bucket,
 * otherwise no cleanup will happen.
 *
 * <p>Objects will have a <a
 * href="https://docs.cloud.google.com/storage/docs/metadata#custom-time">Custom-Time</a> metadata
 * field set to the end of the desired retention period (except {@link Retention#PERMANENT} where
 * this is not set).
 *
 * <p>On the bucket, configure a single lifecycle rule:
 *
 * <ul>
 *   <li>action: Delete
 *   <li>condition: {@code daysSinceCustomTime: 0}
 * </ul>
 *
 * That will delete objects once the current time is past their Custom-Time (end of retention). Note
 * that objects without Custom-Time set (i.e. {@link Retention#PERMANENT}) are never deleted by this
 * rule.
 *
 * <p>See <a href="https://docs.cloud.google.com/storage/docs/lifecycle#dayssincecustomtime">Object
 * Lifecycle Management</a> for reference.
 */
public class GCSBlobStorage implements BlobStorage {

  static final Logger logger = LoggerFactory.getLogger(GCSBlobStorage.class);

  private final Storage storage;
  private final GCSBlobStorageConfigurationProperties configurationProperties;

  private static final String byteContentType = "application/octet-stream";

  public GCSBlobStorage(
      Storage storage, GCSBlobStorageConfigurationProperties configurationProperties) {
    Preconditions.checkNotNull(storage);
    Preconditions.checkNotNull(configurationProperties);
    this.storage = storage;
    this.configurationProperties = configurationProperties;
  }

  public Optional<byte[]> getBytes(String name) {
    Blob blob = storage.get(BlobId.of(configurationProperties.getBucket(), getFullName(name)));
    if (blob == null) {
      return Optional.empty();
    }
    return Optional.of(blob.getContent());
  }

  public void put(String name, byte[] content, Retention retention) {
    BlobInfo.Builder builder =
        BlobInfo.newBuilder(BlobId.of(configurationProperties.getBucket(), getFullName(name)))
            .setContentType(byteContentType);

    customTimeAtEndOfRetention(retention).ifPresent(builder::setCustomTimeOffsetDateTime);

    storage.create(builder.build(), content);
  }

  public void delete(String name) {
    storage.delete(BlobId.of(configurationProperties.getBucket(), getFullName(name)));
  }

  public boolean exists(String name) {
    Blob blob = storage.get(BlobId.of(configurationProperties.getBucket(), getFullName(name)));
    return blob != null;
  }

  /**
   * GCS Custom-Time at end of retention for lifecycle (daysSinceCustomTime: 0). Empty means no
   * Custom-Time (object never expires). Exhaustive on {@link Retention} so new enum values require
   * an explicit case here.
   */
  private static Optional<OffsetDateTime> customTimeAtEndOfRetention(Retention retention) {
    return switch (retention) {
      case PERMANENT -> Optional.empty();
      case MIN_1_DAY -> Optional.of(OffsetDateTime.now().plusDays(1));
    };
  }

  String getFullName(String name) {
    return configurationProperties.getPrefix() + "/" + name;
  }
}
