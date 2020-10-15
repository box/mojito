package com.box.l10n.mojito.service.repository.statistics;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.RepositoryLocaleStatistic;
import com.box.l10n.mojito.entity.RepositoryStatistic;
import com.box.l10n.mojito.service.WordCountService;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.assetExtraction.MultiBranchStateBlobStorage;
import com.box.l10n.mojito.service.branch.BranchStatisticService;
import com.box.l10n.mojito.service.locale.LocaleRepository;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.repository.RepositoryLocaleRepository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.service.sla.DropScheduleService;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitAndWordCount;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import com.box.l10n.mojito.service.tm.textunitdtocache.TextUnitDTOsCacheService;
import com.box.l10n.mojito.service.tm.textunitdtocache.UpdateType;
import com.google.common.collect.ImmutableList;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.ULocale;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Map;
import java.util.function.ToLongFunction;

import static com.box.l10n.mojito.utils.Predicates.not;

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
    TMTextUnitRepository tmTextUnitRepository;

    @Autowired
    DropScheduleService dropScheduleService;

    @Autowired
    BranchStatisticService branchStatisticService;

    @Autowired
    MultiBranchStateBlobStorage multiBranchStateBlobStorage;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    WordCountService wordCountService;

    @Autowired
    TextUnitDTOsCacheService textUnitDTOsCacheService;

    @Autowired
    LocaleService localeService;


    @Value("${l10n.repositoryStatistics.computeOutOfSla:false}")
    boolean computeOutOfSla;

    /**
     * Updates the {@link RepositoryStatistic} of a given repository.
     *
     * @param repositoryId {@link Repository#id}
     */
    public void updateStatistics(Long repositoryId) {

        Repository repository = repositoryRepository.findById(repositoryId).orElse(null);

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

        logger.debug("Update branch statistics");
        branchStatisticService.computeAndSaveBranchStatistics(repositoryId, UpdateType.NEVER);

        logger.debug("Stats updated");
    }

    /**
     * Updates the {@link RepositoryLocaleStatistic} for a given locale and
     * repository.
     *
     * @param repositoryLocale    the repository locale
     * @param repositoryStatistic the parent entity that old the repository
     *                            statistics
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

        logger.debug("computeBaseStatistics for repository id: {}", repositoryId);
        RepositoryStatistic repositoryStatistic = assetRepository.findIdByRepositoryIdAndDeleted(repositoryId, false).stream()
                .map(assetId -> {

                    ImmutableList<TextUnitDTO> textUnitDTOsForAssetAndLocale = textUnitDTOsCacheService.getTextUnitDTOsForAssetAndLocale(assetId, localeService.getDefaultLocale().getId(),  true, UpdateType.ALWAYS);

                    logger.debug("computeBaseStatistics, text unit dto size: {}", textUnitDTOsForAssetAndLocale.size());

                    RepositoryStatistic assetRepositoryStatistic = new RepositoryStatistic();

                    assetRepositoryStatistic.setUsedTextUnitCount(textUnitDTOsForAssetAndLocale.stream()
                            .filter(TextUnitDTO::isUsed)
                            .peek(t -> logger.trace("used: {}", t.getName()))
                            .count()
                    );
                    assetRepositoryStatistic.setUsedTextUnitWordCount(textUnitDTOsForAssetAndLocale.stream()
                            .filter(TextUnitDTO::isUsed)
                            .mapToLong(wordCountFunction())
                            .sum()
                    );

                    assetRepositoryStatistic.setUnusedTextUnitCount(textUnitDTOsForAssetAndLocale.stream()
                            .filter(not(TextUnitDTO::isUsed))
                            .peek(t -> logger.trace("unused: {}", t.getName()))
                            .count()
                    );
                    assetRepositoryStatistic.setUnusedTextUnitWordCount(textUnitDTOsForAssetAndLocale.stream()
                            .filter(not(TextUnitDTO::isUsed))
                            .mapToLong(wordCountFunction())
                            .sum()
                    );


                    assetRepositoryStatistic.setUncommentedTextUnitCount(textUnitDTOsForAssetAndLocale.stream()
                            .filter(t -> t.getComment() == null)
                            .peek(t -> logger.trace("uncommented: {}", t.getName()))
                            .count()
                    );

                    assetRepositoryStatistic.setPluralTextUnitCount(textUnitDTOsForAssetAndLocale.stream()
                            .filter(t-> t.getPluralForm() != null)
                            .peek(t -> logger.trace("plural: {}", t.getName()))
                            .count()
                    );
                    assetRepositoryStatistic.setPluralTextUnitWordCount(textUnitDTOsForAssetAndLocale.stream()
                            .filter(t-> t.getPluralForm() != null)
                            .mapToLong(wordCountFunction())
                            .sum()
                    );

                    return assetRepositoryStatistic;
                })
                .reduce(new RepositoryStatistic(), (r1, r2) -> {
                    RepositoryStatistic sum = new RepositoryStatistic();
                    sum.setUsedTextUnitCount(r1.getUsedTextUnitCount() + r2.getUsedTextUnitCount());
                    sum.setUsedTextUnitWordCount(r1.getUsedTextUnitWordCount() + r2.getUsedTextUnitWordCount());
                    sum.setUncommentedTextUnitCount(r1.getUncommentedTextUnitCount() + r2.getUncommentedTextUnitCount());
                    sum.setPluralTextUnitCount(r1.getPluralTextUnitCount() + r2.getPluralTextUnitCount());
                    sum.setPluralTextUnitWordCount(r1.getPluralTextUnitWordCount() + r2.getPluralTextUnitWordCount());
                    sum.setUnusedTextUnitCount(r1.getUnusedTextUnitCount() + r2.getUnusedTextUnitCount());
                    sum.setUnusedTextUnitWordCount(r1.getUnusedTextUnitWordCount() + r2.getUnusedTextUnitWordCount());
                    return sum;
                });

        // we don't re-implement OOSLA for now, thinking of removing that feature
        // updateRepositoryStatisticWithOutOfSla(repositoryId, repositoryStatistic);

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

        long repositoryId = repositoryLocale.getRepository().getId();

        RepositoryLocaleStatistic repositoryLocaleStatisticNew = assetRepository.findIdByRepositoryIdAndDeleted(repositoryId, false).stream()
                .map(assetId -> {

                    Map<String, TextUnitDTO> textUnitDTOsForLocaleByMD5 = textUnitDTOsCacheService.getTextUnitDTOsForAssetAndLocaleByMD5(assetId, repositoryLocale.getLocale().getId(), null, false, UpdateType.ALWAYS);

                    RepositoryLocaleStatistic repositoryLocaleStatistic = new RepositoryLocaleStatistic();

                    repositoryLocaleStatistic.setTranslatedCount(
                            textUnitDTOsForLocaleByMD5.values().stream()
                                    .filter(TextUnitDTO::isTranslated)
                                    .filter(TextUnitDTO::isUsed)
                                    .peek(t -> logger.debug("translated for {}: {}", t.getTargetLocale(), t.getName()))
                                    .count()
                    );

                    repositoryLocaleStatistic.setTranslatedWordCount(
                            textUnitDTOsForLocaleByMD5.values().stream()
                                    .filter(TextUnitDTO::isTranslated)
                                    .filter(TextUnitDTO::isUsed)
                                    .mapToLong(wordCountFunction())
                                    .sum()
                    );

                    repositoryLocaleStatistic.setTranslationNeededCount(
                            textUnitDTOsForLocaleByMD5.values().stream()
                                    .filter(TextUnitDTO::isUsed)
                                    .filter(textUnitDTOsCacheService.statusPredicate(StatusFilter.TRANSLATION_NEEDED))
                                    .peek(t -> logger.debug("translation needed for {}: {}", t.getTargetLocale(), t.getName()))
                                    .count()
                    );

                    repositoryLocaleStatistic.setTranslationNeededWordCount(
                            textUnitDTOsForLocaleByMD5.values().stream()
                                    .filter(TextUnitDTO::isUsed)
                                    .filter(textUnitDTOsCacheService.statusPredicate(StatusFilter.TRANSLATION_NEEDED))
                                    .mapToLong(wordCountFunction())
                                    .sum()
                    );

                    repositoryLocaleStatistic.setReviewNeededCount(
                            textUnitDTOsForLocaleByMD5.values().stream()
                                    .filter(TextUnitDTO::isUsed)
                                    .filter(textUnitDTOsCacheService.statusPredicate(StatusFilter.REVIEW_NEEDED))
                                    .peek(t -> logger.debug("review needed for {}: {}", t.getTargetLocale(), t.getName()))
                                    .count()
                    );

                    repositoryLocaleStatistic.setReviewNeededWordCount(
                            textUnitDTOsForLocaleByMD5.values().stream()
                                    .filter(TextUnitDTO::isUsed)
                                    .filter(textUnitDTOsCacheService.statusPredicate(StatusFilter.REVIEW_NEEDED))
                                    .mapToLong(wordCountFunction())
                                    .sum()
                    );

                    repositoryLocaleStatistic.setIncludeInFileCount(
                            textUnitDTOsForLocaleByMD5.values().stream()
                                    .filter(TextUnitDTO::isUsed)
                                    .filter(TextUnitDTO::isIncludedInLocalizedFile)
                                    .peek(t -> logger.debug("include in file for {}: {}", t.getTargetLocale(), t.getName()))
                                    .count()
                    );

                    repositoryLocaleStatistic.setIncludeInFileWordCount(
                            textUnitDTOsForLocaleByMD5.values().stream()
                                    .filter(TextUnitDTO::isUsed)
                                    .filter(TextUnitDTO::isIncludedInLocalizedFile)
                                    .mapToLong(wordCountFunction())
                                    .sum()
                    );

                    repositoryLocaleStatistic.setForTranslationCount(
                            textUnitDTOsForLocaleByMD5.values().stream()
                                    .filter(TextUnitDTO::isUsed)
                                    .filter(not(TextUnitDTO::isDoNotTranslate))
                                    .filter(textUnitDTOsCacheService.statusPredicate(StatusFilter.FOR_TRANSLATION))
                                    .peek(t -> logger.debug("for translation for {}: {}", t.getTargetLocale(), t.getName()))
                                    .count()
                    );

                    repositoryLocaleStatistic.setForTranslationWordCount(
                            textUnitDTOsForLocaleByMD5.values().stream()
                                    .filter(TextUnitDTO::isUsed)
                                    .filter(not(TextUnitDTO::isDoNotTranslate))
                                    .filter(textUnitDTOsCacheService.statusPredicate(StatusFilter.FOR_TRANSLATION))
                                    .mapToLong(wordCountFunction())
                                    .sum()
                    );

                    return repositoryLocaleStatistic;
                })
                .reduce(new RepositoryLocaleStatistic(), (r1, r2) -> {
                    RepositoryLocaleStatistic sum = new RepositoryLocaleStatistic();
                    sum.setTranslatedCount(r1.getTranslatedCount() + r2.getTranslatedCount());
                    sum.setTranslatedWordCount(r1.getTranslatedWordCount() + r2.getTranslatedWordCount());
                    sum.setTranslationNeededCount(r1.getTranslationNeededCount() + r2.getTranslationNeededCount());
                    sum.setTranslationNeededWordCount(r1.getTranslationNeededWordCount() + r2.getTranslationNeededWordCount());
                    sum.setReviewNeededCount(r1.getReviewNeededCount() + r2.getReviewNeededCount());
                    sum.setReviewNeededWordCount(r1.getReviewNeededWordCount() + r2.getReviewNeededWordCount());
                    sum.setIncludeInFileCount(r1.getIncludeInFileCount() + r2.getIncludeInFileCount());
                    sum.setIncludeInFileWordCount(r1.getIncludeInFileWordCount() + r2.getIncludeInFileWordCount());
                    sum.setForTranslationCount(r1.getForTranslationCount() + r2.getForTranslationCount());
                    sum.setForTranslationWordCount(r1.getForTranslationWordCount() + r2.getForTranslationWordCount());
                    return sum;
                });

        repositoryLocaleStatisticNew.setDiffToSourcePluralCount(computeDiffToSourceLocaleCount(repositoryLocale.getLocale().getBcp47Tag()));
        repositoryLocaleStatisticNew.setLocale(repositoryLocale.getLocale());
        return repositoryLocaleStatisticNew;
    }

    private ToLongFunction<TextUnitDTO> wordCountFunction() {
        return t -> Long.valueOf(wordCountService.getEnglishWordCount(t.getSource()));
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
        textUnitSearcherParameters.setToBeFullyTranslatedFilter(true);
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
