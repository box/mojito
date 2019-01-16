package com.box.l10n.mojito.service.sla;

import com.box.l10n.mojito.service.repository.statistics.RepositoryStatisticsCronJob;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author jeanaurambault
 */
@Profile("!disablescheduling")
@ConditionalOnProperty(value = "l10n.slaChecker.incidentCheck.cron")
@Configuration
@Component
@DisallowConcurrentExecution
public class SlaCheckerCronJob implements Job {

    static Logger logger = LoggerFactory.getLogger(SlaCheckerCronJob.class);

    @Value("${l10n.slaChecker.incidentCheck.cron:}")
    String incidentCheckCron;

    @Autowired
    TaskScheduler taskScheduler;

    @Autowired
    SlaCheckerService slaCheckerService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        slaCheckerService.checkForIncidents();
    }

    @Bean(name = "slaCheckerCron")
    public JobDetailFactoryBean jobDetailSlaCheckerCronJob() {
        JobDetailFactoryBean jobDetailFactory = new JobDetailFactoryBean();
        jobDetailFactory.setJobClass(SlaCheckerCronJob.class);
        jobDetailFactory.setDescription("Check for incident related to SLA");
        jobDetailFactory.setDurability(true);
        return jobDetailFactory;
    }

    @Bean
    public CronTriggerFactoryBean triggerSlaCheckerCronJob(@Qualifier("slaCheckerCron") JobDetail job) {
        CronTriggerFactoryBean trigger = new CronTriggerFactoryBean();
        trigger.setJobDetail(job);
        trigger.setCronExpression(incidentCheckCron);
        return trigger;
    }
}
