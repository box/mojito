package com.box.l10n.mojito.service.repository;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocaleStatistic;
import com.box.l10n.mojito.entity.RepositoryStatistic;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.repository.statistics.RepositoryLocaleStatisticRepository;
import com.box.l10n.mojito.service.repository.statistics.RepositoryStatisticRepository;
import com.box.l10n.mojito.service.repository.statistics.RepositoryStatisticService;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTestData;
import com.box.l10n.mojito.test.TestIdWatcher;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author jaurambault
 */
public class RepositoryStatisticServiceTest extends ServiceTestBase {

    /**
     * logger
     */
    static Logger logger = getLogger(RepositoryStatisticServiceTest.class);

    @Autowired
    RepositoryStatisticService repositoryStatisticService;

    @Autowired
    RepositoryRepository repositoryRepository;

    @Autowired
    TMService tmService;

    @Autowired
    RepositoryStatisticRepository repositoryStatisticRepository;

    @Autowired
    RepositoryLocaleStatisticRepository repositoryLocaleStatisticRepository;

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    @Test
    public void testUpdateStatistics() throws Exception {

        TMTestData tmTestData = new TMTestData(testIdWatcher);

        logger.debug("First run: compute and save statistics");
        repositoryStatisticService.updateStatistics(tmTestData.repository.getId());

        Repository repository = repositoryRepository.findOne(tmTestData.repository.getId());
        RepositoryStatistic repositoryStatistic = repositoryStatisticRepository.findOne(repository.getRepositoryStatistic().getId());
        Map<String, RepositoryLocaleStatistic> repositoryLocaleStatistics = new HashMap<>();
        for (RepositoryLocaleStatistic repositoryLocaleStatistic : repositoryLocaleStatisticRepository.findByRepositoryStatisticId(repositoryStatistic.getId())) {
            repositoryLocaleStatistics.put(repositoryLocaleStatistic.getLocale().getBcp47Tag(), repositoryLocaleStatistic);
        }

        checkTextUnitCounts(repositoryStatistic);

        int i = 0;
        checkRepositoryLocaleStatistic(repositoryLocaleStatistics.get("fr-FR"), "fr-FR", 1, 8, 1, 8, 0, 0, 0, 0);
        checkRepositoryLocaleStatistic(repositoryLocaleStatistics.get("fr-CA"), "fr-CA", 1, 1, 1, 1, 0, 0, 0, 0);
        checkRepositoryLocaleStatistic(repositoryLocaleStatistics.get("ja-JP"), "ja-JP", 0, 0, 0, 0, 0, 0, 0, 0);
        checkRepositoryLocaleStatistic(repositoryLocaleStatistics.get("ko-KR"), "ko-KR", 1, 8, 1, 8, 0, 0, 0, 0);
    }

    @Test
    public void testComputeBaseStatistics() throws Exception {

        TMTestData tmTestData = new TMTestData(testIdWatcher);

        RepositoryStatistic repositoryStatistic = repositoryStatisticService.computeBaseStatistics(tmTestData.repository.getId());

        checkTextUnitCounts(repositoryStatistic);
    }

    private void checkTextUnitCounts(RepositoryStatistic repositoryStatistic) {
        assertEquals(2L, (long) repositoryStatistic.getUsedTextUnitCount());
        assertEquals(9L, (long) repositoryStatistic.getUsedTextUnitWordCount());
        assertEquals(1L, (long) repositoryStatistic.getUnusedTextUnitCount());
        assertEquals(1L, (long) repositoryStatistic.getUnusedTextUnitWordCount());
    }

    @Test
    public void testComputeLocaleStatistics() throws Exception {

        TMTestData tmTestData = new TMTestData(testIdWatcher);

        logger.debug("Mark one translated string as not included and needs review");

        tmService.addTMTextUnitCurrentVariant(
                tmTestData.addCurrentTMTextUnitVariant1FrFR.getTmTextUnit().getId(),
                tmTestData.frFR.getId(),
                tmTestData.addCurrentTMTextUnitVariant1FrFR.getContent(),
                "this translation fails compilation",
                TMTextUnitVariant.Status.REVIEW_NEEDED,
                false);

        tmService.addTMTextUnitCurrentVariant(
                tmTestData.addCurrentTMTextUnitVariant1KoKR.getTmTextUnit().getId(),
                tmTestData.koKR.getId(),
                tmTestData.addCurrentTMTextUnitVariant1KoKR.getContent(),
                "this translation fails compilation",
                TMTextUnitVariant.Status.TRANSLATION_NEEDED,
                true);

        RepositoryLocaleStatistic repositoryLocaleStatisticFrFR = repositoryStatisticService.computeLocaleStatistics(tmTestData.repoLocaleFrFR.getId());
        RepositoryLocaleStatistic repositoryLocaleStatisticKoKR = repositoryStatisticService.computeLocaleStatistics(tmTestData.repoLocaleKoKR.getId());

        checkRepositoryLocaleStatistic(repositoryLocaleStatisticFrFR, "fr-FR", 1, 8, 0, 0, 1, 8, 0, 0);
        checkRepositoryLocaleStatistic(repositoryLocaleStatisticKoKR, "ko-KR", 1, 8, 1, 8, 0, 0, 1, 8);
    }

    private void checkRepositoryLocaleStatistic(
            RepositoryLocaleStatistic repositoryLocaleStatistic,
            String expectedBcp47tag,
            long expectedTranslatedCount,
            long expectedTranslatedWordCount,
            long expectedIncludeInFileCount,
            long expectedIncludeInFileWordCount,
            long reviewNeededCount,
            long reviewNeededWordCount,
            long expectedTranslationNeededCount,
            long expectedTranslationNeededWordCount) {

        assertEquals(expectedBcp47tag, repositoryLocaleStatistic.getLocale().getBcp47Tag());
        assertEquals(expectedTranslatedCount, (long) repositoryLocaleStatistic.getTranslatedCount());
        assertEquals(expectedTranslatedWordCount, (long) repositoryLocaleStatistic.getTranslatedWordCount());
        assertEquals(expectedIncludeInFileCount, (long) repositoryLocaleStatistic.getIncludeInFileCount());
        assertEquals(expectedIncludeInFileWordCount, (long) repositoryLocaleStatistic.getIncludeInFileWordCount());
        assertEquals(reviewNeededCount, (long) repositoryLocaleStatistic.getReviewNeededCount());
        assertEquals(reviewNeededWordCount, (long) repositoryLocaleStatistic.getReviewNeededWordCount());
        assertEquals(expectedTranslationNeededCount, (long) repositoryLocaleStatistic.getTranslationNeededCount());
        assertEquals(expectedTranslationNeededWordCount, (long) repositoryLocaleStatistic.getTranslationNeededWordCount());
    }

}
