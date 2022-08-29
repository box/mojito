package com.box.l10n.mojito.service.repository.statistics;

import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RepositoryStatisticsJobScheduler {

  @Autowired QuartzPollableTaskScheduler quartzPollableTaskScheduler;

  public void schedule(Long repositoryId) {
    Preconditions.checkNotNull(repositoryId);

    RepositoryStatisticsJobInput repositoryStatisticsJobInput = new RepositoryStatisticsJobInput();
    repositoryStatisticsJobInput.setRepositoryId(repositoryId);

    QuartzJobInfo.Builder<RepositoryStatisticsJobInput, Void> quartzInfo =
        QuartzJobInfo.newBuilder(RepositoryStatisticsJob.class)
            .withUniqueId(String.valueOf(repositoryId))
            .withInput(repositoryStatisticsJobInput);

    quartzPollableTaskScheduler.scheduleJob(quartzInfo.build());
  }
}
