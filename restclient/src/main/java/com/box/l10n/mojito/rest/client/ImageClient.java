package com.box.l10n.mojito.rest.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Client to upload images.
 *
 * @author jaurambault
 */
@Component
public class ImageClient extends BaseClient {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(ImageClient.class);

    @Override
    public String getEntityName() {
        return "images";
    }

    /**
     * Uploads an image.
     *
     * @param name the image name
     * @param content the image content
     */
    public void uploadImage(String name, byte[] content) {
        logger.debug("Upload image with name = {}", name);
        authenticatedRestTemplate.put(getBasePathForEntity() + "/" + name, content);
    }

}
