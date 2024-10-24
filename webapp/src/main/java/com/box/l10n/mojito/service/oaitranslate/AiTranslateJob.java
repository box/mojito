package com.box.l10n.mojito.service.oaitranslate;

import static com.box.l10n.mojito.service.oaitranslate.AiTranslateJob.*;

import com.box.l10n.mojito.quartz.QuartzPollableJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Class to process a batch of strings for machine translation against a set of target languages.
 *
 * @author garion
 */
@Component
public class AiTranslateJob extends QuartzPollableJob<AiTranslateJobInput, Void> {

  static Logger logger = LoggerFactory.getLogger(AiTranslateJob.class);

  @Autowired AiTranslateService aiTranslateService;

  @Override
  public Void call(AiTranslateJobInput aiTranslateJobInput) throws Exception {
    logger.debug(
        "Start AiTranslateJob with repository id:  {}", aiTranslateJobInput.repositoryId());
    aiTranslateService.aiTranslate(aiTranslateJobInput.repositoryId());
    return null;
  }

  public record AiTranslateJobInput(long repositoryId) {}
}
