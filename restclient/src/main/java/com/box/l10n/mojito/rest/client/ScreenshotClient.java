package com.box.l10n.mojito.rest.client;

import com.box.l10n.mojito.rest.entity.ScreenshotRun;
import com.box.l10n.mojito.rest.resttemplate.AuthenticatedRestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Client to upload screenshots.
 *
 * @author jaurambault
 */
@Component
public class ScreenshotClient extends BaseClient {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(ScreenshotClient.class);

    @Override
    public String getEntityName() {
        return "screenshots";
    }

    public void uploadScreenshots(ScreenshotRun screenshotRun) {
        logger.debug("Upload screenshots into repository = {}", screenshotRun.getRepository().getName());
        authenticatedRestTemplate.postForObject(getBasePath() + "/screenshots", screenshotRun, Void.class);
    }

    public AuthenticatedRestTemplate getAuthenticatedRestTemplate() {
        return authenticatedRestTemplate;
    }
  
}
