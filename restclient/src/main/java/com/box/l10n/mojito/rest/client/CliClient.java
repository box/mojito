package com.box.l10n.mojito.rest.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/** @author jeanaurambault */
@Component
public class CliClient extends BaseClient {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(CliClient.class);

  @Override
  public String getBasePath() {
    return "";
  }

  @Override
  public String getEntityName() {
    return "cli";
  }

  public String getVersion() {
    UriComponentsBuilder uriComponentsBuilder =
        UriComponentsBuilder.fromPath(getBasePathForEntity()).pathSegment("version");
    return authenticatedRestTemplate.getForObject(uriComponentsBuilder.toUriString(), String.class);
  }
}
