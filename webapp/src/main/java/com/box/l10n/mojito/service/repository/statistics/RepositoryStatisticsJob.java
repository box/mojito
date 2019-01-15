package com.box.l10n.mojito.service.repository.statistics;

import com.box.l10n.mojito.entity.RepositoryStatistic;
import com.box.l10n.mojito.service.scheduler.SchedulableJob;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Update {@link RepositoryStatistic}s.
 *
 * @author jaurambault
 */
@Component
@DisallowConcurrentExecution
public class RepositoryStatisticsJob extends SchedulableJob {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(RepositoryStatisticsJob.class);

    static final String REPOSITORY_ID = "repositoryId";

    @Autowired
    RepositoryStatisticService repositoryStatisticService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long repositoryId = context.getMergedJobDataMap().getLong(REPOSITORY_ID);
        logger.info("Execute for repositoryId: {}", repositoryId);
        repositoryStatisticService.updateStatistics(repositoryId);
    }

    @Override
    protected String getDescription() {
        return "Update statistics of a repository";
    }

    public void schedule(Long repositoryId) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(REPOSITORY_ID, repositoryId.toString());
        schedule(jobDataMap, REPOSITORY_ID);
    }
}
