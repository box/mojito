package com.box.l10n.mojito.service.image;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Async;

/**
 * Async task to upload an image to S3.
 *
 * @author maallen
 */
public class S3UploadImageAsyncTask {

  /** logger */
  static Logger logger = getLogger(S3UploadImageAsyncTask.class);

  S3ImageService s3ImageService;

  public S3UploadImageAsyncTask(S3ImageService s3ImageService) {
    this.s3ImageService = s3ImageService;
  }

  @Async
  public void uploadImageToS3(String name, byte[] content) {
    logger.debug("Uploading image {} to S3", name);
    s3ImageService.uploadImage(name, content);
  }
}
