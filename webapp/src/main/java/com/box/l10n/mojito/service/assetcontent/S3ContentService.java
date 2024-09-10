package com.box.l10n.mojito.service.assetcontent;

import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.entity.AssetContent;
import com.box.l10n.mojito.service.blobstorage.s3.S3BlobStorage;
import java.util.Optional;
import org.slf4j.Logger;

public class S3ContentService implements ContentService {
  private static final Logger LOGGER = getLogger(S3ContentService.class);

  static final String FILE_EXTENSION = "asset";

  private final S3BlobStorage s3BlobStorage;

  private final String s3PathPrefix;

  public S3ContentService(S3BlobStorage s3BlobStorage, String s3PathPrefix) {
    this.s3BlobStorage = s3BlobStorage;
    this.s3PathPrefix = s3PathPrefix;
  }

  private String getS3Path(AssetContent assetContent) {
    return String.format("%s/%d.%s", this.s3PathPrefix, assetContent.getId(), FILE_EXTENSION);
  }

  @Override
  public Optional<String> getContent(AssetContent assetContent) {
    LOGGER.debug("Get asset content from S3 with id: {}", assetContent.getId());
    return this.s3BlobStorage.getString(getS3Path(assetContent));
  }

  @Override
  public void setContent(AssetContent assetContent, String content) {
    LOGGER.debug("Upload asset content to S3 with id: {}", assetContent.getId());
    this.s3BlobStorage.put(getS3Path(assetContent), content);
  }
}
