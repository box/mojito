package com.box.l10n.mojito.service.image;

import com.box.l10n.mojito.entity.Image;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DatabaseImageServiceTest {

    @Mock
    ImageRepository imageRepository;

    DatabaseImageService databaseImageService;

    @Captor
    ArgumentCaptor<Image> imageCaptor;

    Image image;

    byte[] imageBytes = new byte[]{1,2,3,4,5};

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        image = new Image();
        image.setName("test");
        image.setContent(imageBytes);
        when(imageRepository.findByName("test")).thenReturn(Optional.of(image));
        databaseImageService = new DatabaseImageService(imageRepository);
    }

    @Test
    public void testGetImageFromDB() {
        Optional<Image> image = databaseImageService.getImage("test");
        assertTrue(image.isPresent());
        assertEquals("test", image.get().getName());
        assertEquals(imageBytes, image.get().getContent());
    }

    @Test
    public void testImageNotAvailableInDB() {
        when(imageRepository.findByName("imageNotFound")).thenReturn(Optional.empty());
        Optional<Image> image = databaseImageService.getImage("imageNotFound");
        assertFalse(image.isPresent());
    }

    @Test
    public void testUploadImageToDB() {
        byte[] content = new byte[] {6, 7, 8, 9, 10};
        databaseImageService.uploadImage("otherImage", content);
        verify(imageRepository, times(1)).findByName("otherImage");
        verify(imageRepository, times(1)).save(imageCaptor.capture());
        Image imageCaptorValue = imageCaptor.getValue();
        assertEquals("otherImage", imageCaptorValue.getName());
        assertEquals(content, imageCaptorValue.getContent());
    }

    @Test
    public void testUploadNewVersionOfExistingImageInDB() {
        byte[] content = new byte[] {6, 7, 8, 9, 10};
        databaseImageService.uploadImage("test", content);
        verify(imageRepository, times(1)).findByName("test");
        verify(imageRepository, times(1)).save(imageCaptor.capture());
        Image imageCaptorValue = imageCaptor.getValue();
        assertEquals("test", imageCaptorValue.getName());
        assertEquals(content, imageCaptorValue.getContent());
    }
}
