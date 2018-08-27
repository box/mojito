package com.box.l10n.mojito.quartz;

import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.service.pollableTask.ExceptionHolder;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableFutureTaskResult;
import com.box.l10n.mojito.service.pollableTask.PollableTaskException;
import com.box.l10n.mojito.service.pollableTask.PollableTaskExceptionUtils;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.tm.importer.TextUnitBatchImporterService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.google.common.reflect.TypeToken;
import nu.validator.htmlparser.annotation.Auto;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author jaurambault
 */
public abstract class QuartzPollableJob<I, O> implements Job {

    public static final String POLLABLE_TASK_ID = "pollableTaskId";
    public static final String INPUT = "input";

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(QuartzPollableJob.class);

    final TypeToken<I> typeTokenInput = new TypeToken<I>(getClass()) {};

    @Autowired
    TextUnitBatchImporterService textUnitBatchImporterService;

    @Autowired
    PollableTaskService pollableTaskService;

    @Autowired
    PollableTaskExceptionUtils pollableTaskExceptionUtils;

    public abstract O call(I input) throws JobExecutionException;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long pollableTaskId = context.getMergedJobDataMap().getLong(POLLABLE_TASK_ID);
        PollableTask pollableTask = pollableTaskService.getPollableTask(pollableTaskId);

        Object output = null;
        ExceptionHolder exceptionHolder = new ExceptionHolder(pollableTask);

        try {
            I input = (I) jsonStringToObject(context.getMergedJobDataMap().getString(INPUT), typeTokenInput.getRawType());
            output = call(input);
        } catch (Throwable t) {
            pollableTaskExceptionUtils.processException(t, exceptionHolder);
        } finally {
            pollableTask = pollableTaskService.finishTask(
                    pollableTask.getId(),
                    null,
                    exceptionHolder,
                    null);
        }
    }

    Object jsonStringToObject(String jsonString, Class clazz) throws JobExecutionException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Object result = null;
        try {
            result = objectMapper.readValue(jsonString, clazz);
        } catch (IOException e) {
            throw new JobExecutionException("Can't convert json string to object", e);
        }
        return result;
    }

}
