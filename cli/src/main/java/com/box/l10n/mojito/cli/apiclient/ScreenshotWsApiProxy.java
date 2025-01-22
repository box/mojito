package com.box.l10n.mojito.cli.apiclient;

import com.box.l10n.mojito.cli.model.ScreenshotRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ScreenshotWsApiProxy {
  /** logger */
  static Logger logger = LoggerFactory.getLogger(ScreenshotWsApiProxy.class);

  @Autowired private ScreenshotWsApi screenshotClient;

  public ScreenshotRun createOrAddToScreenshotRun(ScreenshotRun body) {
    if (body.getRepository() != null) {
      logger.debug("Upload screenshots into repository = {}", body.getRepository().getName());
    } else {
      logger.debug("Upload screenshots for run with id = {}", body.getId());
    }
    return this.screenshotClient.createOrAddToScreenshotRun(body);
  }
}
