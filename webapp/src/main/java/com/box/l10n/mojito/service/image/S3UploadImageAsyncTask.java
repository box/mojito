package com.box.l10n.mojito.service.image;

import com.box.l10n.mojito.service.blobstorage.s3.S3BlobStorage;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Async task to upload an image to S3.
 *
 * @author maallen
 */
public class S3UploadImageAsyncTask {

    /**
     * logger
     */
    static Logger logger = getLogger(S3UploadImageAsyncTask.class);

    S3ImageService s3ImageService;

    public S3UploadImageAsyncTask(S3ImageService s3ImageService) {
        this.s3ImageService = s3ImageService;
    }

    @Async
    public void uploadImageToS3(String name, byte[] content){
        logger.debug("Uploading image {} to S3", name);
        s3ImageService.uploadImage(name, content);
    }
}
