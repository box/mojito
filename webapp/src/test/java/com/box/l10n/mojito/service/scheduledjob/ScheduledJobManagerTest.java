package com.box.l10n.mojito.service.scheduledjob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

import com.box.l10n.mojito.entity.ScheduledJob;
import com.box.l10n.mojito.quartz.QuartzSchedulerManager;
import com.box.l10n.mojito.retry.DeadLockLoserExceptionRetryTemplate;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.repository.RepositoryLocaleCreationException;
import com.box.l10n.mojito.service.repository.RepositoryNameAlreadyUsedException;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.thirdparty.ThirdPartySyncJobConfig;
import com.box.l10n.mojito.service.thirdparty.ThirdPartySyncJobsConfig;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.google.common.collect.ImmutableMap;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests for the ScheduledJobManager. SpyBean is not used in these tests to ensure a new context is
 * not created for the test. If a new context is created another test will begin to hang as it tries
 * to use this context for its own tests. As an alternative to spy, static variables are set on the
 * No-op scheduled job to determine if the methods success and failure methods are executed by the
 * listener.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ScheduledJobManagerTest extends ServiceTestBase {

  private ScheduledJobManager scheduledJobManager;
  @Autowired RepositoryService repositoryService;
  @Autowired QuartzSchedulerManager schedulerManager;
  @Autowired ScheduledJobRepository scheduledJobRepository;
  @Autowired ScheduledJobStatusRepository scheduledJobStatusRepository;
  @Autowired ScheduledJobTypeRepository scheduledJobTypeRepository;
  @Autowired RepositoryRepository repositoryRepository;
  @Autowired DeadLockLoserExceptionRetryTemplate deadlockRetryTemplate;
  @Rule public TestIdWatcher testIdWatcher = new TestIdWatcher();

  private static final int MAX_RETRIES = 10;
  private static final int RETRY_DELAY_MS = 100;
  private static final int JOB_WAIT_TIME_MS = 5000;

  private final String TEST_UUID = "e4c72563-d8f0-4465-9eec-aaa96087665e";

  @Before
  public void initialize()
      throws RepositoryNameAlreadyUsedException,
          RepositoryLocaleCreationException,
          ClassNotFoundException,
          SchedulerException {

    for (int i = 1; i <= 4; i++) {
      repositoryService.addRepositoryLocale(
          repositoryService.createRepository(testIdWatcher.getEntityName("scheduled-job-" + i)),
          "fr-FR");
    }

    ThirdPartySyncJobConfig scheduledJobOne =
        createThirdPartySyncJobConfig(TEST_UUID, testIdWatcher.getEntityName("scheduled-job-1"));

    ThirdPartySyncJobConfig scheduledJobTwo =
        createThirdPartySyncJobConfig(
            "7d39ee64-415e-42bb-8492-33f1909484c9", testIdWatcher.getEntityName("scheduled-job-2"));

    // No cron schedule, shouldn't schedule
    ThirdPartySyncJobConfig scheduledJobThree =
        createThirdPartySyncJobConfig(
            "f1d1a12d-b23e-4ef8-9391-f647be6f9db4", testIdWatcher.getEntityName("scheduled-job-3"));
    scheduledJobThree.setCron(null);

    // No uuid, shouldn't schedule
    ThirdPartySyncJobConfig scheduledJobFour =
        createThirdPartySyncJobConfig(null, testIdWatcher.getEntityName("scheduled-job-4"));

    ThirdPartySyncJobsConfig thirdPartySyncJobsConfig = new ThirdPartySyncJobsConfig();
    thirdPartySyncJobsConfig.setThirdPartySyncJobs(
        ImmutableMap.of(
            "scheduled-job-1",
            scheduledJobOne,
            "scheduled-job-2",
            scheduledJobTwo,
            "scheduled-job-3",
            scheduledJobThree,
            "scheduled-job-4",
            scheduledJobFour));

    // Return a Spy with the Job always being a no-op job
    scheduledJobManager =
        Mockito.spy(
            new ScheduledJobManager(
                thirdPartySyncJobsConfig,
                schedulerManager,
                scheduledJobRepository,
                scheduledJobStatusRepository,
                scheduledJobTypeRepository,
                repositoryRepository,
                deadlockRetryTemplate));

    Mockito.doReturn(NoOpScheduledJobTest.class).when(scheduledJobManager).loadJobClass(any());

    scheduledJobManager.init();
  }

  @Test
  public void testQuartzScheduledJob() throws Exception {
    assertEquals(2, scheduledJobManager.getAllJobKeys().size());
    assertEquals(2, scheduledJobRepository.findAll().size());
  }

  @Test
  public void testJobSucceeds() throws Exception {
    ScheduledJob scheduledJob = getTestJob();
    scheduledJob.setEndDate(null);
    scheduledJobRepository.save(scheduledJob);

    NoOpScheduledJobTest.successEvent = false;

    // Wait for job to be in progress
    waitForCondition(
        () -> getTestJob().getJobStatus().getEnum() == ScheduledJobStatus.IN_PROGRESS,
        "Max retries met waiting for scheduled job to go in progress.");

    assertNotNull(getTestJob().getStartDate());

    // Wait for job to succeed
    waitForCondition(
        () -> getTestJob().getJobStatus().getEnum() == ScheduledJobStatus.SUCCEEDED,
        "Max retries met waiting for scheduled job to succeed.");

    assertNotNull(getTestJob().getEndDate());
    // Make sure onSuccess event was called by listener
    assertTrue(NoOpScheduledJobTest.successEvent);
  }

  @Test
  public void testEnableDisableJob() throws Exception {
    // Disable the job
    ScheduledJob scheduledJob = getTestJob();
    scheduledJob.setEnabled(false);
    scheduledJobRepository.save(scheduledJob);

    // Wait as the job could have been executing when we disabled it
    Thread.sleep(JOB_WAIT_TIME_MS);

    // The job should not be executing now - set the end date to null to test there is no future job
    // runs
    scheduledJob = getTestJob();
    scheduledJob.setEndDate(null);
    scheduledJobRepository.save(scheduledJob);

    // Get the current start time of the job to check it doesn't change later on
    ZonedDateTime start = getTestJob().getStartDate();

    // Wait again to see if a job runs
    Thread.sleep(JOB_WAIT_TIME_MS);

    // Check that the job hasn't changed the end date and that the start time is the same
    // As the job is disabled, these fields should not have changed
    assertNull(scheduledJob.getEndDate());
    assertEquals(start, getTestJob().getStartDate());

    // Enable the job and ensure the end date gets changed
    scheduledJob.setEnabled(true);
    scheduledJobRepository.save(scheduledJob);
    Thread.sleep(JOB_WAIT_TIME_MS);
    if (start != null) assertNotEquals(start, getTestJob().getEndDate());
  }

  @Test
  public void testJobFails() throws Exception {
    // Use static variable to manipulate the job, Quartz is in charge of creating the instance of
    // the job so a spy won't work without using SpyBean which causes a new context to be spun up.
    NoOpScheduledJobTest.throwException = true;
    NoOpScheduledJobTest.failureEvent = false;
    Thread.sleep(JOB_WAIT_TIME_MS);
    waitForCondition(
        () -> getTestJob().getJobStatus().getEnum() == ScheduledJobStatus.FAILED,
        "Max retries met waiting for scheduled job to fail.");

    // Check the start and end dates
    assertNotNull(getTestJob().getEndDate());
    assertTrue(NoOpScheduledJobTest.failureEvent);
    NoOpScheduledJobTest.throwException = false;
  }

  @Test
  public void testJobCleanup() throws Exception {
    scheduledJobManager.uuidPool = new HashSet<>();
    scheduledJobManager.uuidPool.add(TEST_UUID);
    scheduledJobManager.cleanQuartzJobs();
    assertEquals(1, scheduledJobManager.getAllJobKeys().size());

    scheduledJobManager.uuidPool = new HashSet<>();
    scheduledJobManager.cleanQuartzJobs();
    assertEquals(0, scheduledJobManager.getAllJobKeys().size());
  }

  private ScheduledJob getTestJob() {
    return scheduledJobRepository.findByUuid(TEST_UUID).get();
  }

  private void waitForCondition(Supplier<Boolean> condition, String failMessage)
      throws InterruptedException {
    int retryCount = 0;
    while (!condition.get()) {
      if (retryCount++ >= MAX_RETRIES) {
        fail(failMessage);
      }
      Thread.sleep(RETRY_DELAY_MS);
    }
  }

  private ThirdPartySyncJobConfig createThirdPartySyncJobConfig(
      String uuid, String repositoryName) {
    ThirdPartySyncJobConfig thirdPartySyncJobConfig = new ThirdPartySyncJobConfig();
    thirdPartySyncJobConfig.setUuid(uuid);
    thirdPartySyncJobConfig.setCron("0/2 * * * * ?");
    thirdPartySyncJobConfig.setRepository(repositoryName);
    thirdPartySyncJobConfig.setThirdPartyProjectId("123456");
    thirdPartySyncJobConfig.setActions(List.of());
    thirdPartySyncJobConfig.setPluralSeparator("_");
    thirdPartySyncJobConfig.setLocaleMapping("");
    thirdPartySyncJobConfig.setSkipTextUnitsWithPattern("");
    thirdPartySyncJobConfig.setSkipAssetsWithPathPattern("");
    thirdPartySyncJobConfig.setIncludeTextUnitsWithPattern("");
    thirdPartySyncJobConfig.setOptions(List.of());
    return thirdPartySyncJobConfig;
  }
}
