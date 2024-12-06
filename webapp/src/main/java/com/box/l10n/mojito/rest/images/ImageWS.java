package com.box.l10n.mojito.rest.images;

import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.entity.Image;
import com.box.l10n.mojito.service.image.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import java.io.IOException;
import java.util.Optional;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple WS to uploadImage and serve images.
 *
 * @author jaurambault
 */
@RestController
public class ImageWS {

  /** logger */
  static Logger logger = getLogger(ImageWS.class);

  @Autowired ImageService imageService;

  private String getFixedImageName(String imageName) {
    return imageName.startsWith("/") ? imageName.substring(1) : imageName;
  }

  @Operation(summary = "Get an image by its name")
  @RequestMapping(value = "/api/images/{*imageName}", method = RequestMethod.GET)
  @ResponseBody
  public ResponseEntity<byte[]> getImage(@PathVariable String imageName) throws IOException {
    String fixedImageName = this.getFixedImageName(imageName);
    Optional<Image> image = imageService.getImage(fixedImageName);

    return image
        .map(
            i ->
                ResponseEntity.ok()
                    .contentType(getMediaTypeFromImageName(fixedImageName))
                    .body(image.get().getContent()))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @Operation(summary = "Upload an image")
  @RequestMapping(value = "/api/images/{*imageName}", method = RequestMethod.PUT)
  @ResponseBody
  public void uploadImage(@RequestBody byte[] imageContent, @PathVariable String imageName) {
    String fixedImageName = this.getFixedImageName(imageName);
    logger.debug("Uploading image: {}", fixedImageName);
    imageService.uploadImage(fixedImageName, imageContent);
  }

  /**
   * Get the media type of an image based on its extension. Supported types are JPEG, PNG and GIF.
   * If there is no match based on the extension, MediaType.APPLICATION_OCTET_STREAM is returned.
   *
   * @param imageName an image name
   * @return the media type of the image
   */
  MediaType getMediaTypeFromImageName(String imageName) {
    MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;

    String fileExtension = FilenameUtils.getExtension(imageName).toLowerCase();

    switch (fileExtension) {
      case "jpeg":
      case "jpg":
        mediaType = MediaType.IMAGE_JPEG;
        break;
      case "png":
        mediaType = MediaType.IMAGE_PNG;
        break;
      case "gif":
        mediaType = MediaType.IMAGE_GIF;
        break;
    }

    return mediaType;
  }
}
