package com.box.l10n.mojito.quartz;

import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.service.pollableTask.ExceptionHolder;
import com.box.l10n.mojito.service.pollableTask.PollableTaskBlobStorage;
import com.box.l10n.mojito.service.pollableTask.PollableTaskExceptionUtils;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.google.common.reflect.TypeToken;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/** @author jaurambault */
public abstract class QuartzPollableJob<I, O> implements Job {

  public static final String POLLABLE_TASK_ID = "pollableTaskId";
  public static final String INPUT = "input";

  /** logger */
  static Logger logger = LoggerFactory.getLogger(QuartzPollableJob.class);

  final TypeToken<I> typeTokenInput = new TypeToken<I>(getClass()) {};
  final TypeToken<O> typeTokenOutput = new TypeToken<O>(getClass()) {};

  @Autowired PollableTaskService pollableTaskService;

  @Autowired PollableTaskExceptionUtils pollableTaskExceptionUtils;

  @Autowired PollableTaskBlobStorage pollableTaskBlobStorage;

  @Autowired
  @Qualifier("fail_on_unknown_properties_false")
  ObjectMapper objectMapper;

  PollableTask currentPollableTask;

  public abstract O call(I input) throws Exception;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    Long pollableTaskId = context.getMergedJobDataMap().getLong(POLLABLE_TASK_ID);
    currentPollableTask = pollableTaskService.getPollableTask(pollableTaskId);

    ExceptionHolder exceptionHolder = new ExceptionHolder(currentPollableTask);

    try {
      I callInput;

      String inputStringFromJob = context.getMergedJobDataMap().getString(INPUT);

      if (inputStringFromJob != null) {
        logger.debug("Inlined data, read from job data");
        callInput =
            (I) objectMapper.readValueUnchecked(inputStringFromJob, typeTokenInput.getRawType());
      } else {
        logger.debug("No inlined data, read from blob storage");
        callInput =
            (I) pollableTaskBlobStorage.getInput(pollableTaskId, typeTokenInput.getRawType());
      }

      O callOutput = call(callInput);

      if (!typeTokenOutput.getRawType().equals(Void.class)) {
        pollableTaskBlobStorage.saveOutput(pollableTaskId, callOutput);
      }
    } catch (Throwable t) {
      pollableTaskExceptionUtils.processException(t, exceptionHolder);
    } finally {
      currentPollableTask =
          pollableTaskService.finishTask(currentPollableTask.getId(), null, exceptionHolder, null);
    }
  }

  public Class<? super O> getOutputType() {
    return typeTokenOutput.getRawType();
  }

  protected PollableTask getCurrentPollableTask() {
    return currentPollableTask;
  }
}
