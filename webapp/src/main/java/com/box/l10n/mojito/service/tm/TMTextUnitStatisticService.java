package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.collect.ImmutableMapCollectors;
import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.BaseEntity;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitStatistic;
import com.box.l10n.mojito.okapi.TextUnitUtils;
import com.box.l10n.mojito.rest.textunit.ImportTextUnitStatisticsBody;
import com.box.l10n.mojito.service.pollableTask.Pollable;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableFutureTaskResult;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.textunitdtocache.TextUnitDTOsCacheService;
import com.box.l10n.mojito.service.tm.textunitdtocache.UpdateType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author garion
 */
@Service
public class TMTextUnitStatisticService {
    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(TMTextUnitStatisticService.class);

    @Autowired
    MeterRegistry meterRegistry;

    @Autowired
    TextUnitBatchMatcher textUnitBatchMatcher;

    @Autowired
    TextUnitDTOsCacheService textUnitDTOsCacheService;

    @Autowired
    TMTextUnitRepository tmTextUnitRepository;

    @Autowired
    TMTextUnitStatisticRepository tmTextUnitStatisticRepository;

    @Autowired
    TextUnitUtils textUnitUtils;

    int batchSize = 5000;

    /**
     * Imports statistics information for a target text unit by matching it using name, content and comment information.
     * Existing statistic data is overwritten by incoming data.
     *
     * @param locale                the locale of the target text unit
     * @param asset                 the asset of the target text unit
     * @param textUnitStatistics    the statistics of the text unit
     * @return                      a PollableFuture
     */
    @Pollable(async = true, message = "Start importing text unit statistics.")
    public PollableFuture importStatistics(
            Locale locale,
            Asset asset,
            List<ImportTextUnitStatisticsBody> textUnitStatistics) {
        logger.debug("Import statistics for the text units for the given locale and asset.");

        ImmutableMap<String, TextUnitDTO> textUnitDTOsByMD5 = textUnitDTOsCacheService.getTextUnitDTOsForAssetAndLocaleByMD5(
                asset.getId(),
                locale.getId(),
                StatusFilter.ALL,
                true,
                UpdateType.ALWAYS);

        ImmutableMap<String, List<TextUnitDTO>> textUnitDTOsByName = textUnitDTOsByMD5.values().stream()
                .collect(Collectors.collectingAndThen(Collectors.groupingBy(TextUnitDTO::getName), ImmutableMap::copyOf));

        ImmutableMap<ImportTextUnitStatisticsBody, ImmutableList<TextUnitDTO>> statisticToTextUnitDTOMap = textUnitStatistics.stream()
                .map(textUnitStatistic -> new AbstractMap.SimpleEntry<>(
                        textUnitStatistic, getMatchingTextUnitDTOs(textUnitStatistic, textUnitDTOsByMD5, textUnitDTOsByName, asset)
                ))
                .filter(entry -> entry.getValue() != null)
                .collect(ImmutableMapCollectors.mapEntriesToImmutableMap(
                        (textUnitDTOsFirst, textUnitDTOsLast) -> textUnitDTOsLast)
                );

        ImmutableList<Long> textUnitIds = statisticToTextUnitDTOMap.values().stream()
                .flatMap(Collection::stream)
                .map(TextUnitDTO::getTmTextUnitId)
                .collect(ImmutableList.toImmutableList());

        Map<ImportTextUnitStatisticsBody, List<TMTextUnit>> statisticsToTextUnitsMap = getStatisticsToTextUnitMap(statisticToTextUnitDTOMap, textUnitIds);
        uploadStatistics(statisticsToTextUnitsMap);

        return new PollableFutureTaskResult();
    }

    private ImmutableList<TextUnitDTO> getMatchingTextUnitDTOs(ImportTextUnitStatisticsBody textUnitStatistic, ImmutableMap<String, TextUnitDTO> textUnitDTOsByMD5, ImmutableMap<String, List<TextUnitDTO>> textUnitDTOsByName, Asset logAsset) {
        String statisticTextUnitMD5 = getTextUnitMd5(textUnitStatistic);
        TextUnitDTO foundTextUnitDTOByMD5 = textUnitDTOsByMD5.get(statisticTextUnitMD5);

        if (foundTextUnitDTOByMD5 != null) {
            return ImmutableList.of(foundTextUnitDTOByMD5);
        }

        // Only attempt a fallback match by name if the content or comment aren't available in the statistic object.
        if (StringUtils.isEmpty(textUnitStatistic.getContent()) || StringUtils.isEmpty(textUnitStatistic.getComment())) {
            List<TextUnitDTO> foundTextUnitDTOsByNameAndContent = textUnitDTOsByName.get(textUnitStatistic.getName());

            if (foundTextUnitDTOsByNameAndContent != null && foundTextUnitDTOsByNameAndContent.size() > 0) {
                return ImmutableList.copyOf(foundTextUnitDTOsByNameAndContent);
            }
        }

        logUnmatchedStatistics(logAsset, textUnitStatistic);
        return null;
    }

    private void logUnmatchedStatistics(Asset asset, ImportTextUnitStatisticsBody textUnitStatistic) {
        String message = String.format(
                "No equivalent text unit found, skip import statistics for text unit with name: %1$s for asset with id: %2$s & path: %3$s",
                textUnitStatistic.getName(),
                asset.getId(),
                asset.getPath());

        logger.warn(message);

        Tags tags = Tags.of("assetId", String.valueOf(asset.getId()),
                "assetPath", asset.getPath());

        meterRegistry.summary("services.TMTextUnitStatisticService.ImportUnmatchedStatistics", tags);
    }

    private ImmutableMap<ImportTextUnitStatisticsBody, List<TMTextUnit>> getStatisticsToTextUnitMap(ImmutableMap<ImportTextUnitStatisticsBody, ImmutableList<TextUnitDTO>> statisticToTextUnitDTOMap, ImmutableList<Long> textUnitIds) {
        List<TMTextUnit> textUnits = new ArrayList<>();
        Iterables.partition(textUnitIds, batchSize).forEach(
                textUnitIdBatch -> textUnits.addAll(tmTextUnitRepository.findByIdInAndEagerFetchStatistics(textUnitIdBatch))
        );

        ImmutableMap<Long, TMTextUnit> idTextUnitMap = textUnits.stream().collect(
                ImmutableMap.toImmutableMap(BaseEntity::getId, Function.identity())
        );

        return statisticToTextUnitDTOMap.entrySet()
                .stream()
                .map(statisticToTextUnitDTOEntry -> {
                    ImportTextUnitStatisticsBody statistics = statisticToTextUnitDTOEntry.getKey();
                    ImmutableList<TextUnitDTO> textUnitDTOs = statisticToTextUnitDTOEntry.getValue();

                    List<TMTextUnit> matchedTextUnits = textUnitDTOs.stream()
                            .map(textUnitDTO -> idTextUnitMap.get(textUnitDTO.getTmTextUnitId()))
                            .collect(Collectors.toList());

                    return new AbstractMap.SimpleEntry<>(
                            statistics,
                            matchedTextUnits
                            );
                })
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private void uploadStatistics(Map <ImportTextUnitStatisticsBody, List<TMTextUnit>> statisticsToTextUnitsMap) {
        List<TMTextUnitStatistic> updatedStatistics = statisticsToTextUnitsMap.entrySet().stream()
                .flatMap(statisticsToTextUnitEntry -> {
                    ImportTextUnitStatisticsBody textUnitStatistic = statisticsToTextUnitEntry.getKey();
                    List<TMTextUnit> tmTextUnits = statisticsToTextUnitEntry.getValue();

                    return tmTextUnits.stream()
                            .map(tmTextUnit -> {
                                logger.debug("Add statistics for text unit with name: {}.", textUnitStatistic.getName());

                                TMTextUnitStatistic statistic = tmTextUnit.getStatistic();

                                if (statistic == null) {
                                    statistic = new TMTextUnitStatistic();
                                    statistic.setTMTextUnit(tmTextUnit);
                                }

                                statistic.setLastDayUsageCount(textUnitStatistic.getLastDayEstimatedVolume());
                                statistic.setLastPeriodUsageCount(textUnitStatistic.getLastPeriodEstimatedVolume());
                                statistic.setLastSeenDate(textUnitStatistic.getLastSeenDate());

                                return statistic;
                            });
                })
                .collect(Collectors.toList());

        Iterables.partition(updatedStatistics, batchSize).forEach(
                updatedStatisticsBatch -> tmTextUnitStatisticRepository.saveAll(updatedStatisticsBatch)
        );
    }

    private String getTextUnitMd5(ImportTextUnitStatisticsBody textUnitStatistic) {
        return textUnitUtils.computeTextUnitMD5(
                textUnitStatistic.getName(), textUnitStatistic.getContent(), textUnitStatistic.getComment()
        );
    }
}
