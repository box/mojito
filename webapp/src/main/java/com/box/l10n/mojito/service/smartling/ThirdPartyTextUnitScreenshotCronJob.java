package com.box.l10n.mojito.service.smartling;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.ScreenshotTextUnit;
import com.box.l10n.mojito.entity.ThirdPartyTextUnitScreenshot;
import com.box.l10n.mojito.service.repository.RepositoryService;
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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;

/**
 * @author cegbukichi
 */
@Profile("!disablescheduling")
@ConditionalOnProperty(value = "l10n.thirdPartyTextUnitScreenshot.cron")
@Configuration
@Component
@DisallowConcurrentExecution
public class ThirdPartyTextUnitScreenshotCronJob implements Job {

    private static Logger logger = LoggerFactory.getLogger(ThirdPartyTextUnitScreenshotCronJob.class);

    @Value("${l10n.thirdPartyTextUnitScreenshot.cron}")
    String thirdPartyTextUnitScreenshotCron;

    @Autowired
    TaskScheduler taskScheduler;

    @Autowired
    ThirdPartyTextUnitScreenshotService thirdPartyTextUnitScreenshotService;

    @Value("#{'${l10n.smartling.projectIds}'.split(',')}")
    List<String> smartlingProjectIds;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.debug("Starting thirdPartyTextUnitScreenshotCronJob execution");
//        List<ScreenshotTextUnit> thirdPartyTextUnitScreenshots = thirdPartyTextUnitScreenshotService.getScreenshotTextUnitsWithUnpushedScreenshots();
        try {
        smartlingProjectIds.forEach(projectId -> {
            thirdPartyTextUnitScreenshotService.pushNewScreenshots(projectId);
        });} catch (HttpStatusCodeException e) {
            logger.info(e.getResponseBodyAsString());
        }
    }

    @Bean(name = "thirdPartyTextUnitScreenshotCron")
    public JobDetailFactoryBean jobDetailThirdPartyTextUnitScreenshotCronJob() {
        JobDetailFactoryBean jobDetailFactoryBean = new JobDetailFactoryBean();
        jobDetailFactoryBean.setJobClass(ThirdPartyTextUnitScreenshotCronJob.class);
        jobDetailFactoryBean.setDescription("Check for new screenshot runs and forward to third party according to matching with mojito text units");
        jobDetailFactoryBean.setDurability(true);
        return jobDetailFactoryBean;
    }

    @Bean
    public CronTriggerFactoryBean triggerThirdPartyTextUnitScreenshotCronJob(@Qualifier("thirdPartyTextUnitScreenshotCron") JobDetail job) {
        logger.debug("Triggering thirdPartyTextUnitScreenshotCronJob");
        CronTriggerFactoryBean trigger = new CronTriggerFactoryBean();
        trigger.setJobDetail(job);
        trigger.setCronExpression(thirdPartyTextUnitScreenshotCron);
        return trigger;
    }
}
