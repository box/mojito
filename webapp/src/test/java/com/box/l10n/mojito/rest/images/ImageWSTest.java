package com.box.l10n.mojito.rest.images;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.http.MediaType;

/**
 * @author jeanaurambault
 */
public class ImageWSTest {

  @Test
  public void testGetMediaTypeFromImageNameNoExtension() {
    String imageName = "noextension";
    ImageWS instance = new ImageWS();
    MediaType expResult = MediaType.APPLICATION_OCTET_STREAM;
    MediaType result = instance.getMediaTypeFromImageName(imageName);
    assertEquals(expResult, result);
  }

  @Test
  public void testGetMediaTypeFromImageNamePng() {
    String imageName = "image1.png";
    ImageWS instance = new ImageWS();
    MediaType expResult = MediaType.IMAGE_PNG;
    MediaType result = instance.getMediaTypeFromImageName(imageName);
    assertEquals(expResult, result);
  }

  @Test
  public void testGetMediaTypeFromImageNamePngUpper() {
    String imageName = "image1.PNG";
    ImageWS instance = new ImageWS();
    MediaType expResult = MediaType.IMAGE_PNG;
    MediaType result = instance.getMediaTypeFromImageName(imageName);
    assertEquals(expResult, result);
  }

  @Test
  public void testGetMediaTypeFromImageNameJpg() {
    String imageName = "image1.jpg";
    ImageWS instance = new ImageWS();
    MediaType expResult = MediaType.IMAGE_JPEG;
    MediaType result = instance.getMediaTypeFromImageName(imageName);
    assertEquals(expResult, result);
  }

  @Test
  public void testGetMediaTypeFromImageNameJpeg() {
    String imageName = "image1.jpeg";
    ImageWS instance = new ImageWS();
    MediaType expResult = MediaType.IMAGE_JPEG;
    MediaType result = instance.getMediaTypeFromImageName(imageName);
    assertEquals(expResult, result);
  }

  @Test
  public void testGetMediaTypeFromImageNameGif() {
    String imageName = "image1.gif";
    ImageWS instance = new ImageWS();
    MediaType expResult = MediaType.IMAGE_GIF;
    MediaType result = instance.getMediaTypeFromImageName(imageName);
    assertEquals(expResult, result);
  }
}
