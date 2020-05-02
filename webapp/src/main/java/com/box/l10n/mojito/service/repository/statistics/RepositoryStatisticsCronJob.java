package com.box.l10n.mojito.service.repository.statistics;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryStatistic;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Update {@link RepositoryStatistic}s on a regular basis.
 * <p>
 * This is required to re-compute OOSLA information. It will be also useful
 * when adding new statistics and have them recomputed automatically. Before
 * we'd to wait for a change in the repository.
 *
 * @author jaurambault
 */
@Profile("!disablescheduling")
@ConditionalOnProperty(value = "l10n.repository-statistics.scheduler.cron")
@Configuration
@Component
@DisallowConcurrentExecution
public class RepositoryStatisticsCronJob implements Job {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(RepositoryStatisticsCronJob.class);

    @Autowired
    RepositoryRepository repositoryRepository;

    @Lazy
    @Autowired
    RepositoryStatisticsJobScheduler repositoryStatisticsJobScheduler;

    @Value("${l10n.repositoryStatistics.scheduler.cron}")
    String cron;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.debug("Sets repository stats as out of date");
        List<Repository> repositories = repositoryRepository.findByDeletedFalseOrderByNameAsc();
        for (Repository repository : repositories) {
            repositoryStatisticsJobScheduler.schedule(repository.getId());
        }
    }

    @Bean(name = "repositoryStatisticsCron")
    public JobDetailFactoryBean jobDetailRepositoryStatisticsCron() {
        JobDetailFactoryBean jobDetailFactory = new JobDetailFactoryBean();
        jobDetailFactory.setJobClass(RepositoryStatisticsCronJob.class);
        jobDetailFactory.setDescription("Mark repository as out of data to later recompute stats");
        jobDetailFactory.setDurability(true);
        return jobDetailFactory;
    }

    @Bean
    public CronTriggerFactoryBean triggerRepositoryStatisticsCron(@Qualifier("repositoryStatisticsCron") JobDetail job) {
        CronTriggerFactoryBean trigger = new CronTriggerFactoryBean();
        trigger.setCronExpression(cron);
        trigger.setJobDetail(job);
        return trigger;
    }

}
