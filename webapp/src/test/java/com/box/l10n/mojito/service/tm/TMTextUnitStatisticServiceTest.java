package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitStatistic;
import com.box.l10n.mojito.rest.textunit.ImportTextUnitStatisticsBody;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.box.l10n.mojito.test.TestIdWatcher;
import org.joda.time.DateTime;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author garion
 */
public class TMTextUnitStatisticServiceTest extends ServiceTestBase {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(TMTextUnitStatisticServiceTest.class);

    @Autowired
    TMService tmService;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    AssetService assetService;

    @Autowired
    TMTextUnitStatisticRepository tmTextUnitStatisticRepository;

    @Autowired
    TMTextUnitStatisticService tmTextUnitStatisticService;

    @Rule
    public TestIdWatcher testIdWatcher = new TestIdWatcher();

    Asset asset;
    Long tmId;
    Long assetId;
    Long tmTextUnitId;
    String tmTextUnitName = "content --- comment";
    String tmTextContent = "content";
    String tmTextComment = "comment";
    Double lastDayEstimatedVolume = 3D;
    Double lastPeriodEstimatedVolume = 42D;
    DateTime lastSeenDate = new DateTime(2021, 11, 25, 0, 0);

    private void createTestTextUnitData(Repository repository) {
        logger.debug("Create data for test");

        asset = assetService.createAssetWithContent(repository.getId(), "test-asset-path.xliff", "test asset content");

        //make sure asset and its relationships are loaded
        asset = assetRepository.findById(asset.getId()).orElse(null);

        assetId = asset.getId();
        tmId = repository.getTm().getId();

        TMTextUnit addTMTextUnit = tmService.addTMTextUnit(tmId, assetId, tmTextUnitName, tmTextContent, tmTextComment);
        tmTextUnitId = addTMTextUnit.getId();
    }

    private ImportTextUnitStatisticsBody getImportTextUnitStatisticsBody() {
        ImportTextUnitStatisticsBody statistic = new ImportTextUnitStatisticsBody();
        statistic.setName(tmTextUnitName);
        statistic.setContent(tmTextContent);
        statistic.setComment(tmTextComment);
        statistic.setLastDayEstimatedVolume(lastDayEstimatedVolume);
        statistic.setLastPeriodEstimatedVolume(lastPeriodEstimatedVolume);
        statistic.setLastSeenDate(lastSeenDate);
        return statistic;
    }

    @Test
    public void importStatisticsByMD5Works() throws Exception {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        createTestTextUnitData(repository);

        List<ImportTextUnitStatisticsBody> textUnitStatistics = new ArrayList<>();
        ImportTextUnitStatisticsBody statistic = getImportTextUnitStatisticsBody();
        textUnitStatistics.add(statistic);

        tmTextUnitStatisticService.importStatistics(repository.getSourceLocale(), asset, textUnitStatistics).get();

        List<TMTextUnitStatistic> actualTMTextUnitStatistics = tmTextUnitStatisticRepository
                .findAll()
                .stream()
                .filter(ts -> Objects.equals(ts.getTMTextUnit().getAsset().getRepository().getId(), repository.getId()))
                .collect(Collectors.toList());

        assertNotNull(actualTMTextUnitStatistics);
        TMTextUnitStatistic actualStatistic = actualTMTextUnitStatistics.stream().findFirst().orElse(null);
        assertNotNull(actualStatistic);
        assertEquals(statistic.getLastDayEstimatedVolume(), (Double) actualStatistic.getLastDayUsageCount());
        assertEquals(statistic.getLastPeriodEstimatedVolume(), (Double) actualStatistic.getLastPeriodUsageCount());
        assertEquals(statistic.getLastSeenDate(), actualStatistic.getLastSeenDate());
    }

    @Test
    public void importDuplicateStatisticsWorks() throws Exception {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        createTestTextUnitData(repository);

        List<ImportTextUnitStatisticsBody> textUnitStatistics = new ArrayList<>();
        ImportTextUnitStatisticsBody statistic = getImportTextUnitStatisticsBody();
        textUnitStatistics.add(statistic);
        textUnitStatistics.add(statistic);

        tmTextUnitStatisticService.importStatistics(repository.getSourceLocale(), asset, textUnitStatistics).get();

        List<TMTextUnitStatistic> actualTMTextUnitStatistics = tmTextUnitStatisticRepository
                .findAll()
                .stream()
                .filter(ts -> Objects.equals(ts.getTMTextUnit().getAsset().getRepository().getId(), repository.getId()))
                .collect(Collectors.toList());
        assertNotNull(actualTMTextUnitStatistics);
        assertEquals(1, actualTMTextUnitStatistics.size());
        TMTextUnitStatistic actualStatistic = actualTMTextUnitStatistics.stream().findFirst().orElse(null);
        assertNotNull(actualStatistic);
        assertEquals(statistic.getLastDayEstimatedVolume(), (Double) actualStatistic.getLastDayUsageCount());
        assertEquals(statistic.getLastPeriodEstimatedVolume(), (Double) actualStatistic.getLastPeriodUsageCount());
        assertEquals(statistic.getLastSeenDate(), actualStatistic.getLastSeenDate());
    }

    @Test
    public void importPartialStatisticForTextUnitsWithSameName() throws Exception {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        asset = assetService.createAssetWithContent(repository.getId(), "test-asset-path.xliff", "test asset content");
        asset = assetRepository.findById(asset.getId()).orElse(null);

        assetId = asset.getId();
        tmId = repository.getTm().getId();

        tmService.addTMTextUnit(tmId, assetId, tmTextUnitName, "content1", "comment1");
        tmService.addTMTextUnit(tmId, assetId, tmTextUnitName, "content2", "comment2");

        List<ImportTextUnitStatisticsBody> textUnitStatistics = new ArrayList<>();
        ImportTextUnitStatisticsBody statistic = getImportTextUnitStatisticsBody();
        statistic.setContent(null);
        statistic.setComment(null);
        textUnitStatistics.add(statistic);

        tmTextUnitStatisticService.importStatistics(repository.getSourceLocale(), asset, textUnitStatistics).get();

        List<TMTextUnitStatistic> actualTMTextUnitStatistics = tmTextUnitStatisticRepository
                .findAll()
                .stream()
                .filter(ts -> Objects.equals(ts.getTMTextUnit().getAsset().getRepository().getId(), repository.getId()))
                .collect(Collectors.toList());
        assertNotNull(actualTMTextUnitStatistics);
        assertEquals(2, actualTMTextUnitStatistics.size());

        for (TMTextUnitStatistic actualStatistic: actualTMTextUnitStatistics) {
            assertNotNull(actualStatistic);
            assertEquals(statistic.getLastDayEstimatedVolume(), (Double) actualStatistic.getLastDayUsageCount());
            assertEquals(statistic.getLastPeriodEstimatedVolume(), (Double) actualStatistic.getLastPeriodUsageCount());
            assertEquals(statistic.getLastSeenDate(), actualStatistic.getLastSeenDate());
        }
    }

    @Test
    public void importStatisticsMissingCommentWorks() throws Exception {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        createTestTextUnitData(repository);

        List<ImportTextUnitStatisticsBody> textUnitStatistics = new ArrayList<>();
        ImportTextUnitStatisticsBody statistic = getImportTextUnitStatisticsBody();
        statistic.setComment(null);
        textUnitStatistics.add(statistic);

        tmTextUnitStatisticService.importStatistics(repository.getSourceLocale(), asset, textUnitStatistics).get();

        List<TMTextUnitStatistic> actualTMTextUnitStatistics = tmTextUnitStatisticRepository
                .findAll()
                .stream()
                .filter(ts -> Objects.equals(ts.getTMTextUnit().getAsset().getRepository().getId(), repository.getId()))
                .collect(Collectors.toList());
        assertNotNull(actualTMTextUnitStatistics);
        assertEquals(1, actualTMTextUnitStatistics.size());
        TMTextUnitStatistic actualStatistic = actualTMTextUnitStatistics.stream().findFirst().orElse(null);
        assertNotNull(actualStatistic);
        assertEquals(statistic.getLastDayEstimatedVolume(), (Double) actualStatistic.getLastDayUsageCount());
        assertEquals(statistic.getLastPeriodEstimatedVolume(), (Double) actualStatistic.getLastPeriodUsageCount());
        assertEquals(statistic.getLastSeenDate(), actualStatistic.getLastSeenDate());
    }

    @Test
    public void importStatisticsMissingContentWorks() throws Exception {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        createTestTextUnitData(repository);

        List<ImportTextUnitStatisticsBody> textUnitStatistics = new ArrayList<>();
        ImportTextUnitStatisticsBody statistic = getImportTextUnitStatisticsBody();
        statistic.setContent(null);
        textUnitStatistics.add(statistic);

        tmTextUnitStatisticService.importStatistics(repository.getSourceLocale(), asset, textUnitStatistics).get();

        List<TMTextUnitStatistic> actualTMTextUnitStatistics = tmTextUnitStatisticRepository
                .findAll()
                .stream()
                .filter(ts -> Objects.equals(ts.getTMTextUnit().getAsset().getRepository().getId(), repository.getId()))
                .collect(Collectors.toList());
        assertNotNull(actualTMTextUnitStatistics);
        assertEquals(1, actualTMTextUnitStatistics.size());
        TMTextUnitStatistic actualStatistic = actualTMTextUnitStatistics.stream().findFirst().orElse(null);
        assertNotNull(actualStatistic);
        assertEquals(statistic.getLastDayEstimatedVolume(), (Double) actualStatistic.getLastDayUsageCount());
        assertEquals(statistic.getLastPeriodEstimatedVolume(), (Double) actualStatistic.getLastPeriodUsageCount());
        assertEquals(statistic.getLastSeenDate(), actualStatistic.getLastSeenDate());
    }

    @Test
    public void importStatisticsMismatchedNameFails() throws Exception {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        createTestTextUnitData(repository);

        List<ImportTextUnitStatisticsBody> textUnitStatistics = new ArrayList<>();
        ImportTextUnitStatisticsBody statistic = getImportTextUnitStatisticsBody();
        statistic.setName("mismatched_name");
        textUnitStatistics.add(statistic);

        tmTextUnitStatisticService.importStatistics(repository.getSourceLocale(), asset, textUnitStatistics).get();

        List<TMTextUnitStatistic> actualTMTextUnitStatistics = tmTextUnitStatisticRepository
                .findAll()
                .stream()
                .filter(ts -> Objects.equals(ts.getTMTextUnit().getAsset().getRepository().getId(), repository.getId()))
                .collect(Collectors.toList());
        assertEquals(0, actualTMTextUnitStatistics.size());
    }

    @Test
    public void importStatisticsMismatchedContentFails() throws Exception {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        createTestTextUnitData(repository);

        List<ImportTextUnitStatisticsBody> textUnitStatistics = new ArrayList<>();
        ImportTextUnitStatisticsBody statistic = getImportTextUnitStatisticsBody();
        statistic.setContent("mismatched_content");
        textUnitStatistics.add(statistic);

        tmTextUnitStatisticService.importStatistics(repository.getSourceLocale(), asset, textUnitStatistics).get();

        List<TMTextUnitStatistic> actualTMTextUnitStatistics = tmTextUnitStatisticRepository
                .findAll()
                .stream()
                .filter(ts -> Objects.equals(ts.getTMTextUnit().getAsset().getRepository().getId(), repository.getId()))
                .collect(Collectors.toList());
        assertEquals(0, actualTMTextUnitStatistics.size());
    }

    @Test
    public void importStatisticsMismatchedCommentFails() throws Exception {
        Repository repository = repositoryService.createRepository(testIdWatcher.getEntityName("repository"));
        createTestTextUnitData(repository);

        List<ImportTextUnitStatisticsBody> textUnitStatistics = new ArrayList<>();
        ImportTextUnitStatisticsBody statistic = getImportTextUnitStatisticsBody();
        statistic.setComment("mismatched_comment");
        textUnitStatistics.add(statistic);

        tmTextUnitStatisticService.importStatistics(repository.getSourceLocale(), asset, textUnitStatistics).get();

        List<TMTextUnitStatistic> actualTMTextUnitStatistics = tmTextUnitStatisticRepository
                .findAll()
                .stream()
                .filter(ts -> Objects.equals(ts.getTMTextUnit().getAsset().getRepository().getId(), repository.getId()))
                .collect(Collectors.toList());
        assertEquals(0, actualTMTextUnitStatistics.size());
    }
}
