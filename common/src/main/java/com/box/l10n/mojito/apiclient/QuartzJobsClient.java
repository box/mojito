package com.box.l10n.mojito.apiclient;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class QuartzJobsClient {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(QuartzJobsClient.class);

  @Autowired private QuartzWsApi quartzWsApi;

  public void deleteAllDynamicJobs() {
    logger.debug("deleteAllDynamicJobs");
    this.quartzWsApi.deleteAllDynamicJobs();
  }

  public List<String> getAllDynamicJobs() {
    logger.debug("getAllDynamicJobs");
    return this.quartzWsApi.getAllDynamicJobs();
  }
}
