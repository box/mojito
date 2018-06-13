package com.box.l10n.mojito.service.repository.statistics;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryStatistic;
import com.box.l10n.mojito.entity.StatisticsSchedule;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import java.util.List;
import javax.annotation.PostConstruct;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

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

    @Autowired
    TaskScheduler taskScheduler;

    @Autowired
    RepositoryStatisticsUpdatedReactor repositoryStatisticsUpdatedReactor;

    @Value("${l10n.repositoryStatistics.scheduler.cron:}")
    String cron;

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

    private void updateRepositoryStatistics(Long repositoryId) {
        List<StatisticsSchedule> statisticsScheduleList = statisticsScheduleRepository.findByRepositoryIdAndTimeToUpdateBefore(repositoryId, DateTime.now());
        if (!statisticsScheduleList.isEmpty()) {
            StatisticsSchedule updateStatistics = statisticsScheduleList.get(0);
            logger.info("Translation stats outdated for repository: {}, re-compute", updateStatistics.getRepository().getName());
            repositoryStatisticService.updateStatistics(updateStatistics.getRepository().getId());
            statisticsScheduleRepository.delete(statisticsScheduleList);
        }
    }

    /**
     * This is required to re-compute OOSLA information. It will be also useful
     * when adding new statistics and have them recomputed automatically. Before
     * we'd to wait for a change in the repository.
     *
     * @throws InterruptedException
     */
    @PostConstruct
    public void registerTaskToUpdateRepositoryStatisticsOnARegularBasis() throws InterruptedException {
        if (!cron.isEmpty()) {
            taskScheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    logger.debug("Sets repository stats as out of date");
                    List<Repository> repositories = repositoryRepository.findByDeletedFalseOrderByNameAsc();
                    for (Repository repository : repositories) {
                        repositoryStatisticsUpdatedReactor.setRepositoryStatsOutOfDate(repository.getId());
                    }
                }
            }, new CronTrigger(cron));
        }
    }

}
