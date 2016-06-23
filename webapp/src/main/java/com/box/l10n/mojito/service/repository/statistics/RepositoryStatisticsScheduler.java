package com.box.l10n.mojito.service.repository.statistics;

import com.box.l10n.mojito.entity.RepositoryStatistic;
import com.box.l10n.mojito.entity.StatisticsSchedule;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import java.util.List;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Update {@link RepositoryStatistic}s on a regular basis.
 *
 * Using polling for now, should consider trigger this based on events.
 *
 * @author jaurambault
 */
@Profile("!disablescheduling")
@Component
public class RepositoryStatisticsScheduler {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(RepositoryStatisticsScheduler.class);

    @Autowired
    RepositoryRepository repositoryRepository;

    @Autowired
    RepositoryStatisticService repositoryStatisticService;

    @Autowired
    TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;

    @Autowired
    TMTextUnitVariantRepository tmTextUnitVariantRepository;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    StatisticsScheduleRepository statisticsScheduleRepository;

    /**
     * Every second, the scheduler looks into
     *
     * @{RepositoryStatisticsScheduler#repositoryStatsOutOfDate} and DB to find
     * the repository that needs statistics to be re-computed and re-compute
     * statistics.
     */
    @Scheduled(fixedDelay = 1000)
    public void updateStatisticsForAllReposiotries() {
        for (Long repositoryId : statisticsScheduleRepository.findRepositoryIds()) {
            updateRepositoryStatistics(repositoryId);
        }
    }

    @Transactional
    private void updateRepositoryStatistics(Long repositoryId) {
        List<StatisticsSchedule> statisticsScheduleList = statisticsScheduleRepository.findByRepositoryIdAndTimeToUpdateBefore(repositoryId, DateTime.now());
        if (!statisticsScheduleList.isEmpty()) {
            StatisticsSchedule updateStatistics = statisticsScheduleList.get(0);
            logger.info("Translation stats outdated for repository: {}, re-compute", updateStatistics.getRepository().getName());
            repositoryStatisticService.updateStatistics(updateStatistics.getRepository().getId());
            statisticsScheduleRepository.delete(statisticsScheduleList);
        }
    }

}
