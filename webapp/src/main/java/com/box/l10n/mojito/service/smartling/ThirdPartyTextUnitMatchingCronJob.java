package com.box.l10n.mojito.service.smartling;

import org.quartz.*;
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
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author cegbukichi
 */
@Profile("!disablescheduling")
@ConditionalOnProperty(value = "l10n.thirdPartyTextUnitMatching.cron")
@Configuration
@Component
@DisallowConcurrentExecution
public class ThirdPartyTextUnitMatchingCronJob implements Job {

    private static Logger logger = LoggerFactory.getLogger(ThirdPartyTextUnitMatchingCronJob.class);

    @Value("${l10n.thirdPartyTextUnitMatching.cron}")
    String thirdPartyTextUnitMatchingCron;

    @Autowired
    TaskScheduler taskScheduler;

    @Autowired
    ThirdPartyTextUnitMatchingService thirdPartyTextUnitMatchingService;

    @Value("#{'${l10n.smartling.projectIds}'.split(',')}")
    List<String> smartlingProjectIds;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.debug("Starting thirdPartyTextUnitMatchingCronJob execution");
        for (String smartlingProjectId : smartlingProjectIds) {
            thirdPartyTextUnitMatchingService.updateThirdPartyTextUnitMapping(smartlingProjectId.trim());
        }
    }


    @Bean(name = "thirdPartyTextUnitMatchingCron")
    public JobDetailFactoryBean jobDetailThirdPartyTextUnitMatchingCronJob() {
        JobDetailFactoryBean jobDetailFactoryBean = new JobDetailFactoryBean();
        jobDetailFactoryBean.setJobClass(ThirdPartyTextUnitMatchingCronJob.class);
        jobDetailFactoryBean.setDescription("Check for third party text units and match with mojito text units");
        jobDetailFactoryBean.setDurability(true);
        return jobDetailFactoryBean;
    }

    @Bean
    public CronTriggerFactoryBean triggerThirdPartyTextUnitMatchingCronJob(@Qualifier("thirdPartyTextUnitMatchingCron") JobDetail job) {
        logger.debug("Triggering thirdPartyTextUnitMatchingCronJob");
        CronTriggerFactoryBean trigger = new CronTriggerFactoryBean();
        trigger.setJobDetail(job);
        trigger.setCronExpression(thirdPartyTextUnitMatchingCron);
        return trigger;
    }

}