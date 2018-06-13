package com.box.l10n.mojito.service.repository.statistics;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.RepositoryLocaleStatistic;
import com.box.l10n.mojito.entity.RepositoryStatistic;
import com.box.l10n.mojito.service.locale.LocaleRepository;
import com.box.l10n.mojito.service.repository.RepositoryLocaleRepository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.sla.DropScheduleService;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitAndWordCount;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.ULocale;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired
    TextUnitSearcher textUnitSearcher;

    @Autowired
    DropScheduleService dropScheduleService;

    @Value("${l10n.repositoryStatistics.computeOutOfSla:false}")
    boolean computeOutOfSla;

    /**
     * Updates the {@link RepositoryStatistic} of a given repository.
     *
     * @param repositoryId {@link Repository#id}
     */
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
        repositoryStatistic.setUnusedTextUnitWordCount(newRepositoryStatistics.getUnusedTextUnitWordCount());
        repositoryStatistic.setPluralTextUnitCount(newRepositoryStatistics.getPluralTextUnitCount());
        repositoryStatistic.setPluralTextUnitWordCount(newRepositoryStatistics.getPluralTextUnitWordCount());
        repositoryStatistic.setOoslaCreatedBefore(newRepositoryStatistics.getOoslaCreatedBefore());
        repositoryStatistic.setOoslaTextUnitCount(newRepositoryStatistics.getOoslaTextUnitCount());
        repositoryStatistic.setOoslaTextUnitWordCount(newRepositoryStatistics.getOoslaTextUnitWordCount());

        //TODO(P1) This should be updated by spring but it's not, needs review
        repositoryStatistic.setLastModifiedDate(DateTime.now());

        repositoryStatisticRepository.save(repositoryStatistic);

        logger.debug("Update locale statistics");
        for (RepositoryLocale repositoryLocale : repositoryService.getRepositoryLocalesWithoutRootLocale(repository)) {
            updateLocaleStatistics(repositoryLocale, repositoryStatistic);
        }

        logger.debug("Stats updated");
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
        RepositoryLocaleStatistic newRepositoryLocaleStatistic = computeLocaleStatistics(repositoryLocale);

        repositoryLocaleStatistic.setIncludeInFileCount(newRepositoryLocaleStatistic.getIncludeInFileCount());
        repositoryLocaleStatistic.setIncludeInFileWordCount(newRepositoryLocaleStatistic.getIncludeInFileWordCount());
        repositoryLocaleStatistic.setReviewNeededCount(newRepositoryLocaleStatistic.getReviewNeededCount());
        repositoryLocaleStatistic.setReviewNeededWordCount(newRepositoryLocaleStatistic.getReviewNeededWordCount());
        repositoryLocaleStatistic.setTranslatedCount(newRepositoryLocaleStatistic.getTranslatedCount());
        repositoryLocaleStatistic.setTranslatedWordCount(newRepositoryLocaleStatistic.getTranslatedWordCount());
        repositoryLocaleStatistic.setTranslationNeededCount(newRepositoryLocaleStatistic.getTranslationNeededCount());
        repositoryLocaleStatistic.setTranslationNeededWordCount(newRepositoryLocaleStatistic.getTranslationNeededWordCount());
        repositoryLocaleStatistic.setForTranslationCount(newRepositoryLocaleStatistic.getForTranslationCount());
        repositoryLocaleStatistic.setForTranslationWordCount(newRepositoryLocaleStatistic.getForTranslationWordCount());
        repositoryLocaleStatistic.setDiffToSourcePluralCount(newRepositoryLocaleStatistic.getDiffToSourcePluralCount());

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

        RepositoryStatistic repositoryStatistic = (RepositoryStatistic) createNativeQuery.getSingleResult();
        updateRepositoryStatisticWithOutOfSla(repositoryId, repositoryStatistic);

        return repositoryStatistic;
    }

    /**
     * Computes the locale statistics for a repository and a locale.
     *
     * @param repositoryLocale
     * @return the statistics of the repository locale
     */
    public RepositoryLocaleStatistic computeLocaleStatistics(RepositoryLocale repositoryLocale) {

        logger.debug("Compute locale statistic for repositoryLocale id: {}", repositoryLocale.getId());

        Query createNativeQuery = entityManager.createNamedQuery("RepositoryLocaleStatistic.computeLocaleStatistics");
        createNativeQuery.setParameter(1, repositoryLocale.getId());

        RepositoryLocaleStatistic repositoryLocaleStatistic = (RepositoryLocaleStatistic) createNativeQuery.getSingleResult();

        logger.debug("Replace POJO with a reference object");
        repositoryLocaleStatistic.setLocale(repositoryLocale.getLocale());
        repositoryLocaleStatistic.setDiffToSourcePluralCount(computeDiffToSourceLocaleCount(repositoryLocale.getLocale().getBcp47Tag()));

        TextUnitAndWordCount countTextUnitAndWordCount = getForTranslationStatistics(repositoryLocale);
        repositoryLocaleStatistic.setForTranslationCount(countTextUnitAndWordCount.getTextUnitCount());
        repositoryLocaleStatistic.setForTranslationWordCount(countTextUnitAndWordCount.getTextUnitWordCount());

        return repositoryLocaleStatistic;
    }

    TextUnitAndWordCount getForTranslationStatistics(RepositoryLocale repositoryLocale) {
        logger.debug("Compute \"for translation\" statistics using textunitsearcher");
        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repositoryLocale.getRepository().getId());
        textUnitSearcherParameters.setStatusFilter(StatusFilter.FOR_TRANSLATION);
        textUnitSearcherParameters.setLocaleId(repositoryLocale.getLocale().getId());
        textUnitSearcherParameters.setUsedFilter(UsedFilter.USED);
        textUnitSearcherParameters.setDoNotTranslateFilter(false);
        TextUnitAndWordCount countTextUnitAndWordCount = textUnitSearcher.countTextUnitAndWordCount(textUnitSearcherParameters);
        return countTextUnitAndWordCount;
    }

    TextUnitAndWordCount getUntranslatedTextUnitsCountBeforeCreatedDate(Long repositoryId, DateTime createdBefore) {
        logger.debug("Get untranslated text unit for repository id: {} and before date: {}", repositoryId, createdBefore);
        TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
        textUnitSearcherParameters.setRepositoryIds(repositoryId);
        textUnitSearcherParameters.setStatusFilter(StatusFilter.UNTRANSLATED);
        textUnitSearcherParameters.setUsedFilter(UsedFilter.USED);
        textUnitSearcherParameters.setDoNotTranslateFilter(false);
        textUnitSearcherParameters.setTmTextUnitCreatedBefore(createdBefore);
        return textUnitSearcher.countTextUnitAndWordCount(textUnitSearcherParameters);
    }

    void updateRepositoryStatisticWithOutOfSla(Long repositoryId, RepositoryStatistic repositoryStatistic) {
        if (computeOutOfSla) {
            logger.debug("Update repository statistic with out of SLA statistics");
            DateTime lastDropCreatedDate = dropScheduleService.getLastDropCreatedDate();
            TextUnitAndWordCount countTextUnitAndWordCount = getUntranslatedTextUnitsCountBeforeCreatedDate(repositoryId, lastDropCreatedDate);
            repositoryStatistic.setOoslaTextUnitCount(countTextUnitAndWordCount.getTextUnitCount());
            repositoryStatistic.setOoslaTextUnitWordCount(countTextUnitAndWordCount.getTextUnitWordCount());
            repositoryStatistic.setOoslaCreatedBefore(lastDropCreatedDate);
            logger.debug("Number of out of sla text unit: {}", countTextUnitAndWordCount.getTextUnitCount());
        }
    }

    private Long computeDiffToSourceLocaleCount(String targetLocaleBcp47Tag) {
        ULocale targetLocale = ULocale.forLanguageTag(targetLocaleBcp47Tag);
        PluralRules pluralRulesTargetLocale = PluralRules.forLocale(targetLocale);
        return 6L - pluralRulesTargetLocale.getKeywords().size();
    }

}
