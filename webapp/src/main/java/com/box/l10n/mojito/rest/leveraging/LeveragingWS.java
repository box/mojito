package com.box.l10n.mojito.rest.leveraging;

import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.rest.asset.AssetWithIdNotFoundException;
import com.box.l10n.mojito.rest.repository.RepositoryWithIdNotFoundException;
import com.box.l10n.mojito.service.leveraging.LeveragingService;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jaurambault
 */
@RestController
public class LeveragingWS {

  /** logger */
  static Logger logger = getLogger(LeveragingWS.class);

  @Autowired RepositoryRepository repositoryRepository;

  @Autowired LeveragingService leveragingService;

  /**
   * Copy the TM of a source repository into the a target repository.
   *
   * @param copyTmConfig config to perform the copy (source and target repositories).
   * @return the config updated with a pollable task
   */
  @Operation(summary = "Copy the TM of a source repository into the a target repository")
  @RequestMapping(
      value = "/api/leveraging/copyTM",
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public CopyTmConfig copyTM(@RequestBody CopyTmConfig copyTmConfig)
      throws AssetWithIdNotFoundException, RepositoryWithIdNotFoundException {
    logger.debug("Copy repository TM");
    PollableFuture<Void> pollableFuture = leveragingService.copyTm(copyTmConfig);
    copyTmConfig.setPollableTask(pollableFuture.getPollableTask());
    return copyTmConfig;
  }
}
