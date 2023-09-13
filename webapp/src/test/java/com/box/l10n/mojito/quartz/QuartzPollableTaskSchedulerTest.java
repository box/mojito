package com.box.l10n.mojito.quartz;

import static com.box.l10n.mojito.quartz.QuartzSchedulerManager.DEFAULT_SCHEDULER_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskBlobStorage;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class QuartzPollableTaskSchedulerTest extends ServiceTestBase {

  @Autowired QuartzPollableTaskScheduler quartzPollableTaskScheduler;

  @Autowired PollableTaskBlobStorage pollableTaskBlobStorage;

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
