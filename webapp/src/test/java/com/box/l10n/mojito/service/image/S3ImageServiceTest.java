package com.box.l10n.mojito.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.entity.Image;
import com.box.l10n.mojito.service.blobstorage.s3.S3BlobStorage;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class S3ImageServiceTest {

  @Mock S3BlobStorage s3BlobStorageMock;

  S3ImageService s3ImageService;

  byte[] imageContent = new byte[] {1, 2, 3, 4, 5};

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    s3ImageService = new S3ImageService(s3BlobStorageMock, "image");
  }

  @Test
  public void testGetImageFromS3() {
    when(s3BlobStorageMock.getBytes(anyString())).thenReturn(Optional.of(imageContent));
    Optional<Image> image = s3ImageService.getImage("testImage");
    verify(s3BlobStorageMock, times(1)).getBytes("image/testImage");
    assertEquals("testImage", image.get().getName());
    assertEquals(imageContent, image.get().getContent());
  }

  @Test
  public void testImageNotAvailableInS3() {
    when(s3BlobStorageMock.getBytes(anyString())).thenReturn(Optional.empty());
    Optional<Image> image = s3ImageService.getImage("testImage");
    verify(s3BlobStorageMock, times(1)).getBytes("image/testImage");
    assertFalse(image.isPresent());
  }

  @Test
  public void testUploadImageToS3() {
    s3ImageService.uploadImage("testImage", imageContent);
    verify(s3BlobStorageMock, times(1)).put("image/testImage", imageContent);
  }
}
