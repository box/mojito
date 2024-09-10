package com.box.l10n.mojito.service.assetcontent;

import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.entity.AssetContent;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

public class S3FallbackContentService implements ContentService {
  private static final Logger LOGGER = getLogger(S3FallbackContentService.class);

  private final S3ContentService s3ContentService;

  private final S3UploadContentAsyncTask s3UploadContentAsyncTask;

  public S3FallbackContentService(
      S3ContentService s3ContentService, S3UploadContentAsyncTask s3UploadContentAsyncTask) {
    this.s3ContentService = s3ContentService;
    this.s3UploadContentAsyncTask = s3UploadContentAsyncTask;
  }

  @Override
  public Optional<String> getContent(AssetContent assetContent) {
    LOGGER.debug("Attempt asset content retrieval from S3 with id: {}", assetContent.getId());
    String content =
        this.s3ContentService
            .getContent(assetContent)
            .orElseGet(
                () ->
                    ofNullable(
                            StringUtils.isBlank(assetContent.getContent())
                                ? null
                                : assetContent.getContent())
                        .map(
                            actualContent -> {
                              LOGGER.debug(
                                  "Found asset content {} in database, triggering async upload to S3",
                                  assetContent.getId());
                              this.s3UploadContentAsyncTask.uploadAssetContentToS3(
                                  assetContent, actualContent);
                              return actualContent;
                            })
                        .orElse(null));
    return ofNullable(content);
  }

  @Override
  public void setContent(AssetContent assetContent, String content) {
    this.s3ContentService.setContent(assetContent, content);
  }
}
