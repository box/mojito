package com.box.l10n.mojito.service.scheduledjob;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.ScheduledJob;
import com.box.l10n.mojito.entity.ScheduledJobType;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import java.util.UUID;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ScheduledJobService {

  static Logger logger = LoggerFactory.getLogger(ScheduledJobService.class);
  private final ScheduledJobRepository scheduledJobRepository;
  private final ScheduledJobStatusRepository scheduledJobStatusRepository;
  private final ScheduledJobTypeRepository scheduledJobTypeRepository;
  private final ScheduledJobManager scheduledJobManager;
  private final RepositoryRepository repositoryRepository;

  @Autowired
  public ScheduledJobService(
      ScheduledJobRepository scheduledJobRepository,
      ScheduledJobStatusRepository scheduledJobStatusRepository,
      ScheduledJobTypeRepository scheduledJobTypeRepository,
      ScheduledJobManager scheduledJobManager,
      RepositoryRepository repositoryRepository) {
    this.scheduledJobRepository = scheduledJobRepository;
    this.scheduledJobStatusRepository = scheduledJobStatusRepository;
    this.scheduledJobTypeRepository = scheduledJobTypeRepository;
    this.scheduledJobManager = scheduledJobManager;
    this.repositoryRepository = repositoryRepository;
  }

  public ScheduledJob createJob(ScheduledJobDTO scheduledJobDTO)
      throws ScheduledJobException, SchedulerException, ClassNotFoundException {
    ScheduledJob scheduledJob = getScheduledJobFromDTO(scheduledJobDTO);
    if (scheduledJob.getRepository() == null) {
      throw new ScheduledJobException("Valid repository must be provided to create a job");
    }
    if (scheduledJob.getCron() == null || scheduledJob.getCron().isEmpty()) {
      throw new ScheduledJobException("Cron expression must be provided to create a job");
    }
    if (scheduledJob.getUuid() == null) {
      scheduledJob.setUuid(UUID.randomUUID().toString());
    }
    scheduledJob.setJobStatus(
        scheduledJobStatusRepository.findByEnum(
            com.box.l10n.mojito.service.scheduledjob.ScheduledJobStatus.SCHEDULED));
    if (scheduledJob.getJobType() != null && scheduledJob.getJobType().getId() != null) {
      scheduledJob.setJobType(
          scheduledJobTypeRepository
              .findById(scheduledJob.getJobType().getId())
              .orElseThrow(
                  () ->
                      new ScheduledJobException(
                          "Job type not found with id: " + scheduledJob.getJobType().getId())));
    } else {
      throw new ScheduledJobException("Job type must be provided to create a job");
    }

    scheduledJobRepository.save(scheduledJob);
    scheduledJobManager.scheduleJob(scheduledJob);

    logger.info(
        "Job '{}' for repository '{}' was created.",
        scheduledJob.getUuid(),
        scheduledJob.getRepository().getName());
    return scheduledJob;
  }

  public ScheduledJob updateJob(String uuid, ScheduledJobDTO scheduledJobDTO)
      throws ScheduledJobException, SchedulerException, ClassNotFoundException {
    ScheduledJob updatedJob =
        scheduledJobRepository
            .findByUuid(uuid)
            .orElseThrow(() -> new ScheduledJobException("Job not found with id: " + uuid));

    ScheduledJob scheduledJob = getScheduledJobFromDTO(scheduledJobDTO);
    if (scheduledJob.getRepository() != null) {
      updatedJob.setRepository(scheduledJob.getRepository());
    }
    if (scheduledJob.getCron() != null) {
      updatedJob.setCron(scheduledJob.getCron());
    }
    if (scheduledJob.getJobStatus() != null) {
      updatedJob.setJobStatus(scheduledJob.getJobStatus());
    }
    if (scheduledJob.getJobType() != null && scheduledJob.getJobType().getId() != null) {
      updatedJob.setJobType(
          scheduledJobTypeRepository
              .findById(scheduledJob.getJobType().getId())
              .orElseThrow(
                  () ->
                      new ScheduledJobException(
                          "Job type not found with id: " + scheduledJob.getJobType().getId())));
    }
    if (scheduledJob.getPropertiesString() != null) {
      updatedJob.setPropertiesString(scheduledJob.getPropertiesString());
      updatedJob.deserializeProperties();
    }

    scheduledJobRepository.save(updatedJob);
    scheduledJobManager.scheduleJob(updatedJob);

    logger.info("Job '{}' was updated.", uuid);
    return updatedJob;
  }

  public void deleteJob(ScheduledJob scheduledJob) throws SchedulerException {
    scheduledJobRepository.deleteByUuid(scheduledJob.getUuid());
    scheduledJobManager.deleteJobFromQuartz(scheduledJob);
    logger.info("Deleted scheduled job with uuid: {}", scheduledJob.getUuid());
  }

  private Repository resolveRepositoryFromDTO(ScheduledJobDTO scheduledJobDTO) {
    if (scheduledJobDTO.getRepository() == null) {
      throw new ScheduledJobException("Repository must be provided");
    }
    Repository repository = repositoryRepository.findByName(scheduledJobDTO.getRepository());
    if (repository == null) {
      throw new ScheduledJobException("Repository not found: " + scheduledJobDTO.getRepository());
    }
    return repository;
  }

  private ScheduledJobType resolveJobTypeFromDTO(ScheduledJobDTO scheduledJobDTO) {
    if (scheduledJobDTO.getType() == null) {
      throw new ScheduledJobException("Job type must be provided");
    }
    ScheduledJobType jobType = scheduledJobTypeRepository.findByEnum(scheduledJobDTO.getType());
    if (jobType == null) {
      throw new ScheduledJobException("Job type not found: " + scheduledJobDTO.getType());
    }
    return jobType;
  }

  private ScheduledJob getScheduledJobFromDTO(ScheduledJobDTO scheduledJobDTO) {
    ScheduledJob scheduledJob = new ScheduledJob();
    scheduledJob.setUuid(scheduledJobDTO.getId());
    scheduledJob.setCron(scheduledJobDTO.getCron());
    scheduledJob.setRepository(resolveRepositoryFromDTO(scheduledJobDTO));
    scheduledJob.setJobType(resolveJobTypeFromDTO(scheduledJobDTO));
    scheduledJob.setPropertiesString(scheduledJobDTO.getPropertiesString());
    return scheduledJob;
  }
}
