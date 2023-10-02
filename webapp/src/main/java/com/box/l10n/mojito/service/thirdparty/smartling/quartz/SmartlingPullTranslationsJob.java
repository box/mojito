package com.box.l10n.mojito.service.thirdparty.smartling.quartz;

import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableJob;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import java.util.stream.LongStream;
import org.springframework.beans.factory.annotation.Autowired;

public class SmartlingPullTranslationsJob
    extends QuartzPollableJob<SmartlingPullTranslationsJobInput, Void> {

  @Autowired QuartzPollableTaskScheduler quartzPollableTaskScheduler;

  @Autowired RepositoryRepository repositoryRepository;

  @Override
  public Void call(SmartlingPullTranslationsJobInput input) throws Exception {

    LongStream.range(0, batchesFor(input.getSingularCount(), input.getBatchSize()))
        .forEach(num -> schedulePullBatchFileJob(input, num, "singular"));

    LongStream.range(0, batchesFor(input.getPluralCount(), input.getBatchSize()))
        .forEach(num -> schedulePullBatchFileJob(input, num, "plural"));

    return null;
  }

  private void schedulePullBatchFileJob(
      SmartlingPullTranslationsJobInput input, long num, String filePrefix) {
    QuartzJobInfo<SmartlingPullBatchFileJobInput, Void> quartzJobInfo =
        QuartzJobInfo.newBuilder(SmartlingPullBatchFileJob.class)
            .withInput(createPullBatchFileJobInput(num, filePrefix, input))
            .withInlineInput(false)
            .withScheduler(input.getSchedulerName())
            .withParentId(getParentId())
            .build();
    quartzPollableTaskScheduler.scheduleJob(quartzJobInfo);
  }

  protected long getParentId() {
    return getCurrentPollableTask().getId();
  }

  private SmartlingPullBatchFileJobInput createPullBatchFileJobInput(
      long batchNumber, String filePrefix, SmartlingPullTranslationsJobInput input) {
    SmartlingPullBatchFileJobInput smartlingPullBatchFileJobInput =
        new SmartlingPullBatchFileJobInput();
    smartlingPullBatchFileJobInput.setBatchNumber(batchNumber);
    smartlingPullBatchFileJobInput.setFilePrefix(filePrefix);
    smartlingPullBatchFileJobInput.setRepositoryName(input.getRepositoryName());
    smartlingPullBatchFileJobInput.setPluralSeparator(input.getPluralSeparator());
    smartlingPullBatchFileJobInput.setLocaleMapping(input.getLocaleMapping());
    smartlingPullBatchFileJobInput.setDeltaPull(input.isDeltaPull());
    smartlingPullBatchFileJobInput.setProjectId(input.getProjectId());
    smartlingPullBatchFileJobInput.setPluralFixForLocale(input.getPluralFixForLocale());
    smartlingPullBatchFileJobInput.setDryRun(input.isDryRun());
    smartlingPullBatchFileJobInput.setSchedulerName(input.getSchedulerName());

    return smartlingPullBatchFileJobInput;
  }

  private long batchesFor(long totalUnits, int batchSize) {
    return totalUnits / batchSize + ((totalUnits % batchSize == 0) ? 0 : 1);
  }
}
