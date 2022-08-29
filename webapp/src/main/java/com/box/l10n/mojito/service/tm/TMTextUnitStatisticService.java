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
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.textunitdtocache.TextUnitDTOsCacheService;
import com.box.l10n.mojito.service.tm.textunitdtocache.UpdateType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** @author garion */
@Service
public class TMTextUnitStatisticService {
  /** logger */
  static Logger logger = LoggerFactory.getLogger(TMTextUnitStatisticService.class);

  @Autowired MeterRegistry meterRegistry;

  @Autowired TextUnitBatchMatcher textUnitBatchMatcher;

  @Autowired TextUnitDTOsCacheService textUnitDTOsCacheService;

  @Autowired TMTextUnitRepository tmTextUnitRepository;

  @Autowired TMTextUnitStatisticRepository tmTextUnitStatisticRepository;

  @Autowired TextUnitUtils textUnitUtils;

  int batchSize = 1000;

  /**
   * Imports statistics information for a target text unit by matching it using name, content and
   * comment information. Existing statistic data is overwritten by incoming data.
   *
   * @param locale the locale of the target text unit
   * @param asset the asset of the target text unit
   * @param textUnitStatistics the statistics of the text unit
   * @return a PollableFuture
   */
  @Pollable(async = true, message = "Start importing text unit statistics.")
  public PollableFuture<Void> importStatistics(
      Locale locale, Asset asset, List<ImportTextUnitStatisticsBody> textUnitStatistics) {
    logger.info(
        "Import {} statistics for the text units for locale: {} and asset: {}",
        textUnitStatistics.size(),
        locale.getBcp47Tag(),
        asset.getPath());

    ImmutableList<TextUnitDTO> textUnitDTOs =
        textUnitDTOsCacheService.getTextUnitDTOsForAssetAndLocale(
            asset.getId(), locale.getId(), true, UpdateType.ALWAYS);

    ImmutableMap<String, List<TextUnitDTO>> textUnitDTOsByName =
        textUnitDTOs.stream()
            .collect(
                Collectors.collectingAndThen(
                    Collectors.groupingBy(TextUnitDTO::getName), ImmutableMap::copyOf));

    ImmutableMap<ImportTextUnitStatisticsBody, ImmutableList<TextUnitDTO>>
        statisticToTextUnitDTOMap =
            textUnitStatistics.stream()
                .map(
                    textUnitStatistic ->
                        new AbstractMap.SimpleEntry<>(
                            textUnitStatistic,
                            getMatchingTextUnitDTOs(textUnitStatistic, textUnitDTOsByName, asset)))
                .filter(entry -> entry.getValue() != null)
                .collect(
                    ImmutableMapCollectors.mapEntriesToImmutableMap(
                        (textUnitDTOsFirst, textUnitDTOsLast) -> textUnitDTOsLast));

    AtomicInteger batchNumber = new AtomicInteger(1);
    Iterables.partition(statisticToTextUnitDTOMap.entrySet(), batchSize)
        .forEach(
            statisticToTextUnitDTOBatch -> {
              logger.info(
                  "Processing Text Unit Statistics batch number: {}, size: {}",
                  batchNumber.getAndIncrement(),
                  statisticToTextUnitDTOBatch.size());

              ImmutableMap<ImportTextUnitStatisticsBody, ImmutableList<TextUnitDTO>>
                  statisticToTextUnitDTOBatchMap = ImmutableMap.copyOf(statisticToTextUnitDTOBatch);

              ImmutableList<Long> textUnitIds =
                  statisticToTextUnitDTOBatchMap.values().stream()
                      .flatMap(Collection::stream)
                      .map(TextUnitDTO::getTmTextUnitId)
                      .distinct()
                      .collect(ImmutableList.toImmutableList());

              updateStatistics(statisticToTextUnitDTOBatchMap, textUnitIds);
            });

    return new PollableFutureTaskResult<Void>();
  }

  @Transactional
  private void updateStatistics(
      ImmutableMap<ImportTextUnitStatisticsBody, ImmutableList<TextUnitDTO>>
          statisticToTextUnitDTOMap,
      ImmutableList<Long> textUnitIds) {
    Map<ImportTextUnitStatisticsBody, List<TMTextUnit>> statisticsToTextUnitsMap =
        getStatisticsToTextUnitMap(statisticToTextUnitDTOMap, textUnitIds);

    List<TMTextUnitStatistic> updatedStatistics =
        statisticsToTextUnitsMap.entrySet().stream()
            .flatMap(
                statisticsToTextUnitEntry -> {
                  ImportTextUnitStatisticsBody textUnitStatistic =
                      statisticsToTextUnitEntry.getKey();
                  List<TMTextUnit> tmTextUnits = statisticsToTextUnitEntry.getValue();

                  return tmTextUnits.stream()
                      .map(
                          tmTextUnit -> {
                            logger.debug(
                                "Add statistics for text unit with name: {}.",
                                textUnitStatistic.getName());

                            TMTextUnitStatistic statistic = tmTextUnit.getStatistic();

                            if (statistic == null) {
                              statistic = new TMTextUnitStatistic();
                              statistic.setTMTextUnit(tmTextUnit);
                            }

                            statistic.setLastDayUsageCount(
                                textUnitStatistic.getLastDayEstimatedVolume());
                            statistic.setLastPeriodUsageCount(
                                textUnitStatistic.getLastPeriodEstimatedVolume());
                            statistic.setLastSeenDate(textUnitStatistic.getLastSeenDate());

                            return statistic;
                          });
                })
            .collect(
                Collectors.collectingAndThen(
                    Collectors.toMap(
                        TMTextUnitStatistic::getTMTextUnit,
                        Function.identity(),
                        (stat1, stat2) -> {
                          stat1.setLastDayUsageCount(
                              stat1.getLastDayUsageCount() + stat2.getLastDayUsageCount());
                          stat1.setLastPeriodUsageCount(
                              stat1.getLastPeriodUsageCount() + stat2.getLastPeriodUsageCount());
                          DateTime stat1date = stat1.getLastSeenDate();
                          DateTime stat2date = stat2.getLastSeenDate();
                          DateTime maxDate =
                              stat1date.compareTo(stat2date) >= 0 ? stat1date : stat2date;
                          stat1.setLastSeenDate(maxDate);

                          logger.warn(
                              "Merged statistics information for TextUnit with id: {} , name: {}",
                              stat1.getTMTextUnit().getId(),
                              stat1.getTMTextUnit().getName());

                          return stat1;
                        }),
                    map -> new ArrayList<>(map.values())));

    List<TMTextUnitStatistic> newStatistics =
        updatedStatistics.stream().filter(s -> s.getId() == null).collect(Collectors.toList());

    tmTextUnitStatisticRepository.saveAll(newStatistics);
  }

  private ImmutableList<TextUnitDTO> getMatchingTextUnitDTOs(
      ImportTextUnitStatisticsBody textUnitStatistic,
      ImmutableMap<String, List<TextUnitDTO>> textUnitDTOsByName,
      Asset logAsset) {
    List<TextUnitDTO> foundTextUnitDTOsByNameAndContent =
        textUnitDTOsByName.get(textUnitStatistic.getName());

    if (foundTextUnitDTOsByNameAndContent != null && foundTextUnitDTOsByNameAndContent.size() > 0) {
      return ImmutableList.copyOf(foundTextUnitDTOsByNameAndContent);
    }

    logUnmatchedStatistics(logAsset, textUnitStatistic);
    return null;
  }

  private void logUnmatchedStatistics(Asset asset, ImportTextUnitStatisticsBody textUnitStatistic) {
    String message =
        String.format(
            "Statistics import skipped. No equivalent text unit found for text unit statistic with data: %1$s for asset with id: %2$s & path: %3$s",
            textUnitStatistic, asset.getId(), asset.getPath());

    logger.warn(message);

    Tags tags = Tags.of("assetId", String.valueOf(asset.getId()), "assetPath", asset.getPath());

    meterRegistry.summary("services.TMTextUnitStatisticService.ImportUnmatchedStatistics", tags);
  }

  private ImmutableMap<ImportTextUnitStatisticsBody, List<TMTextUnit>> getStatisticsToTextUnitMap(
      ImmutableMap<ImportTextUnitStatisticsBody, ImmutableList<TextUnitDTO>>
          statisticToTextUnitDTOMap,
      ImmutableList<Long> textUnitIds) {
    List<TMTextUnit> textUnits =
        new ArrayList<>(tmTextUnitRepository.findByIdInAndEagerFetchStatistics(textUnitIds));

    ImmutableMap<Long, TMTextUnit> idTextUnitMap =
        textUnits.stream()
            .collect(ImmutableMap.toImmutableMap(BaseEntity::getId, Function.identity()));

    return statisticToTextUnitDTOMap.entrySet().stream()
        .map(
            statisticToTextUnitDTOEntry -> {
              ImportTextUnitStatisticsBody statistics = statisticToTextUnitDTOEntry.getKey();
              ImmutableList<TextUnitDTO> textUnitDTOs = statisticToTextUnitDTOEntry.getValue();

              List<TMTextUnit> matchedTextUnits =
                  textUnitDTOs.stream()
                      .map(textUnitDTO -> idTextUnitMap.get(textUnitDTO.getTmTextUnitId()))
                      .collect(Collectors.toList());

              return new AbstractMap.SimpleEntry<>(statistics, matchedTextUnits);
            })
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private String getTextUnitMd5(ImportTextUnitStatisticsBody textUnitStatistic) {
    return textUnitUtils.computeTextUnitMD5(
        textUnitStatistic.getName(),
        textUnitStatistic.getContent(),
        textUnitStatistic.getComment());
  }
}
