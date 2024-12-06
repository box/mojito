package com.box.l10n.mojito.rest.ai;

import static com.box.l10n.mojito.CacheType.Names.AI_CHECKS;

import com.box.l10n.mojito.service.ai.LLMService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(value = "l10n.ai.enabled", havingValue = "true")
public class AIChecksWS {

  static Logger logger = LoggerFactory.getLogger(AIChecksWS.class);

  @Autowired LLMService llmService;

  @Operation(summary = "Execute AI checks")
  @RequestMapping(value = "/api/ai/checks", method = RequestMethod.POST)
  @Timed("AIWS.executeAIChecks")
  @Cacheable(AI_CHECKS)
  public AICheckResponse executeAIChecks(@RequestBody AICheckRequest AICheckRequest) {
    logger.debug("Received request to execute AI checks");
    AICheckResponse response;

    try {
      response = llmService.executeAIChecks(AICheckRequest);
    } catch (AIException e) {
      response = buildErrorInCheckResponse(e);
    }

    return response;
  }

  private static AICheckResponse buildErrorInCheckResponse(AIException e) {
    AICheckResponse response;
    logger.error("Error executing AI checks", e);
    response = new AICheckResponse();
    response.setError(true);
    response.setErrorMessage(e.getMessage());
    return response;
  }
}
