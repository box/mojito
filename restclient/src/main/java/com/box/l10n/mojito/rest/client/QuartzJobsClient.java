package com.box.l10n.mojito.rest.client;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class QuartzJobsClient extends BaseClient {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(QuartzJobsClient.class);

  @Override
  public String getEntityName() {
    return "quartz";
  }

  public List<String> getAllDynamicJobs() {
    logger.debug("getAllDynamicJobs");
    UriComponentsBuilder uriBuilder =
        UriComponentsBuilder.fromPath(getBasePathForEntity()).pathSegment("jobs").path("dynamic");
    return authenticatedRestTemplate.getForObject(uriBuilder.toUriString(), List.class);
  }

  public void deleteAllDynamicJobs() {
    logger.debug("deleteAllDynamicJobs");
    UriComponentsBuilder uriBuilder =
        UriComponentsBuilder.fromPath(getBasePathForEntity()).pathSegment("jobs").path("dynamic");
    authenticatedRestTemplate.delete(uriBuilder.toUriString());
  }
}
