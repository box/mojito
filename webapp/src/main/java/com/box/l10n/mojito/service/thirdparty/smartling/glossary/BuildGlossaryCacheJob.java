package com.box.l10n.mojito.service.thirdparty.smartling.glossary;

import com.box.l10n.mojito.quartz.QuartzPollableJob;
import org.quartz.DisallowConcurrentExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "l10n.glossary.cache.enabled", havingValue = "true")
@DisallowConcurrentExecution
public class BuildGlossaryCacheJob extends QuartzPollableJob<Void, Void> {

  static Logger logger = LoggerFactory.getLogger(BuildGlossaryCacheJob.class);

  @Autowired GlossaryCacheService cacheService;

  @Override
  public Void call(Void input) throws Exception {
    logger.debug("Triggering BuildGlossaryCacheJob");
    cacheService.buildGlossaryCache();
    logger.debug("Finished BuildGlossaryCacheJob");
    return null;
  }
}
