package com.box.l10n.mojito.cli.apiclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ImageWsApiProxy {
  /** logger */
  static Logger logger = LoggerFactory.getLogger(ImageWsApiProxy.class);

  @Autowired private ImageWsApi imageClient;

  public void uploadImage(byte[] body, String imageName) {
    logger.debug("Upload image with name = {}", imageName);
    this.imageClient.uploadImage(body, imageName);
  }
}
