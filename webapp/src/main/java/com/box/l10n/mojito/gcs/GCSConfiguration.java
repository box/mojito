package com.box.l10n.mojito.gcs;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the Google Cloud Storage client.
 *
 * <p>Creates a {@link Storage} client using Application Default Credentials (ADC) when {@code
 * l10n.gcs.enabled=true}. Set {@code l10n.blob-storage.type=gcs} to use GCS as the blob storage
 * implementation.
 */
@Configuration
@ConditionalOnProperty("l10n.gcs.enabled")
public class GCSConfiguration {

  @Bean
  public Storage gcsStorage() {
    return StorageOptions.getDefaultInstance().getService();
  }
}
