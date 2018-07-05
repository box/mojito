package com.box.l10n.mojito.service.pollableTask;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import org.springframework.stereotype.Component;

/**
 * @author aloison
 */
@Profile("!disablescheduling")
@Configuration
@Component
@DisallowConcurrentExecution
public class PollableTaskCleanupJob implements Job {

    static final String FINISH_ZOMBIE_TASKS_WITH_ERROR = "Finish zombie tasks with error";

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(PollableTaskCleanupJob.class);


    @Autowired
    PollableTaskCleanupService pollableTaskCleanupService;

    /**
     * @see PollableTaskCleanupService#finishZombieTasksWithError()
     * It is triggered every 30 seconds (= 30,000 milliseconds).
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.debug(FINISH_ZOMBIE_TASKS_WITH_ERROR);
        pollableTaskCleanupService.finishZombieTasksWithError();
    }

    @Bean(name = "jobDetailPollableTaskCleanup")
    public JobDetailFactoryBean jobDetailPollableTaskCleanup() {
        JobDetailFactoryBean jobDetailFactory = new JobDetailFactoryBean();
        jobDetailFactory.setJobClass(PollableTaskCleanupJob.class);
        jobDetailFactory.setDescription(FINISH_ZOMBIE_TASKS_WITH_ERROR);
        jobDetailFactory.setDurability(true);
        return jobDetailFactory;
    }

    @Bean
    public SimpleTriggerFactoryBean triggerPollableTaskCleanup(@Qualifier("jobDetailPollableTaskCleanup") JobDetail job) {
        SimpleTriggerFactoryBean trigger = new SimpleTriggerFactoryBean();
        trigger.setJobDetail(job);
        trigger.setRepeatInterval(30000);
        trigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
        return trigger;
    }
}
