package com.box.l10n.mojito.rest.images;

import com.box.l10n.mojito.entity.Image;
import com.box.l10n.mojito.service.image.ImageService;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import static org.slf4j.LoggerFactory.getLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Simple WS to uploadImage and serve images.
 *
 * @author jaurambault
 */
@RestController
public class ImageWS {

    /**
     * logger
     */
    static Logger logger = getLogger(ImageWS.class);

    static String PATH_PREFIX = "/api/images/";

    @Autowired
    ImageService imageService;

    @RequestMapping(value = "/api/images/**", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity getImage(HttpServletRequest httpServletRequest) throws IOException {

        String imageName = getImageNameFromRequest(httpServletRequest);

        Image image = imageService.getImage(imageName);

        ResponseEntity responseEntity = null;

        if (image != null) {
            responseEntity = ResponseEntity
                    .ok()
                    .contentType(getMediaTypeFromImageName(imageName))
                    .body(image.getContent());
        } else {
            responseEntity = ResponseEntity.notFound().build();
        }
        
        return responseEntity;
    }

    @RequestMapping(value = "/api/images/**", method = RequestMethod.PUT)
    @ResponseBody
    public void uploadImage(@RequestBody byte[] imageContent, HttpServletRequest httpServletRequest) {
        String imageName = getImageNameFromRequest(httpServletRequest);
        logger.debug("Uploading image: {}", imageName);
        imageService.uploadImage(imageName, imageContent);
    }

    /**
     * Get the image name/path from the request.
     *
     * @param httpServletRequest
     * @return
     */
    String getImageNameFromRequest(HttpServletRequest httpServletRequest) {
        String path = (String) httpServletRequest.getAttribute(
                HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

        return path.substring(PATH_PREFIX.length());
    }

    /**
     * Get the media type of an image based on its extension. Supported types
     * are JPEG, PNG and GIF. If there is no match based on the extension,
     * MediaType.APPLICATION_OCTET_STREAM is returned.
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
