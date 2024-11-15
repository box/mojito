package com.box.l10n.mojito.service.scheduledjob;

import com.box.l10n.mojito.entity.ScheduledJob;
import java.util.Optional;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.listeners.TriggerListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener that receives events about jobs that are about to be triggered under the 'scheduledJobs'
 * scheduler, if the job to be triggered is disabled the job is vetoed out of execution. Pausing the
 * jobs execution exists in Quartz but upon using it, the job loses track of its schedule when
 * re-enabled, or it doesn't schedule at all, using the trigger listener solves this.
 *
 * @author mattwilshire
 */
public class ScheduledJobTriggerListener extends TriggerListenerSupport {

  static Logger logger = LoggerFactory.getLogger(ScheduledJobTriggerListener.class);

  private final ScheduledJobRepository scheduledJobRepository;

  ScheduledJobTriggerListener(ScheduledJobRepository scheduledJobRepository) {
    this.scheduledJobRepository = scheduledJobRepository;
  }

  @Override
  public String getName() {
    return "ScheduledJobTriggerListener";
  }

  @Override
  public void triggerMisfired(Trigger trigger) {
    super.triggerMisfired(trigger);
    Optional<ScheduledJob> job = scheduledJobRepository.findByUuid(trigger.getJobKey().getName());
    if (job.isEmpty()) return;
    logger.warn(
        "TRIGGER MISFIRE FOR {} | {}", job.get().getRepository().getName(), job.get().getJobType());
  }

  @Override
  public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
    // If the job is disabled, don't execute
    Optional<ScheduledJob> job = scheduledJobRepository.findByUuid(trigger.getJobKey().getName());
    return job.filter(scheduledJob -> !scheduledJob.isEnabled()).isPresent();
  }
}
