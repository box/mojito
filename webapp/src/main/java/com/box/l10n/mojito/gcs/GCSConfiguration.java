package com.box.l10n.mojito.gcs;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty("l10n.gcs.enabled")
public class GCSConfiguration {

  static Logger logger = LoggerFactory.getLogger(GCSConfiguration.class);

  @Bean
  public Storage storageClient(GCSConfigurationProperties gcsConfigurationProperties) {
    if (gcsConfigurationProperties.getProjectId() == null) {
      logger.debug("Project ID is not set, using default StorageOptions instance");
      return StorageOptions.getDefaultInstance().getService();
    }

    return StorageOptions.newBuilder()
        .setProjectId(gcsConfigurationProperties.getProjectId())
        .build()
        .getService();
  }
}
