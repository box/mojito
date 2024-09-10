package com.box.l10n.mojito.service.assetcontent;

import com.box.l10n.mojito.service.blobstorage.s3.S3BlobStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ContentServiceConfiguration {

  @Configuration
  @ConditionalOnProperty(value = "l10n.asset-content-service.storage.type", havingValue = "s3")
  public static class S3ContentServiceConfiguration {

    @Autowired private S3BlobStorage s3BlobStorage;

    @Value("${l10n.asset-content-service.storage.s3.prefix:asset-content}")
    private String s3PathPrefix;

    @Bean
    public ContentService s3ContentService() {
      return new S3ContentService(this.s3BlobStorage, this.s3PathPrefix);
    }
  }

  @Configuration
  @ConditionalOnProperty(
      value = "l10n.asset-content-service.storage.type",
      havingValue = "s3Fallback")
  static class S3FallbackContentServiceConfiguration {

    @Autowired private S3BlobStorage s3BlobStorage;

    @Value("${l10n.asset-content-service.storage.s3.prefix:asset-content}")
    private String s3PathPrefix;

    @Bean
    public S3ContentService s3ContentService() {
      return new S3ContentService(this.s3BlobStorage, this.s3PathPrefix);
    }

    @Bean
    public S3UploadContentAsyncTask s3UploadContentAsyncTask(S3ContentService s3ContentService) {
      return new S3UploadContentAsyncTask(s3ContentService);
    }

    @Bean
    @Primary
    public ContentService s3FallbackContentService(
        S3ContentService s3ContentService, S3UploadContentAsyncTask s3UploadContentAsyncTask) {
      return new S3FallbackContentService(s3ContentService, s3UploadContentAsyncTask);
    }
  }
}
