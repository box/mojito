package com.box.l10n.mojito.service.dbupdate;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.repository.RepositoryService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
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

import java.util.List;

/**
 * @author jeanaurambault
 */
@Profile("!disablescheduling")
@Configuration
@Component
@DisallowConcurrentExecution
public class RepositoryManualScreenshotRunJob implements Job {

    static Logger logger = LoggerFactory.getLogger(RepositoryManualScreenshotRunJob.class);

    @Autowired
    RepositoryRepository repositoryRepository;

    @Autowired
    RepositoryService repositoryService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("One time update of manual screenshot run on repository objects");
        List<Repository> repositories = repositoryRepository.findAll();

        for (Repository repository : repositories) {
            if (repository.getManualScreenshotRun() == null) {
                repositoryService.addManualScreenshotRun(repository);
            }
        }
    }

    @Bean(name = "repositoryManualScreenshotRun")
    public JobDetailFactoryBean jobDetailRepositoryManualScreenshotRunJob() {
        JobDetailFactoryBean jobDetailFactory = new JobDetailFactoryBean();
        jobDetailFactory.setJobClass(RepositoryManualScreenshotRunJob.class);
        jobDetailFactory.setDescription("One time update of manual screenshot run on repository objects");
        jobDetailFactory.setDurability(true);
        return jobDetailFactory;
    }

    @Bean
    public SimpleTriggerFactoryBean triggerRepositoryManualScreenshotRunJob(@Qualifier("repositoryManualScreenshotRun") JobDetail job) {
        SimpleTriggerFactoryBean simpleTriggerFactoryBean = new SimpleTriggerFactoryBean();
        simpleTriggerFactoryBean.setJobDetail(job);
        simpleTriggerFactoryBean.setRepeatCount(0);
        return simpleTriggerFactoryBean;
    }
}
