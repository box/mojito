package com.box.l10n.mojito.rest.scheduledjob;

import com.box.l10n.mojito.entity.ScheduledJob;
import com.box.l10n.mojito.security.Role;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobDTO;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobException;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobManager;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobRepository;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobResponse;
import com.box.l10n.mojito.service.scheduledjob.ScheduledJobService;
import com.box.l10n.mojito.service.security.user.UserService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Webservice for viewing, enabling/disabling and triggering scheduled jobs.
 *
 * @author mattwilshire
 */
@RestController
public class ScheduledJobWS {
  static Logger logger = LoggerFactory.getLogger(ScheduledJobWS.class);
  private final ScheduledJobRepository scheduledJobRepository;
  private final ScheduledJobManager scheduledJobManager;
  private final ScheduledJobService scheduledJobService;
  private final UserService userService;

  @Autowired
  public ScheduledJobWS(
      ScheduledJobRepository scheduledJobRepository,
      ScheduledJobManager scheduledJobManager,
      ScheduledJobService scheduledJobService,
      UserService userService) {
    this.scheduledJobRepository = scheduledJobRepository;
    this.scheduledJobManager = scheduledJobManager;
    this.scheduledJobService = scheduledJobService;
    this.userService = userService;
  }

  private final ResponseEntity<ScheduledJobResponse> notFoundResponse =
      createResponse(
          HttpStatus.NOT_FOUND, ScheduledJobResponse.Status.FAILURE, "Job doesn't exist", null);

  @RequestMapping(method = RequestMethod.POST, value = "/api/jobs")
  @ResponseStatus(HttpStatus.OK)
  public ScheduledJobDTO createJob(@RequestBody ScheduledJobDTO scheduledJobDTO)
      throws SchedulerException, ClassNotFoundException, ResponseStatusException {
    try {
      return new ScheduledJobDTO(scheduledJobService.createJob(scheduledJobDTO));
    } catch (ScheduledJobException e) {
      logger.error("Error creating job", e);
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Error creating job: " + e.getMessage(), e);
    }
  }

  @RequestMapping(method = RequestMethod.PATCH, value = "/api/jobs/{id}")
  @ResponseStatus(HttpStatus.OK)
  public ScheduledJobDTO updateJob(
      @PathVariable UUID id, @RequestBody ScheduledJobDTO scheduledJobDTO)
      throws SchedulerException, ClassNotFoundException, ResponseStatusException {
    try {
      return new ScheduledJobDTO(scheduledJobService.updateJob(id.toString(), scheduledJobDTO));
    } catch (ScheduledJobException e) {
      logger.error("Error updating job", e);
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Error updating job: " + e.getMessage(), e);
    }
  }

  @RequestMapping(method = RequestMethod.DELETE, value = "/api/jobs/{id}")
  @ResponseStatus(HttpStatus.OK)
  public void deleteJob(@PathVariable UUID id) throws SchedulerException {
    logger.info("Deleting scheduled job [{}]", id);
    ScheduledJob scheduledJob =
        scheduledJobRepository
            .findByUuid(id.toString())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Job not found with id: " + id));

    scheduledJobService.deleteJob(scheduledJob);
  }

  @RequestMapping(method = RequestMethod.PATCH, value = "/api/jobs/{id}/restore")
  @ResponseStatus(HttpStatus.OK)
  public void restoreJob(@PathVariable UUID id) throws SchedulerException, ClassNotFoundException {
    logger.info("Restoring scheduled job [{}]", id);
    ScheduledJob scheduledJob =
        scheduledJobRepository
            .findByUuid(id.toString())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Job not found with id: " + id));

    scheduledJobService.restoreJob(scheduledJob);
  }

  @RequestMapping(method = RequestMethod.GET, value = "/api/jobs")
  @ResponseStatus(HttpStatus.OK)
  public List<ScheduledJobDTO> getAllJobs() {
    List<ScheduledJob> scheduledJobs = scheduledJobRepository.findAll();
    if (userService.checkUserHasRole(Role.ROLE_ADMIN)) {
      return scheduledJobs.stream().map(ScheduledJobDTO::new).collect(Collectors.toList());
    } else {
      // Filter out jobs that are deleted for non-admin users
      List<ScheduledJob> activeJobs =
          scheduledJobs.stream().filter(scheduledJob -> !scheduledJob.isDeleted()).toList();
      return activeJobs.stream().map(ScheduledJobDTO::new).collect(Collectors.toList());
    }
  }

  @RequestMapping(method = RequestMethod.GET, value = "/api/jobs/{id}")
  @ResponseStatus(HttpStatus.OK)
  public ScheduledJobDTO getJob(@PathVariable UUID id) {
    return scheduledJobRepository
        .findByUuid(id.toString())
        .map(ScheduledJobDTO::new)
        .orElseThrow(
            () ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found with id: " + id));
  }

  @RequestMapping(method = RequestMethod.POST, value = "/api/jobs/{id}/trigger")
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<ScheduledJobResponse> triggerJob(@PathVariable UUID id) {

    Optional<ScheduledJob> optScheduledJob = scheduledJobRepository.findByUuid(id.toString());

    if (optScheduledJob.isEmpty()) return notFoundResponse;

    ScheduledJob scheduledJob = optScheduledJob.get();
    JobKey jobKey = scheduledJobManager.getJobKey(scheduledJob);

    if (!scheduledJob.isEnabled())
      return createResponse(
          HttpStatus.CONFLICT,
          ScheduledJobResponse.Status.FAILURE,
          "Job is disabled, trigger request ignored",
          scheduledJob.getUuid());

    try {
      if (!scheduledJobManager.getScheduler().checkExists(jobKey)) return notFoundResponse;

      // Ignore the trigger request if job is currently running
      for (JobExecutionContext jobExecutionContext :
          scheduledJobManager.getScheduler().getCurrentlyExecutingJobs()) {
        if (jobExecutionContext.getJobDetail().getKey().equals(jobKey)) {

          // If there are no pending manual triggers, queue one up otherwise do nothing.
          if (!isPendingTrigger(jobKey)) {
            scheduledJobManager.getScheduler().triggerJob(jobKey);
          }

          return createResponse(
              HttpStatus.CONFLICT,
              ScheduledJobResponse.Status.FAILURE,
              "Job is currently running, trigger request has been queued and will fire when the current job is finished running.",
              scheduledJob.getUuid());
        }
      }

      scheduledJobManager.getScheduler().triggerJob(jobKey);
      logger.info(
          "Job '{}' for repository '{}' was manually triggered.",
          scheduledJob.getJobType().getEnum(),
          scheduledJob.getRepository().getName());
      return ResponseEntity.status(HttpStatus.OK)
          .body(
              new ScheduledJobResponse(
                  ScheduledJobResponse.Status.SUCCESS, "Job triggered", scheduledJob.getUuid()));
    } catch (SchedulerException e) {
      logger.error(
          "Error triggering job manually, job: {}", jobKey.getName() + ":" + jobKey.getGroup(), e);
      return createResponse(
          HttpStatus.INTERNAL_SERVER_ERROR,
          ScheduledJobResponse.Status.FAILURE,
          e.getMessage(),
          scheduledJob.getUuid());
    }
  }

  @RequestMapping(method = RequestMethod.POST, value = "/api/jobs/{id}/enable")
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<ScheduledJobResponse> enableJob(@PathVariable UUID id) {
    return setJobActive(id, true);
  }

  @RequestMapping(method = RequestMethod.POST, value = "/api/jobs/{id}/disable")
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<ScheduledJobResponse> disableJob(@PathVariable UUID id) {
    return setJobActive(id, false);
  }

  private ResponseEntity<ScheduledJobResponse> setJobActive(UUID id, boolean active) {
    Optional<ScheduledJob> optScheduledJob = scheduledJobRepository.findByUuid(id.toString());

    if (optScheduledJob.isEmpty()) return notFoundResponse;

    ScheduledJob scheduledJob = optScheduledJob.get();
    JobKey jobKey = scheduledJobManager.getJobKey(scheduledJob);

    try {
      if (!scheduledJobManager.getScheduler().checkExists(jobKey)) return notFoundResponse;

      if (scheduledJob.isEnabled() == active)
        return createResponse(
            HttpStatus.ALREADY_REPORTED,
            ScheduledJobResponse.Status.SUCCESS,
            "The job is already " + (active ? "enabled" : "disabled"),
            scheduledJob.getUuid());

      scheduledJob.setEnabled(active);
      scheduledJobRepository.save(scheduledJob);

      return createResponse(
          HttpStatus.OK,
          ScheduledJobResponse.Status.SUCCESS,
          "Job is now " + (active ? "enabled" : "disabled"),
          scheduledJob.getUuid());
    } catch (SchedulerException e) {

      logger.error(
          "SchedulerException thrown from trying to change job '{}' : '{}' to active: {}",
          scheduledJob.getJobType().getEnum(),
          scheduledJob.getRepository().getName(),
          active,
          e);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Job with id: " + id.toString() + " could not be disabled");
    }
  }

  private ResponseEntity<ScheduledJobResponse> createResponse(
      HttpStatus status,
      ScheduledJobResponse.Status jobResponseStatus,
      String message,
      String jobId) {
    return ResponseEntity.status(status)
        .body(new ScheduledJobResponse(jobResponseStatus, message, jobId));
  }

  private boolean isPendingTrigger(JobKey jobKey) throws SchedulerException {
    return scheduledJobManager.getScheduler().getTriggersOfJob(jobKey).stream()
        .anyMatch(
            trigger -> {
              try {
                return trigger.getKey().getName().startsWith("MT_")
                    && scheduledJobManager.getScheduler().getTriggerState(trigger.getKey())
                        != Trigger.TriggerState.COMPLETE;
              } catch (SchedulerException e) {
                return false;
              }
            });
  }
}
