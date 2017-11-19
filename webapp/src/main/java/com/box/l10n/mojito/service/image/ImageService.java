package com.box.l10n.mojito.service.image;

import com.box.l10n.mojito.entity.Image;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Service to uploadImage and serve images.
 *
 * @author jeanaurambault
 */
@Component
public class ImageService {

    /**
     * logger
     */
    static Logger logger = getLogger(ImageService.class);
    
    @Autowired
    ImageRepository imageRepository;

    public Image getImage(String name) {
        logger.debug("Get image with name: {}", name);
        Image image = imageRepository.findByName(name);
        return image;
    }

    public void uploadImage(String name, byte[] content) {

        logger.debug("Upload image with name: {}", name);
        Image image;
        Image prevVersion = imageRepository.findByName(name);

        if (prevVersion != null) {
            prevVersion.setContent(content);
            image = prevVersion;
        } else {
            image = new Image();
            image.setName(name);
            image.setContent(content);
        }

        imageRepository.save(image);
    }
}
