package com.box.l10n.mojito.service.image;

import com.box.l10n.mojito.service.blobstorage.s3.S3BlobStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for {@link ImageService}
 *
 * {@link DatabaseImageService} is the default implementation.
 *
 * {@link S3ImageService} and {@link S3FallbackImageService} both require that configured {@link S3BlobStorage}
 *  and {@link com.amazonaws.services.s3.AmazonS3} client instances are available in the container.
 *
 */
@Configuration
public class ImageServiceConfiguration {

    @ConditionalOnProperty(value = "l10n.image-service.storage.type", havingValue = "s3Fallback")
    static class S3FallbackImageServiceConfiguration {

        @Autowired
        ImageRepository imageRepository;

        @Autowired
        S3BlobStorage s3BlobStorage;

        @Bean @Qualifier("databaseImageService")
        public DatabaseImageService databaseImageService() {
            return new DatabaseImageService(imageRepository);
        }

        @Bean @Qualifier("s3ImageService")
        public S3ImageService s3ImageService(S3BlobStorage s3BlobStorage, @Value("${l10n.image-service.storage.s3.prefix:image}") String s3PathPrefix) {
            return new S3ImageService(s3BlobStorage, s3PathPrefix);
        }

        @Bean
        public S3UploadImageAsyncTask s3UploadImageAsyncTask(S3ImageService s3ImageService) {
            return new S3UploadImageAsyncTask(s3ImageService);
        }

        @Bean @Primary
        public ImageService s3ImageFallback(@Qualifier("s3ImageService") S3ImageService s3ImageService,
                                            @Qualifier("databaseImageService") DatabaseImageService databaseImageService,
                                            S3UploadImageAsyncTask s3UploadImageAsyncTask) {
            return new S3FallbackImageService(s3ImageService, databaseImageService, s3UploadImageAsyncTask);
        }
    }

    @ConditionalOnProperty(value = "l10n.image-service.storage.type", havingValue = "s3")
    static class S3ImageServiceConfiguration {

        @Autowired
        S3BlobStorage s3BlobStorage;

        @Bean
        public ImageService s3ImageService(S3BlobStorage s3BlobStorage, @Value("${l10n.image-service.storage.s3.prefix:image}") String s3PathPrefix) {
            return new S3ImageService(s3BlobStorage, s3PathPrefix);
        }
    }

    @ConditionalOnProperty(value = "l10n.image-service.storage.type", havingValue = "database", matchIfMissing = true)
    static class DatabaseImageServiceConfiguration {

        @Autowired
        ImageRepository imageRepository;

        @Bean
        public ImageService databaseImageService() {
            return new DatabaseImageService(imageRepository);
        }
    }
}
