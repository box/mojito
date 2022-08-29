package com.box.l10n.mojito.service.image;

import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.entity.Image;
import java.util.Optional;
import org.slf4j.Logger;

/**
 * Service to uploadImage and serve images.
 *
 * @author jeanaurambault
 */
public class DatabaseImageService implements ImageService {

  /** logger */
  static Logger logger = getLogger(ImageService.class);

  ImageRepository imageRepository;

  public DatabaseImageService(ImageRepository imageRepository) {
    this.imageRepository = imageRepository;
  }

  public Optional<Image> getImage(String name) {
    logger.debug("Get image with name: {}", name);
    return imageRepository.findByName(name);
  }

  public void uploadImage(String name, byte[] content) {

    logger.debug("Upload image with name: {}", name);

    Image image =
        imageRepository
            .findByName(name)
            .orElseGet(
                () -> {
                  Image img = new Image();
                  img.setName(name);
                  return img;
                });

    image.setContent(content);

    imageRepository.save(image);
  }
}
