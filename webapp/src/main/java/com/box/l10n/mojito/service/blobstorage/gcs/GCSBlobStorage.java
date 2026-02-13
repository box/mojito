package com.box.l10n.mojito.service.blobstorage.gcs;

import com.box.l10n.mojito.service.blobstorage.BlobStorage;
import com.box.l10n.mojito.service.blobstorage.Retention;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.common.base.Preconditions;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
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

  public GCSBlobStorage(
      Storage storage, GCSBlobStorageConfigurationProperties configurationProperties) {
    Preconditions.checkNotNull(storage);
    Preconditions.checkNotNull(configurationProperties);
    this.storage = storage;
    this.configurationProperties = configurationProperties;
  }

  @Override
  public Optional<byte[]> getBytes(String name) {
    Blob blob = storage.get(BlobId.of(configurationProperties.getBucket(), getFullName(name)));
    if (blob == null) {
      return Optional.empty();
    }
    return Optional.of(blob.getContent());
  }

  @Override
  public Optional<String> getString(String name) {
    return getBytes(name).map(bytes -> new String(bytes, StandardCharsets.UTF_8));
  }

  @Override
  public void put(String name, byte[] content, Retention retention) {
    put(name, content, retention, "application/octet-stream");
  }

  @Override
  public void delete(String name) {
    storage.delete(BlobId.of(configurationProperties.getBucket(), getFullName(name)));
  }

  @Override
  public boolean exists(String name) {
    Blob blob = storage.get(BlobId.of(configurationProperties.getBucket(), getFullName(name)));
    return blob != null;
  }

  @Override
  public void put(String name, String content, Retention retention) {
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
    Map<String, String> metadata = new HashMap<>();
    metadata.put("Content-Type", "text/plain");
    metadata.put("Content-Encoding", StandardCharsets.UTF_8.toString());
    put(name, bytes, retention, "text/plain", metadata);
  }

  void put(String name, byte[] content, Retention retention, String contentType) {
    Map<String, String> metadata = new HashMap<>();
    put(name, content, retention, contentType, metadata);
  }

  void put(
      String name,
      byte[] content,
      Retention retention,
      String contentType,
      Map<String, String> metadata) {
    Map<String, String> fullMetadata = new HashMap<>(metadata != null ? metadata : Map.of());
    BlobInfo.Builder builder =
        BlobInfo.newBuilder(BlobId.of(configurationProperties.getBucket(), getFullName(name)))
            .setContentType(contentType)
            .setMetadata(fullMetadata);

    customTimeAtEndOfRetention(retention).ifPresent(builder::setCustomTimeOffsetDateTime);

    storage.create(builder.build(), content);
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
