package com.box.l10n.mojito.service.repository;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryStatistic;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.service.asset.AssetRepository;
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

/**
 * Update {@link RepositoryStatistic}s on a regular basis.
 *
 * Using polling for now, should consider trigger this based on events.
 *
 * @author jaurambault
 */
//TODO(P0) this profile was actually removed! hence this is running on CI
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

    @Scheduled(fixedDelay = 1000)
    public void updateStatisticsForAllReposiotries() {
        logger.debug("Update statistics for all repositories");

        for (Repository repository : repositoryRepository.findByDeletedFalseOrderByNameAsc()) {

            if (isRepositoryStatisticsOutdated(repository)) {
                logger.debug("Translation stats outdated for repository: {}, re-compute", repository.getName());
                repositoryStatisticService.updateStatistics(repository.getId());
            }
        }
    }

    private boolean isRepositoryStatisticsOutdated(Repository repository) {

        boolean isOutDated;

        RepositoryStatistic repositoryStatistic = repository.getRepositoryStatistic();

        if (repositoryStatistic != null) {
            DateTime oldStatisticsDate = repository.getRepositoryStatistic().getLastModifiedDate();
            isOutDated = hasNewSuccessfulAssetExtraction(repository, oldStatisticsDate)
                    || hasNewTranslations(repository, oldStatisticsDate);
        } else {
            isOutDated = true;
        }

        return isOutDated;
    }

    private boolean hasNewSuccessfulAssetExtraction(Repository repository, DateTime oldStatisticsDate) {

        List<Asset> assets = assetRepository.findByRepositoryIdOrderByLastModifiedDateDesc(repository.getId());

        for (Asset asset : assets) {
            if (asset.getLastModifiedDate().isAfter(oldStatisticsDate)
                    || (asset.getLastSuccessfulAssetExtraction() != null 
                    && asset.getLastSuccessfulAssetExtraction().getLastModifiedDate().isAfter(oldStatisticsDate))) {
                return true;
            }
        }
        
        return false;
    }

    private boolean hasNewTranslations(Repository reposiotry, DateTime oldStatisticsDate) {

        DateTime lastTranslationDate = oldStatisticsDate;

        TMTextUnitVariant findLatestTMTextUnitVariant = tmTextUnitVariantRepository.findTopByTmTextUnitTmIdOrderByCreatedDateDesc(reposiotry.getTm().getId());

        if (findLatestTMTextUnitVariant != null) {
            lastTranslationDate = findLatestTMTextUnitVariant.getCreatedDate();
        }

        return oldStatisticsDate.isBefore(lastTranslationDate);
    }

}
