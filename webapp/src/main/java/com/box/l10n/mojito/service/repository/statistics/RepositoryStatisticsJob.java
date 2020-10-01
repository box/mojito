package com.box.l10n.mojito.service.repository.statistics;

import com.box.l10n.mojito.entity.RepositoryStatistic;
import com.box.l10n.mojito.quartz.QuartzPollableJob;
import com.google.common.base.Preconditions;
import org.quartz.DisallowConcurrentExecution;
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
public class RepositoryStatisticsJob extends QuartzPollableJob<RepositoryStatisticsJobInput, Void> {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(RepositoryStatisticsJob.class);

    @Autowired
    RepositoryStatisticService repositoryStatisticService;

    @Override
    public Void call(RepositoryStatisticsJobInput input) throws Exception {
        Long repositoryId = input.getRepositoryId();
        Preconditions.checkNotNull(repositoryId);
        logger.debug("Execute for repositoryId: {}", repositoryId);
        repositoryStatisticService.updateStatistics(repositoryId);
        return null;
    }
}
