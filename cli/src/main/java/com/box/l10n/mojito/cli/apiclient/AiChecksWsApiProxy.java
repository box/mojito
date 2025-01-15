package com.box.l10n.mojito.cli.apiclient;

import com.box.l10n.mojito.cli.model.AICheckRequest;
import com.box.l10n.mojito.cli.model.AICheckResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AiChecksWsApiProxy {
  /** logger */
  static Logger logger = LoggerFactory.getLogger(AiChecksWsApiProxy.class);

  @Autowired private AiChecksWsApi aiCheckClient;

  public AICheckResponse executeAIChecks(AICheckRequest body) {
    logger.debug("Received request to execute AI checks");
    return this.aiCheckClient.executeAIChecks(body);
  }
}
