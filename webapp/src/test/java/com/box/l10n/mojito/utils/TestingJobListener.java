package com.box.l10n.mojito.utils;

import static com.box.l10n.mojito.quartz.QuartzPollableJob.INPUT;

import com.box.l10n.mojito.json.ObjectMapper;
import java.util.LinkedList;
import java.util.Queue;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

public class TestingJobListener implements JobListener {

  private static int executionCount = 0;

  private final Queue<JobExecutionContext> toBeExecuted = new LinkedList<>();
  private final Queue<JobExecutionContext> executed = new LinkedList<>();
  private final Queue<JobExecutionException> exceptions = new LinkedList<>();
  private final ObjectMapper objectMapper;

  public TestingJobListener(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public TestingJobListener() {
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public String getName() {
    return "TestingJobListener";
  }

  @Override
  public void jobToBeExecuted(JobExecutionContext context) {
    toBeExecuted.offer(context);
  }

  @Override
  public void jobExecutionVetoed(JobExecutionContext context) {}

  @Override
  public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
    executed.offer(context);
    exceptions.offer(jobException);
    incrementExecutionCount();
  }

  public Queue<JobExecutionContext> getExecuted() {
    return executed;
  }

  public int getExecutionCount() {
    return executionCount;
  }

  private synchronized void incrementExecutionCount() {
    executionCount++;
  }

  public synchronized void resetExecutionCount() {
    executionCount = 0;
  }

  public Queue<JobExecutionContext> getToBeExecuted() {
    return toBeExecuted;
  }

  public <T> T getFirstInputMapAs(Class<T> klass) {
    String input =
        getExecuted().stream()
            .filter(context -> context.getMergedJobDataMap().containsKey(INPUT))
            .map(context -> context.getMergedJobDataMap().getString(INPUT))
            .findFirst()
            .orElse("");

    return objectMapper.readValueUnchecked(input, klass);
  }
}
