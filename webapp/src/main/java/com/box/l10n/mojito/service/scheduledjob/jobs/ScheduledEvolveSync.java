package com.box.l10n.mojito.service.scheduledjob.jobs;

import com.box.l10n.mojito.entity.ScheduledJob;
import com.box.l10n.mojito.pagerduty.PagerDutyException;
import com.box.l10n.mojito.pagerduty.PagerDutyIntegrationService;
import com.box.l10n.mojito.pagerduty.PagerDutyPayload;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.evolve.EvolveSyncJob;
import com.box.l10n.mojito.service.evolve.EvolveSyncJobInput;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.scheduledjob.IScheduledJob;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobRepository;
import com.box.l10n.mojito.utils.ServerConfig;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.text.StrSubstitutor;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@DisallowConcurrentExecution
public class ScheduledEvolveSync implements IScheduledJob {
  static Logger logger = LoggerFactory.getLogger(ScheduledThirdPartySync.class);

  private static final long TEN_SECONDS = 10000;

  @Autowired private PollableTaskService pollableTaskService;

  @Autowired private QuartzPollableTaskScheduler quartzPollableTaskScheduler;

  @Autowired private ScheduledJobRepository scheduledJobRepository;

  @Autowired private PagerDutyIntegrationService pagerDutyIntegrationService;

  @Autowired private ServerConfig serverConfig;

  private ScheduledJob scheduledJob;

  private Long pollableTaskId;

  @Value(
      "${l10n.scheduledJobs.thirdPartySync.evolve.notifications.title:MOJITO | Evolve sync failed}")
  private String notificationTitle;

  @Value("${l10n.scheduledJobs.thirdPartySync.timeout:3600}")
  private Long timeout = 3600L;

  @Override
  public void onSuccess(JobExecutionContext context) {
    pagerDutyIntegrationService
        .getDefaultPagerDutyClient()
        .ifPresent(
            pd -> {
              try {
                pd.resolveIncident(scheduledJob.getUuid());
              } catch (PagerDutyException e) {
                logger.error(
                    "Couldn't send resolve PagerDuty notification for successful Evolve sync", e);
              }
            });
  }

  @Override
  public void onFailure(JobExecutionContext context, JobExecutionException jobException) {
    pagerDutyIntegrationService
        .getDefaultPagerDutyClient()
        .ifPresent(
            pd -> {
              String pollableTaskUrl =
                  UriComponentsBuilder.fromHttpUrl(serverConfig.getUrl())
                      .path("api/pollableTasks/" + pollableTaskId.toString())
                      .build()
                      .toUriString();

              String scheduledJobUrl =
                  UriComponentsBuilder.fromHttpUrl(serverConfig.getUrl())
                      .path("api/jobs/" + scheduledJob.getUuid())
                      .build()
                      .toUriString();

              String title =
                  StrSubstitutor.replace(
                      this.notificationTitle,
                      ImmutableMap.of("repository", scheduledJob.getRepository().getName()),
                      "{",
                      "}");

              PagerDutyPayload payload =
                  new PagerDutyPayload(
                      title,
                      serverConfig.getUrl(),
                      PagerDutyPayload.Severity.CRITICAL,
                      ImmutableMap.of(
                          "Pollable Task", pollableTaskUrl, "Scheduled Job", scheduledJobUrl));

              try {
                pd.triggerIncident(scheduledJob.getUuid(), payload);
              } catch (PagerDutyException e) {
                logger.error(
                    "Couldn't send PagerDuty notification for failed Evolve sync, Pollable Task URL: '{}', Scheduled Job: '{}'",
                    pollableTaskUrl,
                    scheduledJobUrl,
                    e);
              }
            });
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    scheduledJob = scheduledJobRepository.findByJobKey(context.getJobDetail().getKey());
    ScheduledThirdPartySyncProperties scheduledJobProperties =
        (ScheduledThirdPartySyncProperties) scheduledJob.getProperties();
    logger.info("Evolve sync has started.");
    EvolveSyncJobInput input =
        new EvolveSyncJobInput(
            scheduledJob.getRepository().getId(), scheduledJobProperties.getLocaleMapping());
    try {
      PollableFuture<Void> task =
          quartzPollableTaskScheduler.scheduleJobWithCustomTimeout(
              EvolveSyncJob.class, input, "evolveSync", this.timeout);
      pollableTaskId = task.getPollableTask().getId();
      pollableTaskService.waitForPollableTask(pollableTaskId, this.timeout * 1000, TEN_SECONDS);
    } catch (Exception e) {
      throw new JobExecutionException(e);
    }
  }
}
