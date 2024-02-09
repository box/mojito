package com.box.l10n.mojito.quartz;

import static com.box.l10n.mojito.quartz.QuartzSchedulerManager.DEFAULT_SCHEDULER_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.service.DBUtils;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskBlobStorage;
import com.box.l10n.mojito.service.pollableTask.PollableTaskRepository;
import java.util.concurrent.ExecutionException;
import org.junit.Assume;
import org.junit.Test;
import org.quartz.DisallowConcurrentExecution;
import org.springframework.beans.factory.annotation.Autowired;

public class QuartzPollableTaskSchedulerTest extends ServiceTestBase {

  @Autowired QuartzPollableTaskScheduler quartzPollableTaskScheduler;

  @Autowired PollableTaskBlobStorage pollableTaskBlobStorage;

  @Autowired DBUtils dbUtils;

  @Autowired PollableTaskRepository pollableTaskRepository;

  @Test
  public void test() throws ExecutionException, InterruptedException {
    PollableFuture<AQuartzPollableJobOutput> pollableFuture =
        quartzPollableTaskScheduler.scheduleJob(
            AQuartzPollableJob.class, 10L, DEFAULT_SCHEDULER_NAME);
    AQuartzPollableJobOutput s = pollableFuture.get();
    assertEquals("output: 10", s.getOutput());

    PollableFuture<AQuartzPollableJobOutput> stringPollableFuture2 =
        quartzPollableTaskScheduler.scheduleJob(
            AQuartzPollableJob.class, 10L, DEFAULT_SCHEDULER_NAME);
    AQuartzPollableJobOutput s2 = stringPollableFuture2.get();
    assertEquals("output: 10", s2.getOutput());
  }

  @Test
  public void testVoid() throws ExecutionException, InterruptedException {
    PollableFuture<Void> pollableFuture =
        quartzPollableTaskScheduler.scheduleJob(
            VoidQuartzPollableJob.class, 10L, DEFAULT_SCHEDULER_NAME);
    Void aVoid = pollableFuture.get();
    assertEquals(null, aVoid);
    try {
      Object output =
          pollableTaskBlobStorage.getOutputJson(pollableFuture.getPollableTask().getId());
      fail();
    } catch (RuntimeException re) {
      assertTrue(re.getMessage().startsWith("Can't get the output json for:"));
    }
  }

  @Test
  public void testGetShortClassName() {
    assertEquals(
        "com.box.l10n.mojito.quartz.QuartzPollableTaskSchedulerTest",
        quartzPollableTaskScheduler.getShortClassName(QuartzPollableTaskSchedulerTest.class));
    assertEquals(
        "c.b.l.m.q.Q.ALongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongNameClassForTest",
        quartzPollableTaskScheduler.getShortClassName(
            ALongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongNameClassForTest
                .class));
  }

  @Test
  public void testRescheduleJobsWithUniqueIdAndVoidOutput()
      throws ExecutionException, InterruptedException {
    /**
     * This test verifies that in the event of a rescheduling of a job that has void output, the
     * pollable task associated with the Quartz Trigger in a BLOCKED state is marked as finished
     * with a message stating it has been skipped.
     *
     * <p>This ensures that no more than 2 pollable tasks (the executing tasks and the task waiting
     * to execute) are present at the same time for the same uniqueId.
     *
     * <p>The tests steps are as follows: 1. Schedule a job with a uniqueId 2. Schedule a job with
     * the same uniqueId, this time with different output 3. Schedule a job with the same uniqueId,
     * this time with the same output as the first job 4. Verify that the output of the first job is
     * returned as expected 5. Verify that the output of the second job is null as it has been
     * skipped due to the rescheduling caused by the third task. Verify that the output of the third
     * job is returned.
     */
    Assume.assumeTrue(dbUtils.isQuartzMysql());
    QuartzJobInfo<Long, Void> quartzJobInfo =
        QuartzJobInfo.newBuilder(AQuartzPollableJobVoidOutput.class).withUniqueId("test1").build();
    QuartzJobInfo<Long, Void> quartzJobInfo2 =
        QuartzJobInfo.newBuilder(AQuartzPollableJobVoidOutput.class).withUniqueId("test1").build();
    QuartzJobInfo<Long, Void> quartzJobInfo3 =
        QuartzJobInfo.newBuilder(AQuartzPollableJobVoidOutput.class).withUniqueId("test1").build();
    quartzPollableTaskScheduler.cleanupOnUniqueIdReschedule = true;
    PollableFuture<Void> pollableFuture = quartzPollableTaskScheduler.scheduleJob(quartzJobInfo);
    PollableFuture<Void> pollableFuture2 = quartzPollableTaskScheduler.scheduleJob(quartzJobInfo2);
    PollableFuture<Void> pollableFuture3 = quartzPollableTaskScheduler.scheduleJob(quartzJobInfo3);

    // Wait for all tasks to complete
    pollableFuture3.get();

    PollableTask pollableTask =
        pollableTaskRepository.findById(pollableFuture.getPollableTask().getId()).get();

    assertTrue(pollableTask.isAllFinished());
    assertNull(pollableTask.getMessage());

    pollableTask = pollableTaskRepository.findById(pollableFuture2.getPollableTask().getId()).get();
    assertTrue(pollableTask.isAllFinished());
    assertTrue(
        pollableTask
            .getMessage()
            .contains(
                "Job skipped as new job re-scheduled with the same unique id, tracked by pollable task id:"));

    pollableTask = pollableTaskRepository.findById(pollableFuture3.getPollableTask().getId()).get();
    assertTrue(pollableTask.isAllFinished());
    assertNull(pollableTask.getMessage());
  }

  @Test
  public void testRescheduleJobsWithUniqueIdAndNonVoidOutput()
      throws ExecutionException, InterruptedException {
    Assume.assumeTrue(dbUtils.isQuartzMysql());
    QuartzJobInfo<Long, AQuartzPollableJobOutput> quartzJobInfo =
        QuartzJobInfo.newBuilder(AQuartzPollableJobNonVoidOutput.class)
            .withUniqueId("test1")
            .withInput(10L)
            .build();
    QuartzJobInfo<Long, AQuartzPollableJobOutput> quartzJobInfo2 =
        QuartzJobInfo.newBuilder(AQuartzPollableJobNonVoidOutput.class)
            .withUniqueId("test1")
            .withInput(20L)
            .build();
    QuartzJobInfo<Long, AQuartzPollableJobOutput> quartzJobInfo3 =
        QuartzJobInfo.newBuilder(AQuartzPollableJobNonVoidOutput.class)
            .withUniqueId("test1")
            .withInput(30L)
            .build();
    quartzPollableTaskScheduler.cleanupOnUniqueIdReschedule = true;
    PollableFuture<AQuartzPollableJobOutput> pollableFuture =
        quartzPollableTaskScheduler.scheduleJob(quartzJobInfo);
    PollableFuture<AQuartzPollableJobOutput> pollableFuture2 =
        quartzPollableTaskScheduler.scheduleJob(quartzJobInfo2);
    PollableFuture<AQuartzPollableJobOutput> pollableFuture3 =
        quartzPollableTaskScheduler.scheduleJob(quartzJobInfo3);

    // Wait jobs 1 & 3 to complete, job 2 will be a zombie due to providing non void output so it
    // won't be skipped
    pollableFuture.get();
    pollableFuture3.get();

    assertEquals("output: 10", pollableFuture.get().getOutput());
    PollableTask pollableTask =
        pollableTaskRepository.findById(pollableFuture2.getPollableTask().getId()).get();
    assertFalse(pollableTask.isAllFinished());
    assertNull(pollableTask.getMessage());
    assertEquals("output: 30", pollableFuture3.get().getOutput());
  }

  @DisallowConcurrentExecution
  public static class AQuartzPollableJobVoidOutput extends VoidQuartzPollableJob {
    public Void call(Long input) throws Exception {
      Thread.sleep(2000);
      return super.call(input);
    }
  }

  @DisallowConcurrentExecution
  public static class AQuartzPollableJobNonVoidOutput extends AQuartzPollableJob2 {
    public AQuartzPollableJobOutput call(Long input) throws Exception {
      Thread.sleep(2000);
      return super.call(input);
    }
  }

  public static class AQuartzPollableJob extends AQuartzPollableJob2 {}

  public static class AQuartzPollableJob2
      extends QuartzPollableJob<Long, AQuartzPollableJobOutput> {
    @Override
    public AQuartzPollableJobOutput call(Long input) throws Exception {
      AQuartzPollableJobOutput aQuartzPollableJobOutput = new AQuartzPollableJobOutput();
      aQuartzPollableJobOutput.setOutput("output: " + input);
      return aQuartzPollableJobOutput;
    }
  }

  static class AQuartzPollableJobOutput {
    String output;

    public String getOutput() {
      return output;
    }

    public void setOutput(String output) {
      this.output = output;
    }
  }

  public static class VoidQuartzPollableJob extends QuartzPollableJob<Long, Void> {
    @Override
    public Void call(Long input) throws Exception {
      return null;
    }
  }

  static
  class ALongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongLongNameClassForTest {}
}
