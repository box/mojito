package com.box.l10n.mojito.service.repository.statistics;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.RepositoryLocaleStatistic;
import com.box.l10n.mojito.entity.RepositoryStatistic;
import com.box.l10n.mojito.service.locale.LocaleRepository;
import com.box.l10n.mojito.service.repository.RepositoryLocaleRepository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.repository.RepositoryService;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to compute and update {@link Repository} statistics
 *
 * @author jaurambault
 */
@Service
public class RepositoryStatisticService {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(RepositoryStatisticService.class);

    @Autowired
    RepositoryRepository repositoryRepository;

    @Autowired
    RepositoryStatisticRepository repositoryStatisticRepository;

    @Autowired
    RepositoryLocaleStatisticRepository repositoryLocaleStatisticRepository;

    @Autowired
    LocaleRepository localeRepository;

    @Autowired
    RepositoryLocaleRepository repositoryLocaleRepository;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    EntityManager entityManager;

    /**
     * Updates the {@link RepositoryStatistic} of a given repository.
     *
     * @param repositoryId {@link Repository#id}
     */
    @Transactional
    public void updateStatistics(Long repositoryId) {

        Repository repository = repositoryRepository.findOne(repositoryId);

        logger.debug("Get the current repository statistics");
        RepositoryStatistic repositoryStatistic = repository.getRepositoryStatistic();

        if (repositoryStatistic == null) {
            logger.debug("No current repository statistics (shouldn't happen if repository was created via the service), Create it.");
            repositoryStatistic = new RepositoryStatistic();
        }

        logger.debug("Update current entity with new statitsitcs");
        RepositoryStatistic newRepositoryStatistics = computeBaseStatistics(repositoryId);

        repositoryStatistic.setUsedTextUnitCount(newRepositoryStatistics.getUsedTextUnitCount());
        repositoryStatistic.setUsedTextUnitWordCount(newRepositoryStatistics.getUsedTextUnitWordCount());
        repositoryStatistic.setUnusedTextUnitCount(newRepositoryStatistics.getUnusedTextUnitCount());
        repositoryStatistic.setUsedTextUnitWordCount(newRepositoryStatistics.getUnusedTextUnitWordCount());
        //TODO(P1) This should be updated by spring but it's not, needs review
        repositoryStatistic.setLastModifiedDate(DateTime.now());

        repositoryStatisticRepository.save(repositoryStatistic);

        logger.debug("Update locale statistics");
        for (RepositoryLocale repositoryLocale : repositoryService.getRepositoryLocalesWithoutRootLocale(repository)) {
            updateLocaleStatistics(repositoryLocale, repositoryStatistic);
        }
    }

    /**
     * Updates the {@link RepositoryLocaleStatistic} for a given locale and
     * repository.
     *
     * @param repositoryLocale the repository locale
     * @param repositoryStatistic the parent entity that old the repository
     * statistics
     */
    void updateLocaleStatistics(RepositoryLocale repositoryLocale, RepositoryStatistic repositoryStatistic) {

        logger.debug("Get current statistics for locale: {}", repositoryLocale.getLocale().getBcp47Tag());
        RepositoryLocaleStatistic repositoryLocaleStatistic = repositoryLocaleStatisticRepository.findByRepositoryStatisticIdAndLocaleId(repositoryStatistic.getId(), repositoryLocale.getLocale().getId());

        if (repositoryLocaleStatistic == null) {
            logger.debug("No stats yet, create entity for that locale");
            repositoryLocaleStatistic = new RepositoryLocaleStatistic();
            repositoryLocaleStatistic.setRepositoryStatistic(repositoryStatistic);
            repositoryLocaleStatistic.setLocale(repositoryLocale.getLocale());
        }

        logger.debug("Compute new statistics for locale: {}", repositoryLocale.getLocale().getBcp47Tag());
        RepositoryLocaleStatistic newRepositoryLocaleStatistic = computeLocaleStatistics(repositoryLocale.getId());

        repositoryLocaleStatistic.setIncludeInFileCount(newRepositoryLocaleStatistic.getIncludeInFileCount());
        repositoryLocaleStatistic.setReviewNeededCount(newRepositoryLocaleStatistic.getReviewNeededCount());
        repositoryLocaleStatistic.setTranslatedCount(newRepositoryLocaleStatistic.getTranslatedCount());
        repositoryLocaleStatistic.setTranslationNeededCount(newRepositoryLocaleStatistic.getTranslationNeededCount());

        repositoryLocaleStatisticRepository.save(repositoryLocaleStatistic);
    }

    /**
     * Computes base statistics (used/unused text unit count, word count, ...)
     * for a repository.
     *
     * @param repositoryId {@link Repository#id}
     * @return the base statistic of the repository
     */
    public RepositoryStatistic computeBaseStatistics(Long repositoryId) {

        logger.debug("Compute text unit counts for repository id: {}", repositoryId);

        Query createNativeQuery = entityManager.createNamedQuery("RepositoryStatistic.computeBaseStatistics");
        createNativeQuery.setParameter(1, repositoryId);

        return (RepositoryStatistic) createNativeQuery.getSingleResult();
    }

    /**
     * Computes the locale statistics for a repository and a locale.
     *
     * @param repositoryLocaleId {@link RepositoryLocale#id}
     * @return the statistics of the repository locale
     */
    public RepositoryLocaleStatistic computeLocaleStatistics(Long repositoryLocaleId) {

        logger.debug("Compute locale statistic for repositoryLocale id: {}", repositoryLocaleId);

        RepositoryLocale repositoryLocale = repositoryLocaleRepository.findOne(repositoryLocaleId);

        Query createNativeQuery = entityManager.createNamedQuery("RepositoryLocaleStatistic.computeLocaleStatistics");
        createNativeQuery.setParameter(1, repositoryLocaleId);

        RepositoryLocaleStatistic repositoryLocaleStatistic = (RepositoryLocaleStatistic) createNativeQuery.getSingleResult();

        logger.debug("Replace POJO with a reference object");
        repositoryLocaleStatistic.setLocale(repositoryLocale.getLocale());

        return repositoryLocaleStatistic;
    }

}
