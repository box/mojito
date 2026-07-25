package com.box.l10n.mojito.service.oaitranslate;

import com.box.l10n.mojito.quartz.QuartzPollableJob;
import com.box.l10n.mojito.service.oaitranslate.AiTranslateService.AiTranslateInput;
import com.box.l10n.mojito.service.oaitranslate.AiTranslateService.AiTranslateOutput;
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
public class AiTranslateJob extends QuartzPollableJob<AiTranslateInput, AiTranslateOutput> {

  static Logger logger = LoggerFactory.getLogger(AiTranslateJob.class);

  @Autowired AiTranslateService aiTranslateService;

  @Override
  public AiTranslateOutput call(AiTranslateInput aiTranslateJobInput) throws Exception {
    return aiTranslateService.aiTranslate(aiTranslateJobInput, getCurrentPollableTask());
  }
}
