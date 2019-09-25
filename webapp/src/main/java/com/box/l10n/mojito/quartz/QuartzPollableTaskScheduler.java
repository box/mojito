package com.box.l10n.mojito.quartz;

import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.ibm.icu.text.MessageFormat;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.box.l10n.mojito.quartz.QuartzConfig.DYNAMIC_GROUP_NAME;

@Component
public class QuartzPollableTaskScheduler {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(QuartzPollableTaskScheduler.class);

    static final long TIMEOUT = 3600L;

    @Autowired
    Scheduler scheduler;

    @Autowired
    PollableTaskService pollableTaskService;

    @Autowired
    ObjectMapper objectMapper;

    public <T> PollableFuture<T> scheduleJob(Class<? extends QuartzPollableJob> clazz, Object input) {
        return scheduleJob(clazz, input, null, null, 0);
    }

    public <T> PollableFuture<T> scheduleJob(Class<? extends QuartzPollableJob> clazz,
                                             Object input,
                                             Long parentId,
                                             String message,
                                             int expectedSubTaskNumber) {

        String pollableTaskName = getPollableTaskName(clazz);

        logger.debug("Create currentPollableTask with name: {}", pollableTaskName);
        PollableTask pollableTask = pollableTaskService.createPollableTask(parentId, pollableTaskName, message,
                expectedSubTaskNumber, TIMEOUT);

        String keyName = getKeyName(clazz, pollableTask.getId());

        JobDetail jobDetail = JobBuilder.newJob().ofType(clazz)
                .withIdentity(keyName, DYNAMIC_GROUP_NAME)
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .startNow()
                .forJob(jobDetail)
                .usingJobData(QuartzPollableJob.POLLABLE_TASK_ID, pollableTask.getId().toString())
                .usingJobData(QuartzPollableJob.INPUT, objectMapper.writeValueAsStringUnsafe(input))
                .withIdentity(keyName, DYNAMIC_GROUP_NAME).build();

        try {
            logger.debug("Schedule QuartzPollableJob with key: {}", keyName);
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            String msg = MessageFormat.format("Couldn't schedule QuartzPollableJob with key: {0}", keyName);
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }

        return new QuartzPollableFutureTask<T>(pollableTask);
    }


    String getKeyName(Class clazz, Long pollableTaskId) {
        return getPollableTaskName(clazz) + "_" + pollableTaskId;
    }

    private String getPollableTaskName(Class clazz) {
        return clazz.getCanonicalName();
    }

}
