package com.box.l10n.mojito.service.branch;

import static com.box.l10n.mojito.quartz.QuartzSchedulerManager.DEFAULT_SCHEDULER_NAME;
import static com.box.l10n.mojito.service.assetExtraction.AssetExtractionService.PRIMARY_BRANCH;
import static com.box.l10n.mojito.service.tm.search.StatusFilter.FOR_TRANSLATION;
import static com.box.l10n.mojito.utils.Predicates.not;
import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.BranchStatistic;
import com.box.l10n.mojito.entity.BranchTextUnitStatistic;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.localtm.merger.BranchStateTextUnit;
import com.box.l10n.mojito.localtm.merger.MultiBranchState;
import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.assetExtraction.AssetTextUnitToTMTextUnitRepository;
import com.box.l10n.mojito.service.assetExtraction.MultiBranchStateService;
import com.box.l10n.mojito.service.branch.notification.job.BranchNotificationJob;
import com.box.l10n.mojito.service.branch.notification.job.BranchNotificationJobInput;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.textunitdtocache.TextUnitDTOsCacheService;
import com.box.l10n.mojito.service.tm.textunitdtocache.UpdateType;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BranchStatisticService {

  public static final int BATCH_SIZE_UPDATE_BRANCH_TEXT_UNIT_STATISTICS = 1000;
  /** logger */
  static Logger logger = getLogger(BranchStatisticService.class);

  @Autowired BranchService branchService;

  @Autowired BranchRepository branchRepository;

  @Autowired BranchStatisticRepository branchStatisticRepository;

  @Autowired BranchTextUnitStatisticRepository branchTextUnitStatisticRepository;

  @Autowired QuartzPollableTaskScheduler quartzPollableTaskScheduler;

  @Autowired TextUnitSearcher textUnitSearcher;

  @Autowired TMTextUnitRepository tmTextUnitRepository;

  @Autowired AssetTextUnitToTMTextUnitRepository assetTextUnitToTMTextUnitRepository;

  @Autowired MultiBranchStateService multiBranchStateService;

  @Autowired AssetRepository assetRepository;

  @Autowired TextUnitDTOsCacheService textUnitDTOsCacheService;

  @Autowired EntityManager entityManager;

  @Autowired MeterRegistry meterRegistry;

  @Autowired RepositoryRepository repositoryRepository;

  @Value("${l10n.branchStatistic.quartz.schedulerName:" + DEFAULT_SCHEDULER_NAME + "}")
  String schedulerName;

  /**
   * Compute statistics for all branches that are not deleted in a given repository.
   *
   * @param repositoryId
   * @param updateType
   */
  public void computeAndSaveBranchStatistics(
      Long repositoryId, String repositoryName, UpdateType updateType) {
    logger.debug("computeAndSaveBranchStatistics for repository: {}", repositoryId);
    meterRegistry
        .timer(
            "BranchStatisticService.computeAndSaveBranchStatistics",
            Tags.of("repositoryName", repositoryName))
        .record(
            () -> {
              List<Branch> branchesToCheck = getBranchesToProcess(repositoryId);
              computeAndSaveBranchStatistics(repositoryId, updateType, branchesToCheck);
              for (Branch branch : branchesToCheck) {
                scheduleBranchNotification(branch);
              }
            });
  }

  /**
   * Computes and save branch statistics
   *
   * @param branch
   */
  public void computeAndSaveBranchStatistics(Branch branch) {
    logger.debug(
        "computeAndSaveBranchStatistics for branch: {} ({})", branch.getId(), branch.getName());
    computeAndSaveBranchStatistics(
        branch.getRepository().getId(), UpdateType.ALWAYS, ImmutableList.of(branch));
  }

  void computeAndSaveBranchStatistics(
      Long repositoryId, UpdateType updateType, List<Branch> branches) {

    if (!branches.isEmpty()) {

      Repository repository = repositoryRepository.findById(repositoryId).orElse(null);
      String repositoryName = repository != null ? repository.getName() : null;
      ImmutableSet<String> branchNamesToCheck =
          branches.stream().map(Branch::getName).collect(ImmutableSet.toImmutableSet());

      logger.info("Computing branch stastistics");
      Map<String, ImmutableMap<Long, ForTranslationCountForTmTextUnitId>>
          mapBranchNameToTranslationCountForTextUnitId =
              assetRepository.findIdByRepositoryIdAndDeleted(repositoryId, false).stream()
                  .flatMap(
                      assetId -> {
                        AssetExtraction lastSuccessfulAssetExtraction =
                            assetRepository
                                .findById(assetId)
                                .get()
                                .getLastSuccessfulAssetExtraction();
                        MultiBranchState multiBranchState =
                            multiBranchStateService.getMultiBranchStateForAssetExtractionId(
                                lastSuccessfulAssetExtraction.getId(),
                                lastSuccessfulAssetExtraction.getVersion());

                        return lastSuccessfulAssetExtraction.getAsset().getRepository()
                            .getRepositoryLocales().stream()
                            .filter(
                                rl -> rl.getParentLocale() != null && rl.isToBeFullyTranslated())
                            .flatMap(
                                rl -> {
                                  ImmutableMap<Long, TextUnitDTO>
                                      textUnitDTOsForLocaleByTmTextUnitIds =
                                          textUnitDTOsCacheService
                                              .getTextUnitDTOsForAssetAndLocale(
                                                  assetId,
                                                  rl.getLocale().getId(),
                                                  false,
                                                  updateType)
                                              .stream()
                                              .collect(
                                                  ImmutableMap.toImmutableMap(
                                                      TextUnitDTO::getTmTextUnitId,
                                                      Function.identity()));

                                  return multiBranchState.getBranches().stream()
                                      .filter(
                                          branch -> branchNamesToCheck.contains(branch.getName()))
                                      .flatMap(
                                          branch ->
                                              computeTranslationCountForBranch(
                                                  assetId,
                                                  branch,
                                                  multiBranchState,
                                                  textUnitDTOsForLocaleByTmTextUnitIds,
                                                  repositoryName));
                                })
                            .collect(groupByBranchAndCount())
                            .values()
                            .stream()
                            .flatMap(
                                longForTranslationCountForTmTextUnitIdImmutableMap ->
                                    longForTranslationCountForTmTextUnitIdImmutableMap.values()
                                        .stream());
                      })
                  .collect(toMapBranchNameToTranslationCountForTextUnitId());

      logger.debug("Updating branch statistics");
      for (Branch branch : branches) {
        updateBranchStatisticInTx(
            branch,
            mapBranchNameToTranslationCountForTextUnitId.getOrDefault(
                branch.getName(), ImmutableMap.of()));
      }
      logger.debug("Finished computing statistics");
    }
  }

  Stream<ForTranslationCountForTmTextUnitId> computeTranslationCountForBranch(
      Long assetId,
      com.box.l10n.mojito.localtm.merger.Branch branch,
      MultiBranchState multiBranchState,
      ImmutableMap<Long, TextUnitDTO> textUnitDTOsForLocaleByTmTextUnitIds,
      String repositoryName) {

    return meterRegistry
        .timer(
            "BranchStatisticService.computeTranslationCountForBranch",
            Tags.of("repository", repositoryName))
        .record(
            () -> {
              logger.debug("Proccesing asset id: {} for branch: {}", assetId, branch.getName());
              ImmutableList<Long> tmTextUnitIds =
                  multiBranchState.getBranchStateTextUnits().stream()
                      .filter(
                          bstu -> bstu.getBranchNameToBranchDatas().containsKey(branch.getName()))
                      .map(BranchStateTextUnit::getTmTextUnitId)
                      .collect(ImmutableList.toImmutableList());

              Stream<ForTranslationCountForTmTextUnitId> forTranslationCountForTmTextUnitIdStream =
                  tmTextUnitIds.stream()
                      .map(
                          tmTextUnitId -> {
                            long forTranslationCount =
                                Optional.ofNullable(
                                        textUnitDTOsForLocaleByTmTextUnitIds.get(tmTextUnitId))
                                    .filter(TextUnitDTO::isUsed)
                                    .filter(not(TextUnitDTO::isDoNotTranslate))
                                    .filter(
                                        textUnitDTOsCacheService.statusPredicate(FOR_TRANSLATION))
                                    .map(textUnitDTO -> 1L)
                                    .orElse(0L);

                            long totalCount =
                                Optional.ofNullable(
                                        textUnitDTOsForLocaleByTmTextUnitIds.get(tmTextUnitId))
                                    .filter(TextUnitDTO::isUsed)
                                    .filter(not(TextUnitDTO::isDoNotTranslate))
                                    .map(textUnitDTO -> 1L)
                                    .orElse(0L);

                            return ForTranslationCountForTmTextUnitId.builder()
                                .tmTextUnitId(tmTextUnitId)
                                .forTranslationCount(forTranslationCount)
                                .totalCount(totalCount)
                                .branch(branch)
                                .build();
                          });
              return forTranslationCountForTmTextUnitIdStream;
            });
  }

  @Transactional
  void updateBranchStatisticInTx(
      Branch branch,
      ImmutableMap<Long, ForTranslationCountForTmTextUnitId> tmTextUnitIdToForTranslationCount) {
    Preconditions.checkNotNull(tmTextUnitIdToForTranslationCount);
    Preconditions.checkNotNull(branch);

    BranchStatistic branchStatistic =
        updateBranchStatistic(tmTextUnitIdToForTranslationCount, branch);

    logger.debug(
        "Updating branch text unit statistics for branch: {} ({}), need to process: {} entries",
        branch.getId(),
        branch.getName(),
        tmTextUnitIdToForTranslationCount.size());
    Lists.partition(
            tmTextUnitIdToForTranslationCount.keySet().asList(),
            BATCH_SIZE_UPDATE_BRANCH_TEXT_UNIT_STATISTICS)
        .forEach(
            updateBatchOfBranchTextUnitStatistics(
                tmTextUnitIdToForTranslationCount, branchStatistic));

    logger.debug(
        "Get tm text unit ids to remove for branch: {} ({})", branch.getId(), branch.getName());
    Set<Long> tmTextUnitIdsToRemove =
        branchTextUnitStatisticRepository.findTmTextUnitIds(branchStatistic.getId()).stream()
            .filter(id -> !tmTextUnitIdToForTranslationCount.keySet().contains(id))
            .collect(Collectors.toSet());

    logger.debug(
        "Removing branch text units ({}) from branch: {} ({})",
        tmTextUnitIdsToRemove.size(),
        branch.getId(),
        branch.getName());
    int removedCount =
        branchTextUnitStatisticRepository.deleteByBranchStatisticBranchIdAndTmTextUnitIdIn(
            branch.getId(), tmTextUnitIdsToRemove);
    logger.debug("Removed statistic: {}", removedCount);
  }

  private Consumer<List<Long>> updateBatchOfBranchTextUnitStatistics(
      ImmutableMap<Long, ForTranslationCountForTmTextUnitId> tmTextUnitIdToForTranslationCount,
      BranchStatistic branchStatistic) {
    return tmTextUnitIds -> {
      logger.trace("Get BranchTextUnitStatistic for tmTextUnitIds");
      ImmutableMap<Long, BranchTextUnitStatisticWithCounts> tmTextUnitToBranchTextUnitStatistics =
          branchTextUnitStatisticRepository
              .getByBranchStatisticIdAndTmTextUnitIdIn(branchStatistic.getId(), tmTextUnitIds)
              .stream()
              .collect(
                  ImmutableMap.toImmutableMap(
                      branchTextUnitStatistic -> branchTextUnitStatistic.getTmTextUnitId(),
                      Function.identity()));

      ImmutableList<BranchTextUnitStatistic> branchTextUnitStatistics =
          tmTextUnitIds.stream()
              .map(
                  tmTextUnitId -> {
                    BranchTextUnitStatisticWithCounts branchTextUnitStatisticWithCounts =
                        tmTextUnitToBranchTextUnitStatistics.get(tmTextUnitId);

                    long forTranslationCount =
                        tmTextUnitIdToForTranslationCount
                            .get(tmTextUnitId)
                            .getForTranslationCount();
                    long totalCount =
                        tmTextUnitIdToForTranslationCount.get(tmTextUnitId).getTotalCount();

                    BranchTextUnitStatistic branchTextUnitStatistic = new BranchTextUnitStatistic();
                    branchTextUnitStatistic.setBranchStatistic(branchStatistic);
                    branchTextUnitStatistic.setTmTextUnit(
                        tmTextUnitRepository.getOne(tmTextUnitId));
                    branchTextUnitStatistic.setForTranslationCount(forTranslationCount);
                    branchTextUnitStatistic.setTotalCount(totalCount);

                    if (branchTextUnitStatisticWithCounts != null) {
                      logger.debug("BranchTextUnitStatistic entity exists, check to update");
                      if (forTranslationCount
                              != branchTextUnitStatisticWithCounts.getForTranslationCount()
                          || totalCount != branchTextUnitStatisticWithCounts.getTotalCount()) {
                        logger.trace(
                            "Update needed (forTranslation: {}, total: {})",
                            forTranslationCount,
                            totalCount);
                        branchTextUnitStatistic.setId(branchTextUnitStatisticWithCounts.getId());
                      } else {
                        logger.trace("Update not needed, count are the same");
                        branchTextUnitStatistic = null;
                      }
                    } else {
                      logger.debug("BranchTextUnitStatistic entity doesn't exist, create it");
                    }

                    return branchTextUnitStatistic;
                  })
              .filter(Objects::nonNull)
              .collect(ImmutableList.toImmutableList());

      branchTextUnitStatisticRepository.saveAll(branchTextUnitStatistics);
      entityManager.flush();
      entityManager.clear();
      logger.trace("After saveAll()");
    };
  }

  BranchStatistic updateBranchStatistic(
      ImmutableMap<Long, ForTranslationCountForTmTextUnitId> tmTextUnitIdToForTranslationCount,
      Branch branch) {
    BranchStatistic branchStatistic = getOrCreateBranchStatistic(branch);

    logger.debug("Computing total counts for branch: {} ({})", branch.getId(), branch.getName());

    long sumTotalCount =
        tmTextUnitIdToForTranslationCount.values().stream()
            .mapToLong(ForTranslationCountForTmTextUnitId::getTotalCount)
            .sum();

    long sumForTranslationCount =
        tmTextUnitIdToForTranslationCount.values().stream()
            .mapToLong(ForTranslationCountForTmTextUnitId::getForTranslationCount)
            .sum();

    branchStatistic.setForTranslationCount(sumForTranslationCount);
    branchStatistic.setTotalCount(sumTotalCount);

    logger.debug(
        "Updating branch statistics for branch: {} ({})", branch.getId(), branch.getName());
    return branchStatisticRepository.save(branchStatistic);
  }

  BranchStatistic getOrCreateBranchStatistic(Branch branch) {
    BranchStatistic branchStatistic = branchStatisticRepository.findByBranch(branch);

    if (branchStatistic == null) {
      logger.debug("No branchStatistic, create it");
      branchStatistic = new BranchStatistic();
      branchStatistic.setBranch(branch);
      branchStatistic = branchStatisticRepository.save(branchStatistic);
    }

    return branchStatistic;
  }

  Collector<
          ForTranslationCountForTmTextUnitId,
          ?,
          Map<String, ImmutableMap<Long, ForTranslationCountForTmTextUnitId>>>
      toMapBranchNameToTranslationCountForTextUnitId() {
    return Collectors.groupingBy(
        forTranslationCountForTmTextUnitId ->
            forTranslationCountForTmTextUnitId.getBranch().getName(),
        ImmutableMap.toImmutableMap(
            ForTranslationCountForTmTextUnitId::getTmTextUnitId,
            Function.identity(),
            (t1, t2) -> {
              return t1.withForTranslationCount(
                      t1.getForTranslationCount() + t2.getForTranslationCount())
                  .withTotalCount(t1.getTotalCount() + t2.getTotalCount());
            }));
  }

  Collector<
          ForTranslationCountForTmTextUnitId,
          ?,
          Map<
              com.box.l10n.mojito.localtm.merger.Branch,
              ImmutableMap<Long, ForTranslationCountForTmTextUnitId>>>
      groupByBranchAndCount() {
    return Collectors.groupingBy(
        ForTranslationCountForTmTextUnitId::getBranch,
        ImmutableMap.toImmutableMap(
            ForTranslationCountForTmTextUnitId::getTmTextUnitId,
            Function.identity(),
            (t1, t2) -> {
              return t1.withForTranslationCount(
                      t1.getForTranslationCount() + t2.getForTranslationCount())
                  .withTotalCount(t1.getTotalCount() + t2.getTotalCount());
            }));
  }

  void scheduleBranchNotification(Branch branch) {
    BranchNotificationJobInput branchNotificationJobInput = new BranchNotificationJobInput();
    branchNotificationJobInput.setBranchId(branch.getId());

    QuartzJobInfo<BranchNotificationJobInput, Void> quartzJobInfo =
        QuartzJobInfo.newBuilder(BranchNotificationJob.class)
            .withUniqueId(String.valueOf(branch.getId()))
            .withInput(branchNotificationJobInput)
            .withScheduler(schedulerName)
            .build();
    quartzPollableTaskScheduler.scheduleJob(quartzJobInfo);
  }

  /**
   * The branches to process/compute statistics for are the ones that are not deleted AND are not
   * the primary branch (branch with null name is also excluded at the moment since this mean we're
   * not really working with branches)
   *
   * @param repositoryId
   * @return
   */
  List<Branch> getBranchesToProcess(Long repositoryId) {
    List<Branch> branches =
        branchRepository.findByRepositoryIdAndDeletedFalseAndNameNotNullAndNameNot(
            repositoryId, PRIMARY_BRANCH);
    return branches;
  }

  /**
   * Gets the text units of the specified branch.
   *
   * @param branch
   * @return
   */
  public List<TextUnitDTO> getTextUnitDTOsForBranch(Branch branch) {
    logger.debug("Get text units for branch: {} ({})", branch.getId(), branch.getName());

    List<Long> branchTmTextUnitIds = getBranchTextUnitIds(branch);

    if (branchTmTextUnitIds.size() > 100) {
      logger.info(
          "getTextUnitDTOsForBranch() will pass {} tm text unit ids to the text unit searcher, repositoryId: {}, branch: {} ({})",
          branchTmTextUnitIds.size(),
          branch.getRepository().getId(),
          branch.getId(),
          branch.getName());
    }

    List<TextUnitDTO> textUnitDTOS;

    if (branchTmTextUnitIds.isEmpty()) {
      textUnitDTOS = Collections.emptyList();
    } else {
      textUnitDTOS = getTextUnitDTOSForTmTextUnitIds(branch, branchTmTextUnitIds);
    }

    return textUnitDTOS;
  }

  private List<TextUnitDTO> getTextUnitDTOSForTmTextUnitIds(
      Branch branch, List<Long> tmTextUnitIds) {

    TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
    textUnitSearcherParameters.setRepositoryIds(branch.getRepository().getId());
    textUnitSearcherParameters.setTmTextUnitIds(tmTextUnitIds);
    textUnitSearcherParameters.setForRootLocale(true);

    return meterRegistry
        .timer(
            "BranchStatisticService.getTextUnitDTOSForTmTextUnitIds",
            Tags.of(
                "repositoryId",
                Objects.toString(branch.getRepository().getId()),
                "branchId",
                Objects.toString(branch.getId())))
        .record(
            () -> {
              return textUnitSearcher.search(textUnitSearcherParameters);
            });
  }

  private List<Long> getBranchTextUnitIds(Branch branch) {
    return meterRegistry
        .timer(
            "BranchStatisticService.getBranchTextUnitIds",
            Tags.of(
                "repositoryId",
                Objects.toString(branch.getRepository().getId()),
                "branchId",
                Objects.toString(branch.getId())))
        .record(
            () -> {
              return assetTextUnitToTMTextUnitRepository.findByBranch(branch);
            });
  }
}
