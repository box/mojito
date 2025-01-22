package com.box.l10n.mojito.cli.apiclient;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class QuartzWsApiProxy {
  /** logger */
  static Logger logger = LoggerFactory.getLogger(QuartzWsApiProxy.class);

  @Autowired private QuartzWsApi quartzClient;

  public void deleteAllDynamicJobs() {
    logger.debug("deleteAllDynamicJobs");
    this.quartzClient.deleteAllDynamicJobs();
  }

  public List<String> getAllDynamicJobs() {
    logger.debug("getAllDynamicJobs");
    return this.quartzClient.getAllDynamicJobs();
  }
}
