package com.box.l10n.mojito.service.thirdparty.smartling.quartz;

import static com.box.l10n.mojito.service.thirdparty.smartling.SmartlingFileUtils.getOutputSourceFile;

import com.box.l10n.mojito.LocaleMappingHelper;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableJob;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.repository.RepositoryService;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class SmartlingPullBatchFileJob
    extends QuartzPollableJob<SmartlingPullBatchFileJobInput, Void> {

  static Logger logger = LoggerFactory.getLogger(SmartlingPullBatchFileJob.class);

  @Autowired QuartzPollableTaskScheduler quartzPollableTaskScheduler;

  @Autowired MeterRegistry meterRegistry;

  @Autowired RepositoryRepository repositoryRepository;

  @Autowired LocaleMappingHelper localeMappingHelper;

  @Autowired RepositoryService repositoryService;

  @Override
  public Void call(SmartlingPullBatchFileJobInput input) throws Exception {

    Repository repository = repositoryRepository.findByName(input.getRepositoryName());

    for (RepositoryLocale locale :
        repositoryService.getRepositoryLocalesWithoutRootLocale(repository)) {
      logger.debug("Pulling locale file for locale: {}", locale.getLocale().getBcp47Tag());
      scheduleLocaleFileJob(input, repository, locale);
    }

    return null;
  }

  private void scheduleLocaleFileJob(
      SmartlingPullBatchFileJobInput input, Repository repository, RepositoryLocale locale) {
    SmartlingPullLocaleFileJobInput smartlingPullLocaleFileJobInput =
        new SmartlingPullLocaleFileJobInput();
    String fileName =
        getOutputSourceFile(input.getBatchNumber(), repository.getName(), input.getFilePrefix());
    smartlingPullLocaleFileJobInput.setRepositoryName(input.getRepositoryName());
    smartlingPullLocaleFileJobInput.setLocaleId(locale.getId());
    smartlingPullLocaleFileJobInput.setSmartlingFilePrefix(input.getFilePrefix());
    smartlingPullLocaleFileJobInput.setFileName(fileName);
    smartlingPullLocaleFileJobInput.setPluralSeparator(input.getPluralSeparator());
    smartlingPullLocaleFileJobInput.setSmartlingLocale(
        parseLocaleMapping(input.getLocaleMapping())
            .getOrDefault(locale.getLocale().getBcp47Tag(), locale.getLocale().getBcp47Tag()));
    smartlingPullLocaleFileJobInput.setDeltaPull(input.isDeltaPull());
    smartlingPullLocaleFileJobInput.setPluralFixForLocale(
        Optional.ofNullable(input.getPluralFixForLocale())
            .orElse("")
            .contains(locale.getLocale().getBcp47Tag()));
    smartlingPullLocaleFileJobInput.setDryRun(input.isDryRun());
    smartlingPullLocaleFileJobInput.setSmartlingProjectId(input.getProjectId());
    smartlingPullLocaleFileJobInput.setLocaleBcp47Tag(locale.getLocale().getBcp47Tag());
    QuartzJobInfo<SmartlingPullLocaleFileJobInput, Void> quartzJobInfo =
        QuartzJobInfo.newBuilder(SmartlingPullLocaleFileJob.class)
            .withInput(smartlingPullLocaleFileJobInput)
            .withInlineInput(false)
            .withScheduler(input.getSchedulerName())
            .withParentId(getParentId())
            .withMessage(
                String.format(
                    "Pulling file '%s' for locale %s", fileName, locale.getLocale().getBcp47Tag()))
            .build();

    quartzPollableTaskScheduler.scheduleJob(quartzJobInfo);
  }

  private Map<String, String> parseLocaleMapping(String input) {
    return Optional.ofNullable(localeMappingHelper.getLocaleMapping(input))
        .orElseGet(Collections::emptyMap);
  }

  protected long getParentId() {
    return getCurrentPollableTask().getId();
  }
}
