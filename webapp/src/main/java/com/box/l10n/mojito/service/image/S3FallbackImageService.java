package com.box.l10n.mojito.service.image;

import com.box.l10n.mojito.entity.Image;
import org.slf4j.Logger;

import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Service that retrieves images from an S3 bucket and falls back to the Mojito DB for retrieval if
 * an image is not available in the S3 bucket.
 *
 * If a requested image cannot be found in S3 an attempt will be made to retrieve from the Mojito DB, if the image is
 * found in the DB then an async task will be executed to upload the image to S3.
 *
 * @author maallen
 */
public class S3FallbackImageService implements ImageService {

    static Logger logger = getLogger(S3FallbackImageService.class);

    S3ImageService s3ImageService;

    DatabaseImageService databaseImageService;

    S3UploadImageAsyncTask s3UploadImageAsyncTask;

    public S3FallbackImageService(S3ImageService s3ImageService, DatabaseImageService databaseImageService,
                                  S3UploadImageAsyncTask s3UploadImageAsyncTask) {
        this.s3ImageService = s3ImageService;
        this.databaseImageService = databaseImageService;
        this.s3UploadImageAsyncTask = s3UploadImageAsyncTask;
    }

    @Override
    public Optional<Image> getImage(String name) {
        logger.debug("Attempt image retrieval from S3 with name: {}", name);
        Image image = s3ImageService.getImage(name).orElseGet(() ->
                databaseImageService.getImage(name).map(img -> {
                    logger.debug("Found image {} in database, triggering async upload to S3", img.getName());
                    s3UploadImageAsyncTask.uploadImageToS3(img.getName(), img.getContent());
                    return img;
                }).orElse(null));
        return Optional.ofNullable(image);
    }

    @Override
    public void uploadImage(String name, byte[] content) {
        s3ImageService.uploadImage(name, content);
    }

}
