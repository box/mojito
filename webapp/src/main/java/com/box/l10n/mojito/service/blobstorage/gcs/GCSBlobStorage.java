package com.box.l10n.mojito.service.blobstorage.gcs;

import com.box.l10n.mojito.service.blobstorage.BlobStorage;
import com.box.l10n.mojito.service.blobstorage.Retention;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.common.base.Preconditions;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation that uses Google Cloud Storage to store blobs.
 *
 * <p>Uses Application Default Credentials (ADC) for authentication. ADC is automatically used when
 * you run on Google Cloud (GCE, GKE, Cloud Run) or when {@code GOOGLE_APPLICATION_CREDENTIALS} is
 * set to a service account key file, or after {@code gcloud auth application-default login}.
 *
 * <p>Rely on GCS lifecycle rules to cleanup expired blobs. This must be setup manually in the
 * bucket; otherwise no cleanup will happen.
 *
 * <p>Objects will have a "retention" custom metadata field, see values in {@link Retention}. You
 * can configure lifecycle rules to delete objects with retention=ephemeral (or equivalent) after a
 * given age.
 *
 * <p>See https://cloud.google.com/storage/docs/lifecycle for details.
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
    metadata.put("retention", retention.toString());
    put(name, content, retention, contentType, metadata);
  }

  void put(
      String name,
      byte[] content,
      Retention retention,
      String contentType,
      Map<String, String> metadata) {
    Map<String, String> fullMetadata = new HashMap<>(metadata != null ? metadata : Map.of());
    fullMetadata.put("retention", retention.toString());

    BlobInfo blobInfo =
        BlobInfo.newBuilder(BlobId.of(configurationProperties.getBucket(), getFullName(name)))
            .setContentType(contentType)
            .setMetadata(fullMetadata)
            .build();

    storage.create(blobInfo, content);
  }

  /** Returns the GCS URI for the given blob name (e.g. gs://bucket/prefix/name). */
  public String getGcsUri(String name) {
    return String.format("gs://%s/%s", configurationProperties.getBucket(), getFullName(name));
  }

  String getFullName(String name) {
    return configurationProperties.getPrefix() + "/" + name;
  }
}
