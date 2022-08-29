package com.box.l10n.mojito.service.image;

import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.entity.Image;
import com.box.l10n.mojito.service.blobstorage.s3.S3BlobStorage;
import java.util.Optional;
import org.slf4j.Logger;

/**
 * Service to upload and retrieve images from S3.
 *
 * <p>Configured {@link S3BlobStorage} and {@link com.amazonaws.services.s3.AmazonS3} client
 * instances are required to upload and retrieve images from S3.
 *
 * @author maallen
 */
public class S3ImageService implements ImageService {

  /** logger */
  static Logger logger = getLogger(S3ImageService.class);

  S3BlobStorage s3BlobStorage;

  String s3PathPrefix;

  public S3ImageService(S3BlobStorage s3BlobStorage, String s3PathPrefix) {
    this.s3BlobStorage = s3BlobStorage;
    this.s3PathPrefix = s3PathPrefix;
  }

  public Optional<Image> getImage(String name) {
    logger.debug("Get image from S3 with name: {}", name);

    return s3BlobStorage
        .getBytes(getS3Path(name))
        .map(
            bytes -> {
              Image image = new Image();
              image.setName(name);
              image.setContent(bytes);
              return image;
            });
  }

  public void uploadImage(String name, byte[] content) {
    logger.debug("Upload image to S3 with name: {}", name);
    s3BlobStorage.put(getS3Path(name), content);
  }

  private String getS3Path(String name) {
    return s3PathPrefix + "/" + name;
  }
}
