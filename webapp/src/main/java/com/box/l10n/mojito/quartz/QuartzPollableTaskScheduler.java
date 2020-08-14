package com.box.l10n.mojito.quartz;

import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableTaskBlobStorage;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.ibm.icu.text.MessageFormat;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.box.l10n.mojito.quartz.QuartzConfig.DYNAMIC_GROUP_NAME;

@Component
public class QuartzPollableTaskScheduler {
    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(QuartzPollableTaskScheduler.class);

    @Autowired
    Scheduler scheduler;

    @Autowired
    PollableTaskService pollableTaskService;

    @Autowired
    PollableTaskBlobStorage pollableTaskBlobStorage;

    @Autowired
    ObjectMapper objectMapper;

    public <I, O> PollableFuture<O> scheduleJob(Class<? extends QuartzPollableJob<I, O>> clazz, I input) {
        QuartzJobInfo<I, O> quartzJobInfo = QuartzJobInfo.newBuilder(clazz).
                withInput(input).
                withMessage(clazz.getSimpleName()).
                build();

        return scheduleJob(quartzJobInfo);
    }

    /**
     * Schedules a job.
     *
     * @param clazz                 class of the job to be executed
     * @param input                 the input of the job (will get serialized inline the quartz data or in the blobstorage, see inlineInput)
     * @param parentId              optional parentId for the pollable task id associated with the job
     * @param message               set on the pollable task
     * @param expectedSubTaskNumber set on the pollable task
     * @param triggerStartDate      date at which the job should be started
     * @param uniqueId              optional id used to generate the job keyname. If not provided the pollable task id is used.
     *                              Pollable id keeps changing, unique id can be used for recuring jobs (eg. update stats of repositry xyz)
     * @param inlineInput           to inline the input in quartz data or save it in the blobstorage
     * @param <I>
     * @param <O>
     * @return
     */
    public <I, O> PollableFuture<O> scheduleJob(QuartzJobInfo<I, O> quartzJobInfo) {

        String pollableTaskName = getPollableTaskName(quartzJobInfo.getClazz());

        logger.debug("Create currentPollableTask with name: {}", pollableTaskName);
        PollableTask pollableTask = pollableTaskService.createPollableTask(
                quartzJobInfo.getParentId(),
                pollableTaskName,
                quartzJobInfo.getMessage(),
                quartzJobInfo.getExpectedSubTaskNumber(),
                quartzJobInfo.getTimeout());

        String uniqueId = quartzJobInfo.getUniqueId() != null ?
                quartzJobInfo.getUniqueId() : pollableTask.getId().toString();

        String keyName = getKeyName(quartzJobInfo.getClazz(), uniqueId);

        try {
            TriggerKey triggerKey = new TriggerKey(keyName, DYNAMIC_GROUP_NAME);
            JobKey jobKey = new JobKey(keyName, DYNAMIC_GROUP_NAME);

            JobDetail jobDetail = scheduler.getJobDetail(jobKey);

            if (jobDetail == null) {
                logger.debug("Job doesn't exist, create for key: {}", keyName);
                jobDetail = JobBuilder.newJob().ofType(quartzJobInfo.getClazz())
                        .withIdentity(jobKey)
                        .build();
            }

            logger.debug("Schedule a job for key: {}", keyName);

            TriggerBuilder<Trigger> triggerTriggerBuilder = TriggerBuilder.newTrigger()
                    .startAt(quartzJobInfo.getTriggerStartDate())
                    .forJob(jobDetail)
                    .usingJobData(QuartzPollableJob.POLLABLE_TASK_ID, pollableTask.getId().toString())
                    .withIdentity(triggerKey);

            if (quartzJobInfo.isInlineInput()) {
                logger.debug("This job input is inlined into the quartz job");
                triggerTriggerBuilder.usingJobData(QuartzPollableJob.INPUT, objectMapper.writeValueAsStringUnchecked(quartzJobInfo.getInput()));
            } else {
                logger.debug("The input data is saved into the blob storage");
                pollableTaskBlobStorage.saveInput(pollableTask.getId(), quartzJobInfo.getInput());
            }

            Trigger trigger = triggerTriggerBuilder.build();

            if (!scheduler.checkExists(triggerKey)) {
                logger.debug("Schedule QuartzPollableJob with key: {}", keyName);
                scheduler.scheduleJob(jobDetail, trigger);
            } else {
                logger.debug("Job already scheduled for key: {}, reschedule", keyName);
                scheduler.rescheduleJob(triggerKey, trigger);
            }
        } catch (SchedulerException se) {
            String msg = MessageFormat.format("Couldn't schedule QuartzPollableJob with key: {0}", keyName);
            logger.error(msg, se);
            throw new RuntimeException(msg, se);
        }

        Class<O> jobOutputType = getJobOutputType(quartzJobInfo);
        return new QuartzPollableFutureTask<O>(pollableTask, jobOutputType);
    }

    <I, O> Class<O> getJobOutputType(QuartzJobInfo<I, O> quartzJobInfo) {
        QuartzPollableJob<I, O> quartzPollableJob = null;

        try {
            quartzPollableJob = quartzJobInfo.getClazz().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Can't get the output type of the job", e);
        }

        Class<? super O> outputType = quartzPollableJob.getOutputType();
        return (Class<O>) outputType;
    }

    String getKeyName(Class clazz, String id) {
        return getPollableTaskName(clazz) + "_" + id;
    }

    String getPollableTaskName(Class clazz) {
        return getShortClassName(clazz);
    }

    /**
     * TODO(perf) this acutally not perf related but taking a not
     * we need to consider limit the name lenght, since it sometimes doens't fit in the column, needs review
     * rename test for now
     */
    String getShortClassName(Class clazz) {
        String result = clazz.getCanonicalName();

        if (result.length() > 170) {
            String[] split = clazz.getCanonicalName().split("\\.");

            if (split.length == 1) {
                result = split[0];
            } else {
                String[] splitWithoutLast = Arrays.copyOfRange(split, 0, split.length - 1);
                String joined = Stream.of(splitWithoutLast).map(s -> s.isEmpty() ? "" : s.substring(0, 1)).collect(Collectors.joining("."));
                result = joined + "." + split[split.length - 1];
            }
        }

        return result;
    }

}
