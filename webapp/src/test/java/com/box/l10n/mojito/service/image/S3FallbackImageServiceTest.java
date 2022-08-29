package com.box.l10n.mojito.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.entity.Image;
import com.box.l10n.mojito.service.blobstorage.s3.S3BlobStorage;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
    classes = {
      S3FallbackImageServiceTest.class,
      ImageServiceConfiguration.class,
      S3FallbackImageService.class
    },
    properties = {
      "l10n.image-service.storage.type=s3Fallback",
      "l10n.blob-storage.type=s3",
      "l10n.aws.s3.enabled=true"
    })
public class S3FallbackImageServiceTest {

  @Configuration
  static class S3FallbackImageServiceTestConfiguration {

    @MockBean ImageRepository imageRepository;

    @MockBean S3BlobStorage s3BlobStorage;

    @Bean("databaseImageService")
    public DatabaseImageService databaseImageService() {
      return Mockito.spy(new DatabaseImageService(imageRepository));
    }

    @Bean("s3ImageService")
    public S3ImageService s3ImageService(
        S3BlobStorage s3BlobStorage,
        @Value("${l10n.image-service.storage.s3.prefix:image}") String s3PathPrefix) {
      return Mockito.spy(new S3ImageService(s3BlobStorage, s3PathPrefix));
    }

    @Bean
    public S3UploadImageAsyncTask s3UploadImageAsyncTask(S3ImageService s3ImageService) {
      return Mockito.spy(new S3UploadImageAsyncTask(s3ImageService));
    }

    @Bean
    @Primary
    public ImageService s3ImageFallback(
        @Qualifier("s3ImageService") S3ImageService s3ImageService,
        @Qualifier("databaseImageService") DatabaseImageService databaseImageService,
        S3UploadImageAsyncTask s3UploadImageAsyncTask) {
      return new S3FallbackImageService(
          s3ImageService, databaseImageService, s3UploadImageAsyncTask);
    }
  }

  @MockBean S3BlobStorage s3BlobStorageMock;

  @MockBean ImageRepository imageRepositoryMock;

  @SpyBean S3UploadImageAsyncTask s3UploadImageAsyncTaskSpy;

  @SpyBean S3ImageService s3ImageService;

  @Autowired S3FallbackImageService s3FallbackImageService;

  byte[] imageBytes = new byte[] {1, 2, 3, 4, 5};

  Optional<byte[]> imageContent;

  @Before
  public void setup() {
    imageContent = Optional.of(imageBytes);
  }

  @Test
  public void testGetImageFromS3() {
    when(s3BlobStorageMock.getBytes(anyString())).thenReturn(imageContent);
    Optional<Image> image = s3FallbackImageService.getImage("testName");
    assertEquals("testName", image.get().getName());
    assertEquals(imageBytes, image.get().getContent());
    verify(s3ImageService, times(1)).getImage("testName");
    verify(s3BlobStorageMock, times(1)).getBytes("image/testName");
    verifyNoInteractions(imageRepositoryMock, s3UploadImageAsyncTaskSpy);
  }

  @Test
  public void testUploadImageToS3() {
    s3FallbackImageService.uploadImage("testImage", imageBytes);
    verify(s3BlobStorageMock, times(1)).put("image/testImage", imageBytes);
    verifyNoInteractions(imageRepositoryMock, s3UploadImageAsyncTaskSpy);
  }

  @Test
  public void testDatabaseCheckedForImageIfNotInS3() {
    when(s3BlobStorageMock.getBytes(anyString())).thenReturn(Optional.empty());
    when(imageRepositoryMock.findByName("test")).thenReturn(Optional.empty());
    Optional<Image> image = s3FallbackImageService.getImage("test");
    assertFalse(image.isPresent());
    verify(s3ImageService, times(1)).getImage("test");
    verify(s3BlobStorageMock, times(1)).getBytes("image/test");
    verify(imageRepositoryMock, times(1)).findByName("test");
    verifyNoInteractions(s3UploadImageAsyncTaskSpy);
  }

  @Test
  public void testImageUploadedToS3IfFoundInDB() {
    Image image = new Image();
    image.setName("test");
    image.setContent(imageBytes);
    when(s3BlobStorageMock.getBytes(anyString())).thenReturn(Optional.empty());
    when(imageRepositoryMock.findByName("test")).thenReturn(Optional.of(image));
    Optional<Image> retrievedImage = s3FallbackImageService.getImage("test");
    assertTrue(retrievedImage.isPresent());
    assertEquals("test", image.getName());
    assertEquals(imageBytes, retrievedImage.get().getContent());
    verify(s3ImageService, times(1)).getImage("test");
    verify(s3BlobStorageMock, times(1)).getBytes("image/test");
    verify(imageRepositoryMock, times(1)).findByName("test");
    verify(s3UploadImageAsyncTaskSpy, times(1))
        .uploadImageToS3("test", retrievedImage.get().getContent());
    verify(s3BlobStorageMock, timeout(3000).times(1))
        .put("image/test", retrievedImage.get().getContent());
  }
}
