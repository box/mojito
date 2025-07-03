package com.box.l10n.mojito.service.scheduledjob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.entity.ScheduledJob;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.repository.RepositoryService;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author gerryyang
 */
public class ScheduledJobServiceTest extends ServiceTestBase {
  @Autowired ScheduledJobService scheduledJobService;
  @Autowired RepositoryService repositoryService;
  @Autowired ScheduledJobRepository scheduledJobRepository;
  @Autowired RepositoryRepository repositoryRepository;

  @Before
  public void setup() throws RepositoryNameAlreadyUsedException {
    if (repositoryRepository.findByName("Demo") == null) {
      repositoryService.createRepository("Demo");
    }
    if (repositoryRepository.findByName("Demo1") == null) {
      repositoryService.createRepository("Demo1");
    }
    scheduledJobRepository.deleteAll();
  }

  @Test
  public void testCreateScheduledJobSuccess() throws SchedulerException, ClassNotFoundException {
    ScheduledJobDTO scheduledJobDTO = new ScheduledJobDTO();
    scheduledJobDTO.setRepository("Demo");
    scheduledJobDTO.setCron("0 0/1 * * * ?");
    scheduledJobDTO.setType(ScheduledJobType.THIRD_PARTY_SYNC);
    scheduledJobDTO.setPropertiesString("{\"version\": 1}");

    ScheduledJob scheduledJob = scheduledJobService.createJob(scheduledJobDTO);
    Optional<ScheduledJob> createdJob = scheduledJobRepository.findByUuid(scheduledJob.getUuid());
    assertTrue(createdJob.isPresent());
    assertEquals(ScheduledJob.class, createdJob.get().getClass());
  }

  @Test
  public void testCreateScheduledJobFailure() {
    ScheduledJobDTO scheduledJobDTO = new ScheduledJobDTO();
    scheduledJobDTO.setRepository("Invalid Repository");
    scheduledJobDTO.setCron("Invalid Cron Expression");
    scheduledJobDTO.setType(ScheduledJobType.THIRD_PARTY_SYNC);
    scheduledJobDTO.setPropertiesString("Invalid Properties String");

    assertThrows(ScheduledJobException.class, () -> scheduledJobService.createJob(scheduledJobDTO));
  }

  @Test
  public void testCreateScheduledJobIfJobForRepositoryAlreadyExists()
      throws SchedulerException, ClassNotFoundException {
    ScheduledJobDTO scheduledJobDTO = new ScheduledJobDTO();
    scheduledJobDTO.setRepository("Demo");
    scheduledJobDTO.setCron("0 0/1 * * * ?");
    scheduledJobDTO.setType(ScheduledJobType.THIRD_PARTY_SYNC);
    scheduledJobDTO.setPropertiesString("{\"version\": 1}");

    ScheduledJob createdJob = scheduledJobService.createJob(scheduledJobDTO);

    // Throws an exception because Third Party Sync job for the repository already exists
    assertThrows(ScheduledJobException.class, () -> scheduledJobService.createJob(scheduledJobDTO));

    scheduledJobService.deleteJob(createdJob);

    // After soft deleting the job, we can create a job for that repository again
    ScheduledJob scheduledJob = scheduledJobService.createJob(scheduledJobDTO);
    Optional<ScheduledJob> recreatedJob = scheduledJobRepository.findByUuid(scheduledJob.getUuid());
    assertTrue(recreatedJob.isPresent());
    assertEquals(ScheduledJob.class, recreatedJob.get().getClass());

    scheduledJobDTO.setType(ScheduledJobType.EVOLVE_SYNC);

    // This should succeed because each repository can have an Evolve job and Third Party job
    ScheduledJob scheduledJobEvolve = scheduledJobService.createJob(scheduledJobDTO);
    Optional<ScheduledJob> createdJobEvolve =
        scheduledJobRepository.findByUuid(scheduledJobEvolve.getUuid());
    assertTrue(createdJobEvolve.isPresent());
    assertEquals(ScheduledJob.class, createdJobEvolve.get().getClass());
  }

  @Test
  public void testUpdateScheduledJobSuccess() throws SchedulerException, ClassNotFoundException {
    ScheduledJobDTO scheduledJobDTO = new ScheduledJobDTO();
    scheduledJobDTO.setRepository("Demo");
    scheduledJobDTO.setCron("0 0/1 * * * ?");
    scheduledJobDTO.setType(ScheduledJobType.THIRD_PARTY_SYNC);
    scheduledJobDTO.setPropertiesString("{\"version\": 1}");
    ScheduledJob createdJob = scheduledJobService.createJob(scheduledJobDTO);

    ScheduledJobDTO updatedJobDTO = new ScheduledJobDTO();
    updatedJobDTO.setRepository("Demo1");
    updatedJobDTO.setCron("0 0/2 * * * ?");
    scheduledJobService.updateJob(createdJob.getUuid(), updatedJobDTO);

    ScheduledJob updatedJob =
        scheduledJobRepository
            .findByUuid(createdJob.getUuid())
            .orElseThrow(
                () -> new ScheduledJobException("No job found with UUID: " + createdJob.getUuid()));

    assertEquals("Demo1", updatedJob.getRepository().getName());
    assertEquals("0 0/2 * * * ?", updatedJob.getCron());
  }

  @Test
  public void testUpdateScheduledJobFailure() throws SchedulerException, ClassNotFoundException {
    ScheduledJobDTO scheduledJobDTO = new ScheduledJobDTO();
    scheduledJobDTO.setRepository("Demo");
    scheduledJobDTO.setCron("0 0/1 * * * ?");
    scheduledJobDTO.setType(ScheduledJobType.THIRD_PARTY_SYNC);
    scheduledJobDTO.setPropertiesString("{\"version\": 1}");
    ScheduledJob createdJob = scheduledJobService.createJob(scheduledJobDTO);

    ScheduledJobDTO updatedJobDTO = new ScheduledJobDTO();
    updatedJobDTO.setRepository("Invalid Repository");
    updatedJobDTO.setCron("Invalid Cron Expression");
    updatedJobDTO.setPropertiesString("Invalid Properties String");

    assertThrows(
        ScheduledJobException.class,
        () -> scheduledJobService.updateJob(createdJob.getUuid(), updatedJobDTO));
  }

  @Test
  public void testUpdateJobRepository() throws SchedulerException, ClassNotFoundException {
    ScheduledJobDTO scheduledJobDTODemo = new ScheduledJobDTO();
    scheduledJobDTODemo.setRepository("Demo");
    scheduledJobDTODemo.setCron("0 0/1 * * * ?");
    scheduledJobDTODemo.setType(ScheduledJobType.THIRD_PARTY_SYNC);
    scheduledJobDTODemo.setPropertiesString("{\"version\": 1}");
    ScheduledJob createdJobDemo = scheduledJobService.createJob(scheduledJobDTODemo);

    ScheduledJobDTO scheduledJobDTODemo1 = new ScheduledJobDTO();
    scheduledJobDTODemo1.setRepository("Demo1");
    scheduledJobDTODemo1.setCron("0 0/1 * * * ?");
    scheduledJobDTODemo1.setType(ScheduledJobType.THIRD_PARTY_SYNC);
    scheduledJobDTODemo1.setPropertiesString("{\"version\": 1}");
    ScheduledJob createdJobDemo1 = scheduledJobService.createJob(scheduledJobDTODemo1);

    ScheduledJobDTO updatedJobDTO = new ScheduledJobDTO();
    updatedJobDTO.setRepository("Demo1");

    // Throws exception because updating job repository, but a job already exists for that
    // repository
    assertThrows(
        ScheduledJobException.class,
        () -> scheduledJobService.updateJob(createdJobDemo.getUuid(), updatedJobDTO));

    updatedJobDTO.setRepository("Demo");
    assertThrows(
        ScheduledJobException.class,
        () -> scheduledJobService.updateJob(createdJobDemo1.getUuid(), updatedJobDTO));
  }

  @Test
  public void testDeleteRestoreScheduledJob() throws SchedulerException, ClassNotFoundException {
    ScheduledJobDTO scheduledJobDTO = new ScheduledJobDTO();
    scheduledJobDTO.setRepository("Demo");
    scheduledJobDTO.setCron("0 0/1 * * * ?");
    scheduledJobDTO.setType(ScheduledJobType.THIRD_PARTY_SYNC);
    scheduledJobDTO.setPropertiesString("{\"version\": 1}");
    ScheduledJob createdJob = scheduledJobService.createJob(scheduledJobDTO);

    scheduledJobService.deleteJob(createdJob);
    Optional<ScheduledJob> deletedJob = scheduledJobRepository.findByUuid(createdJob.getUuid());
    assertTrue(deletedJob.isPresent());
    assertTrue(deletedJob.get().isDeleted());

    scheduledJobService.restoreJob(deletedJob.get());
    Optional<ScheduledJob> restoredJob = scheduledJobRepository.findByUuid(createdJob.getUuid());
    assertTrue(restoredJob.isPresent());
    assertFalse(restoredJob.get().isDeleted());
  }

  @Test
  public void testRestoreJobIfJobForRepositoryAlreadyExists()
      throws SchedulerException, ClassNotFoundException {
    ScheduledJobDTO scheduledJobDTO = new ScheduledJobDTO();
    scheduledJobDTO.setRepository("Demo");
    scheduledJobDTO.setCron("0 0/1 * * * ?");
    scheduledJobDTO.setType(ScheduledJobType.THIRD_PARTY_SYNC);
    scheduledJobDTO.setPropertiesString("{\"version\": 1}");
    ScheduledJob createdJob = scheduledJobService.createJob(scheduledJobDTO);

    scheduledJobService.deleteJob(createdJob);
    scheduledJobService.createJob(scheduledJobDTO);

    // Throws an exception because restoring a job for a repository that already has a job
    assertThrows(ScheduledJobException.class, () -> scheduledJobService.restoreJob(createdJob));
  }
}
