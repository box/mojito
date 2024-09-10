package com.box.l10n.mojito.service.assetcontent;

import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.entity.AssetContent;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Async;

public class S3UploadContentAsyncTask {
  private static final Logger LOGGER = getLogger(S3UploadContentAsyncTask.class);

  private final S3ContentService s3ContentService;

  public S3UploadContentAsyncTask(S3ContentService s3ContentService) {
    this.s3ContentService = s3ContentService;
  }

  @Async
  public void uploadAssetContentToS3(AssetContent assetContent, String content) {
    LOGGER.debug("Uploading asset content {} to S3", assetContent.getId());
    this.s3ContentService.setContent(assetContent, content);
  }
}
