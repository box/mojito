package com.box.l10n.mojito.rest.machinetranslation;

import static com.box.l10n.mojito.CacheType.Names.MACHINE_TRANSLATION;

import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.machinetranslation.BatchMachineTranslationJob;
import com.box.l10n.mojito.service.machinetranslation.MachineTranslationService;
import com.box.l10n.mojito.service.machinetranslation.RepositoryMachineTranslation;
import com.box.l10n.mojito.service.machinetranslation.TranslationDTO;
import com.box.l10n.mojito.service.machinetranslation.TranslationsResponseDTO;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webservice for machine translating strings, with optional source leveraging.
 *
 * @author garion
 */
@RestController
public class MachineTranslationWS {

  static Logger logger = LoggerFactory.getLogger(MachineTranslationWS.class);

  public static final String DEFAULT_LOCALE = "en";

  @Autowired MachineTranslationService machineTranslationService;

  @Autowired QuartzPollableTaskScheduler quartzPollableTaskScheduler;

  @RequestMapping(method = RequestMethod.POST, value = "/api/machine-translation-batch")
  @ResponseStatus(HttpStatus.OK)
  @Cacheable(MACHINE_TRANSLATION)
  public PollableTask getTranslations(@RequestBody BatchTranslationRequestDTO translationRequest) {
    QuartzJobInfo<BatchTranslationRequestDTO, TranslationsResponseDTO> quartzJobInfo =
        QuartzJobInfo.newBuilder(BatchMachineTranslationJob.class)
            .withInlineInput(false)
            .withInput(translationRequest)
            .build();
    PollableFuture<TranslationsResponseDTO> localizedAssetBodyPollableFuture =
        quartzPollableTaskScheduler.scheduleJob(quartzJobInfo);
    return localizedAssetBodyPollableFuture.getPollableTask();
  }

  @RequestMapping(method = RequestMethod.POST, value = "/api/machine-translation")
  @ResponseStatus(HttpStatus.OK)
  @Cacheable(MACHINE_TRANSLATION)
  public TranslationDTO getSingleTranslation(
      @RequestBody TranslationRequestDTO translationRequest) {
    return machineTranslationService.getSingleTranslation(
        translationRequest.getTextSource(),
        translationRequest.getSourceBcp47Tag(),
        translationRequest.getTargetBcp47Tag(),
        translationRequest.isSkipFunctionalProtection(),
        translationRequest.isSkipLeveraging(),
        translationRequest.getRepositoryIds(),
        translationRequest.getRepositoryNames());
  }

  @RequestMapping(method = RequestMethod.GET, value = "/api/machine-translation/config")
  @ResponseStatus(HttpStatus.OK)
  public String getMachineTranslationConfiguration() {
    return machineTranslationService.getConfiguredEngineSource().toString();
  }

  @Autowired RepositoryMachineTranslation repositoryMachineTranslation;

  // TODO(jean) make it a real WS + CLI ...
  @RequestMapping(method = RequestMethod.GET, value = "/api/machine-translation/myrepository")
  @ResponseStatus(HttpStatus.OK)
  public void oneOffMachineTranslation(
      @RequestParam(value = "repositoryName") String repositoryName,
      @RequestParam(value = "targetLocale") String targetLocale) {
    repositoryMachineTranslation.translateRepository(repositoryName, targetLocale);
  }
}
