package com.box.l10n.mojito.service.assetcontent;

import static com.box.l10n.mojito.service.assetcontent.S3ContentService.FILE_EXTENSION;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.entity.AssetContent;
import com.box.l10n.mojito.service.blobstorage.s3.S3BlobStorage;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
    classes = {
      S3FallbackContentServiceTest.class,
      S3FallbackContentServiceTest.S3FallbackContentServiceTestConfiguration.class
    },
    properties = {
      "l10n.asset-content-service.storage.type=s3Fallback",
      "l10n.blob-storage.type=s3",
      "l10n.aws.s3.enabled=true"
    })
public class S3FallbackContentServiceTest {

  @TestConfiguration
  static class S3FallbackContentServiceTestConfiguration {

    @Value("${l10n.asset-content-service.storage.s3.prefix:asset-content}")
    private String s3PathPrefix;

    @Bean
    public S3BlobStorage s3BlobStorage() {
      return Mockito.mock(S3BlobStorage.class);
    }

    @Bean
    public S3ContentService s3ContentService(S3BlobStorage s3BlobStorage) {
      return Mockito.spy(new S3ContentService(s3BlobStorage, this.s3PathPrefix));
    }

    @Bean
    public S3UploadContentAsyncTask s3UploadContentAsyncTask(S3ContentService s3ContentService) {
      return Mockito.spy(new S3UploadContentAsyncTask(s3ContentService));
    }

    @Bean
    @Primary
    public ContentService s3FallbackContentService(
        S3ContentService s3ContentService, S3UploadContentAsyncTask s3UploadContentAsyncTask) {
      return new S3FallbackContentService(s3ContentService, s3UploadContentAsyncTask);
    }
  }

  @Autowired S3BlobStorage s3BlobStorage;

  @Autowired S3ContentService s3ContentService;

  @Autowired S3UploadContentAsyncTask s3UploadContentAsyncTask;

  @Autowired ContentService s3FallbackContentService;

  String content;

  AssetContent assetContent;

  String s3PathPrefix;

  String pathPlaceholder;

  @Before
  public void setup() {
    this.content = "asset_content_1";
    this.s3PathPrefix = "asset-content";
    this.pathPlaceholder = "%s/%d.%s";
    this.assetContent = Mockito.mock(AssetContent.class);
    when(this.assetContent.getId()).thenReturn(1L);
    Mockito.reset(this.s3BlobStorage, this.s3ContentService, s3UploadContentAsyncTask);
  }

  @Test
  public void testGetContentFromS3() {
    when(this.s3BlobStorage.getString(anyString())).thenReturn(of(this.content));
    Optional<String> actualContent = this.s3FallbackContentService.getContent(this.assetContent);
    assertTrue(actualContent.isPresent());
    assertEquals(this.content, actualContent.get());
    verify(this.s3ContentService, times(1)).getContent(any());
    verify(this.s3BlobStorage, times(1))
        .getString(
            String.format(
                this.pathPlaceholder,
                this.s3PathPrefix,
                this.assetContent.getId(),
                FILE_EXTENSION));
    verify(this.assetContent, times(0)).getContent();
    verifyNoInteractions(this.s3UploadContentAsyncTask);
  }

  @Test
  public void testGetContentFromDatabase() {
    when(this.s3BlobStorage.getString(anyString())).thenReturn(empty());
    when(this.assetContent.getContent()).thenReturn(this.content);
    Optional<String> actualContent = this.s3FallbackContentService.getContent(this.assetContent);
    assertTrue(actualContent.isPresent());
    verify(this.s3ContentService, times(1)).getContent(any());
    verify(this.s3BlobStorage, times(1))
        .getString(
            String.format(
                this.pathPlaceholder,
                this.s3PathPrefix,
                this.assetContent.getId(),
                FILE_EXTENSION));
    verify(this.assetContent, times(2)).getContent();
    verify(this.s3UploadContentAsyncTask, times(1)).uploadAssetContentToS3(any(), eq(this.content));
  }

  @Test
  public void testGetContentNoResult() {
    when(this.s3BlobStorage.getString(anyString())).thenReturn(empty());
    when(this.assetContent.getContent()).thenReturn("");
    Optional<String> actualContent = this.s3FallbackContentService.getContent(this.assetContent);
    assertFalse(actualContent.isPresent());
    verify(this.s3ContentService, times(1)).getContent(any());
    verify(this.s3BlobStorage, times(1))
        .getString(
            String.format(
                this.pathPlaceholder,
                this.s3PathPrefix,
                this.assetContent.getId(),
                FILE_EXTENSION));
    verify(this.assetContent, times(1)).getContent();
    verifyNoInteractions(this.s3UploadContentAsyncTask);
  }

  @Test
  public void testSetContentToS3() {
    this.s3FallbackContentService.setContent(this.assetContent, this.content);
    verify(this.s3ContentService, times(1)).setContent(any(), eq(this.content));
    verify(this.s3BlobStorage, times(1))
        .put(
            String.format(
                this.pathPlaceholder, this.s3PathPrefix, this.assetContent.getId(), FILE_EXTENSION),
            this.content);
  }
}
