package com.box.l10n.mojito.service.repository;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocaleStatistic;
import com.box.l10n.mojito.entity.RepositoryStatistic;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTestData;
import com.box.l10n.mojito.test.TestIdWatcher;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import static org.slf4j.LoggerFactory.getLogger;

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

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    @Test
    public void testUpdateStatistics() throws Exception {

        TMTestData tmTestData = new TMTestData(testIdWatcher);

        logger.debug("First run: compute and save statistics");
        repositoryStatisticService.updateStatistics(tmTestData.repository.getId());

        Repository repository = repositoryRepository.findOne(tmTestData.repository.getId());

        checkTextUnitCounts(repository.getRepositoryStatistic());

        ArrayList<RepositoryLocaleStatistic> repositoryLocaleStatistics = Lists.newArrayList(repository.getRepositoryStatistic().getRepositoryLocaleStatistics());

        int i = 0;
        checkRepositoryLocaleStatistic(repositoryLocaleStatistics.get(i++), "fr-FR", 1, 1, 0, 0);
        checkRepositoryLocaleStatistic(repositoryLocaleStatistics.get(i++), "fr-CA", 0, 0, 0, 0);
        checkRepositoryLocaleStatistic(repositoryLocaleStatistics.get(i++), "ja-JP", 0, 0, 0, 0);
        checkRepositoryLocaleStatistic(repositoryLocaleStatistics.get(i++), "ko-KR", 1, 1, 0, 0);
    }

    @Transactional
    @Test
    public void testComputeBaseStatistics() throws Exception {

        TMTestData tmTestData = new TMTestData(testIdWatcher);

        RepositoryStatistic repositoryStatistic = repositoryStatisticService.computeBaseStatistics(tmTestData.repository.getId());

        checkTextUnitCounts(repositoryStatistic);
    }

    private void checkTextUnitCounts(RepositoryStatistic repositoryStatistic) {
        assertEquals(2L, (long) repositoryStatistic.getUsedTextUnitCount());
        assertEquals(0L, (long) repositoryStatistic.getUsedTextUnitWordCount());
        assertEquals(1L, (long) repositoryStatistic.getUnusedTextUnitCount());
        assertEquals(0L, (long) repositoryStatistic.getUnusedTextUnitWordCount());
    }

    @Transactional
    @Test
    public void testComputeLocaleStatistics() throws Exception {

        TMTestData tmTestData = new TMTestData(testIdWatcher);

        logger.debug("Mark one translated string as not included and needs review");

        tmService.addTMTextUnitCurrentVariant(
                tmTestData.addCurrentTMTextUnitVariant1FrFR.getTmTextUnit().getId(),
                tmTestData.addCurrentTMTextUnitVariant1FrFR.getLocale().getId(),
                tmTestData.addCurrentTMTextUnitVariant1FrFR.getContent(),
                "this translation fails compilation",
                TMTextUnitVariant.Status.REVIEW_NEEDED,
                false);

        tmService.addTMTextUnitCurrentVariant(
                tmTestData.addCurrentTMTextUnitVariant1KoKR.getTmTextUnit().getId(),
                tmTestData.addCurrentTMTextUnitVariant1KoKR.getLocale().getId(),
                tmTestData.addCurrentTMTextUnitVariant1KoKR.getContent(),
                "this translation fails compilation",
                TMTextUnitVariant.Status.TRANSLATION_NEEDED,
                true);

        RepositoryLocaleStatistic repositoryLocaleStatisticFrFR = repositoryStatisticService.computeLocaleStatistics(tmTestData.repoLocaleFrFR.getId());
        RepositoryLocaleStatistic repositoryLocaleStatisticKoKR = repositoryStatisticService.computeLocaleStatistics(tmTestData.repoLocaleKoKR.getId());

        checkRepositoryLocaleStatistic(repositoryLocaleStatisticFrFR, "fr-FR", 1, 0, 1, 0);
        checkRepositoryLocaleStatistic(repositoryLocaleStatisticKoKR, "ko-KR", 1, 1, 0, 1);
    }

    private void checkRepositoryLocaleStatistic(
            RepositoryLocaleStatistic repositoryLocaleStatistic,
            String expectedBcp47tag,
            long expectedTranslatedCount,
            long expectedIncludeInFileCount,
            long reviewNeededCount,
            long expectedTranslationNeededCount) {

        assertEquals(expectedBcp47tag, repositoryLocaleStatistic.getLocale().getBcp47Tag());
        assertEquals(expectedTranslatedCount, (long) repositoryLocaleStatistic.getTranslatedCount());
        assertEquals(expectedIncludeInFileCount, (long) repositoryLocaleStatistic.getIncludeInFileCount());
        assertEquals(reviewNeededCount, (long) repositoryLocaleStatistic.getReviewNeededCount());
        assertEquals(expectedTranslationNeededCount, (long) repositoryLocaleStatistic.getTranslationNeededCount());
    }

}
