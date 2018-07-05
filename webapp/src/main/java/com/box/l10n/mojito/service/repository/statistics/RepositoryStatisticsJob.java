package com.box.l10n.mojito.service.repository.statistics;

import com.box.l10n.mojito.aspect.StopWatch;
import com.box.l10n.mojito.entity.RepositoryStatistic;
import com.box.l10n.mojito.entity.StatisticsSchedule;
import com.box.l10n.mojito.service.repository.RepositoryRepository;

import java.util.List;

import org.joda.time.DateTime;
import org.quartz.*;
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
 * Update {@link RepositoryStatistic}s.
 *
 * See {@link RepositoryStatisticService#addJobIfMissing(Long)} to schedule on demand jobs.
 *
 * @author jaurambault
 */
@Profile("!disablescheduling")
@Configuration
@Component
@DisallowConcurrentExecution
public class RepositoryStatisticsJob implements Job {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(RepositoryStatisticsJob.class);

    @Autowired
    RepositoryRepository repositoryRepository;

    @Autowired
    RepositoryStatisticService repositoryStatisticService;

    @Autowired
    StatisticsScheduleRepository statisticsScheduleRepository;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long repositoryId = context.getMergedJobDataMap().getLong("repositoryId");
        logger.info("Update statistic for repositoryId: {}", repositoryId);
        repositoryStatisticService.updateStatistics(repositoryId);
    }

}
