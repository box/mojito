package com.box.l10n.mojito.service.evolve;

import com.box.l10n.mojito.quartz.QuartzPollableJob;
import org.quartz.DisallowConcurrentExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty("l10n.evolve.url")
@DisallowConcurrentExecution
public class EvolveSyncJob extends QuartzPollableJob<EvolveSyncJobInput, Void> {
  /** logger */
  static Logger logger = LoggerFactory.getLogger(EvolveSyncJob.class);

  @Autowired private EvolveService evolveService;

  @Override
  public Void call(EvolveSyncJobInput input) {
    logger.debug("Run EvolveSyncJob");
    this.evolveService.sync(input.getRepositoryId(), input.getLocaleMapping());
    return null;
  }
}
