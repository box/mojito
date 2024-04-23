package com.box.l10n.mojito.service.assetExtraction;

import static com.box.l10n.mojito.quartz.QuartzSchedulerManager.DEFAULT_SCHEDULER_NAME;
import static com.box.l10n.mojito.service.assetExtraction.LocalBranchToEntityBranchConverter.NULL_BRANCH_TEXT_PLACEHOLDER;
import static java.util.function.Function.identity;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetContent;
import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.AssetExtractionByBranch;
import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.entity.AssetTextUnitToTMTextUnit;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.PluralForm;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.PushRun;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.localtm.merger.AssetExtractorTextUnitsToMultiBranchStateConverter;
import com.box.l10n.mojito.localtm.merger.BranchData;
import com.box.l10n.mojito.localtm.merger.BranchStateTextUnit;
import com.box.l10n.mojito.localtm.merger.MultiBranchState;
import com.box.l10n.mojito.localtm.merger.MultiBranchStateMerger;
import com.box.l10n.mojito.okapi.FilterConfigIdOverride;
import com.box.l10n.mojito.okapi.TextUnitUtils;
import com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.okapi.extractor.AssetExtractor;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.asset.FilterOptionsMd5Builder;
import com.box.l10n.mojito.service.assetTextUnit.AssetTextUnitRepository;
import com.box.l10n.mojito.service.assetcontent.AssetContentService;
import com.box.l10n.mojito.service.blobstorage.StructuredBlobStorage;
import com.box.l10n.mojito.service.branch.BranchRepository;
import com.box.l10n.mojito.service.leveraging.LeveragerByTmTextUnit;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pluralform.PluralFormService;
import com.box.l10n.mojito.service.pollableTask.ParentTask;
import com.box.l10n.mojito.service.pollableTask.Pollable;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableFutureTaskResult;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.pushrun.PushRunService;
import com.box.l10n.mojito.service.repository.statistics.RepositoryStatisticsJobScheduler;
import com.box.l10n.mojito.service.tm.TMRepository;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.textunitdtocache.TextUnitDTOsCacheService;
import com.box.l10n.mojito.service.tm.textunitdtocache.UpdateType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ibm.icu.text.MessageFormat;
import io.micrometer.core.annotation.Timed;
import jakarta.persistence.EntityManager;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.RetryContext;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to manage asset extraction. It processes assets to extract {@link AssetTextUnit}s.
 *
 * @author aloison
 */
@Service
public class AssetExtractionService {

  public static final String PRIMARY_BRANCH = "master";
  public static final int BATCH_SIZE = 1000;

  /** logger */
  static Logger logger = LoggerFactory.getLogger(AssetExtractionService.class);

  @Autowired AssetMappingService assetMappingService;

  @Autowired AssetRepository assetRepository;

  @Autowired AssetContentService assetContentService;

  @Autowired AssetExtractionRepository assetExtractionRepository;

  @Autowired AssetTextUnitRepository assetTextUnitRepository;

  @Autowired AssetExtractionByBranchRepository assetExtractionByBranchRepository;

  @Autowired BranchRepository branchRepository;

  @Autowired QuartzPollableTaskScheduler quartzPollableTaskScheduler;

  @Autowired RetryTemplate retryTemplate;

  @Autowired FilterOptionsMd5Builder filterOptionsMd5Builder;

  @Autowired AssetExtractor assetExtractor;

  @Autowired PluralFormService pluralFormService;

  @Autowired ObjectMapper objectMapper;

  @Autowired TextUnitUtils textUnitUtils;

  @Autowired
  AssetExtractorTextUnitsToMultiBranchStateConverter
      assetExtractorTextUnitsToMultiBranchStateConverter;

  @Autowired MultiBranchStateMerger multiBranchStateMerger;

  @Autowired TMRepository tmRepository;

  @Autowired TMService tmService;

  @Autowired TMTextUnitRepository tmTextUnitRepository;

  @Autowired AssetTextUnitToTMTextUnitRepository assetTextUnitToTMTextUnitRepository;

  @Autowired StructuredBlobStorage structuredBlobStorage;

  @Autowired MultiBranchStateService multiBranchStateService;

  @Autowired TextUnitDTOsCacheService textUnitDTOsCacheService;

  @Autowired LocaleService localeService;

  @Autowired PollableTaskService pollableTaskService;

  @Autowired PushRunService pushRunService;

  @Autowired EntityManager entityManager;

  @Autowired LocalBranchToEntityBranchConverter localBranchToEntityBranchConverter;

  private RepositoryStatisticsJobScheduler repositoryStatisticsJobScheduler;

  @Value("${l10n.assetExtraction.quartz.schedulerName:" + DEFAULT_SCHEDULER_NAME + "}")
  String quartzSchedulerName;

  @Autowired
  public void setRepositoryStatisticsJobScheduler(
      RepositoryStatisticsJobScheduler repositoryStatisticsJobScheduler) {
    this.repositoryStatisticsJobScheduler = repositoryStatisticsJobScheduler;
  }

  /**
   * If the asset type is supported, starts the text units extraction for the given asset.
   *
   * @param parentTask The parent task to be updated
   * @param assetContentId {@link Asset#id} to extract text units from
   * @param filterConfigIdOverride
   * @param filterOptions
   * @param currentTask The current task, injected
   * @return A {@link Future}
   * @throws UnsupportedAssetFilterTypeException If asset type not supported
   * @throws AssetExtractionConflictException
   */
  public PollableFuture<Asset> processAsset(
      Long assetContentId,
      Long pushRunId,
      FilterConfigIdOverride filterConfigIdOverride,
      List<String> filterOptions,
      PollableTask currentTask)
      throws UnsupportedAssetFilterTypeException, AssetExtractionConflictException {

    logger.info("Start processing asset content, id: {}", assetContentId);
    AssetContent assetContent = assetContentService.findOne(assetContentId);
    Asset asset = getUndeletedAsset(assetContent.getAsset());

    MultiBranchState stateForNewContent =
        convertAssetContentToMultiBranchState(
            assetContent, filterConfigIdOverride, filterOptions, currentTask);
    CreateTextUnitsResult createdTextUnitsResult =
        createTextUnitsForNewContent(assetContent, stateForNewContent, currentTask);

    updateBranchAssetExtraction(
        assetContent, createdTextUnitsResult.getUpdatedState(), filterOptions, currentTask);
    updateLastSuccessfulAssetExtraction(
        asset, createdTextUnitsResult.getUpdatedState(), currentTask);
    updatePushRun(asset, createdTextUnitsResult.getUpdatedState(), pushRunId, currentTask);
    performLeveraging(createdTextUnitsResult.getLeveragingMatches(), currentTask);

    logger.info("Done processing asset content id: {}", assetContentId);
    return new PollableFutureTaskResult<>(asset);
  }

  @Pollable(message = "Updating merged asset text units")
  void updateLastSuccessfulAssetExtraction(
      Asset asset, MultiBranchState currentState, @ParentTask PollableTask currentTask) {
    logger.trace(
        "Make sure we have a last successful extraction in the Asset (legacy support edge case)");
    AssetExtraction lastSuccessfulAssetExtraction = getOrCreateLastSuccessfulAssetExtraction(asset);
    MultiBranchState lastSuccessfulMultiBranchState =
        updateAssetExtractionWithState(
            lastSuccessfulAssetExtraction.getId(), currentState, AssetContentMd5s.of());
  }

  @Pollable(message = "Updating branch asset text units")
  void updateBranchAssetExtraction(
      AssetContent assetContent,
      MultiBranchState currentState,
      List<String> filterOptions,
      @ParentTask PollableTask currentTask) {
    AssetExtractionByBranch assetExtractionByBranch =
        getUndeletedOrCreateAssetExtractionByBranch(assetContent);
    AssetContentMd5s assetContentMd5s =
        AssetContentMd5s.of()
            .withContentMd5(assetContent.getContentMd5())
            .withFilterOptionsMd5(filterOptionsMd5Builder.md5(filterOptions));
    updateAssetExtractionWithState(
        assetExtractionByBranch.getAssetExtraction().getId(), currentState, assetContentMd5s);
  }

  @Pollable(message = "Updating push run")
  void updatePushRun(
      Asset asset,
      MultiBranchState multiBranchState,
      Long pushRunId,
      @ParentTask PollableTask parentTask) {

    if (pushRunId == null || pushRunId == 0) {
      logger.debug("Skipping associating text units to PushRun as no PushRun was provided!");
      return;
    }

    PushRun pushRun = pushRunService.getPushRunById(pushRunId);

    List<Long> textUnitIds =
        multiBranchState.getBranchStateTextUnits().stream()
            .map(BranchStateTextUnit::getTmTextUnitId)
            .collect(Collectors.toList());

    pushRunService.associatePushRunToTextUnitIds(pushRun, asset, textUnitIds);
  }

  MultiBranchState updateAssetExtractionWithState(
      Long assetExtractionId, MultiBranchState currentState, AssetContentMd5s assetContentMd5s) {
    return retryTemplate.execute(
        context -> {
          if (context.getRetryCount() > 0) {
            logger.info(
                "Assume concurrent modification when update asset extraction, re-fetch state, merge and save. Attempt: {}",
                context.getRetryCount());
          }

          logger.debug("updateAssetExtractionWithState, attempt: {}", context.getRetryCount());

          AssetExtraction assetExtraction =
              assetExtractionRepository
                  .findById(assetExtractionId)
                  .orElseThrow(
                      () ->
                          new RuntimeException(
                              "There must be an asset extraction for id: " + assetExtractionId));

          Preconditions.checkNotNull(
              assetExtraction.getVersion(),
              "Invalid asset extraction, there must be a version, asset extraction id: "
                  + assetExtractionId);
          final long assetExtractionVersionForDelete =
              assetExtraction
                  .getVersion(); // important to keep the version and not re-read it in some way
          // since it can be incremented

          if (context.getRetryCount() == 5) {
            // 5th is last right now, this is very brittle. Help fix deployement issue for now.
            logger.info(
                "On 5th retry, assume something is wrong with with the state. Delete state for asset extraction id: "
                    + assetExtractionId);
            multiBranchStateService.deleteMultiBranchStateForAssetExtractionId(
                assetExtraction.getId(), assetExtraction.getVersion());
          }

          logger.debug(
              "Fetching base state, asset extraction id: {}, version: {}",
              assetExtraction.getId(),
              assetExtraction.getVersion());
          MultiBranchState baseState =
              multiBranchStateService.getMultiBranchStateForAssetExtractionId(
                  assetExtraction.getId(), assetExtraction.getVersion());
          MultiBranchState newState =
              multiBranchStateMerger.merge(
                  currentState,
                  baseState,
                  ImmutableSet.of(PRIMARY_BRANCH, NULL_BRANCH_TEXT_PLACEHOLDER));

          Modifications modifications = getModifications(baseState, newState);
          MultiBranchState updatedMergedState =
              updateAssetExtractionWithStateInTx(
                  assetExtraction, newState, modifications, assetContentMd5s);

          // This is done outside the transaction because the delete can't be rollbacked when using
          // S3 storage  and
          // (database storage would benefit of that logic to be in the transacation) hence it would
          // corrupt the
          // dataset. A drawback is that if the service fail, this may not be deleted while the DB
          // is in final state.
          // We could have a cleanup job for those cases.
          multiBranchStateService.deleteMultiBranchStateForAssetExtractionId(
              assetExtractionId, assetExtractionVersionForDelete);
          return updatedMergedState;
        });
  }

  /**
   * @param currentState
   * @param currentAssetTextUnitsMd5ToIds
   * @param assetContentMd5s
   * @return
   * @throws DataIntegrityViolationException potential concurrent modification and optimistic
   *     locking may happen
   */
  @Transactional
  MultiBranchState updateAssetExtractionWithStateInTx(
      AssetExtraction assetExtraction,
      MultiBranchState currentState,
      Modifications modifications,
      AssetContentMd5s assetContentMd5s)
      throws DataIntegrityViolationException {

    removeAssetTextUnits(assetExtraction, modifications.getRemoved());
    updateAssetTextUnits(assetExtraction, modifications.getUpdated());

    ImmutableList<BranchStateTextUnit> createAssetTextUnits =
        createAssetTextUnits(assetExtraction, modifications.getAdded());
    MultiBranchState currentStateWithAssetTextUnitIds =
        updateStateWithCreatedAssetTextUnitIds(currentState, createAssetTextUnits);

    // This write can be done in transaction (even with s3 storage). If the transacation rollbacks,
    // the data will be
    // removed for the DB implementation. For the S3 implementation it won't. For S3 the data won't
    // be accessed
    // before it gets overridden with proper content so this should be safe.
    multiBranchStateService.putMultiBranchStateForAssetExtractionId(
        currentStateWithAssetTextUnitIds,
        assetExtraction.getId(),
        assetExtraction.getVersion() + 1);

    logger.debug(
        "Change asset extraction last modified date to create a new version for optimistic locking");
    assetExtraction.setLastModifiedDate(JSR310Migration.newDateTimeEmptyCtor());
    assetExtraction.setContentMd5(assetContentMd5s.getContentMd5());
    assetExtraction.setFilterOptionsMd5(assetContentMd5s.getFilterOptionsMd5());
    assetExtractionRepository.save(assetExtraction);
    return currentState;
  }

  MultiBranchState updateStateWithCreatedAssetTextUnitIds(
      MultiBranchState currentState, ImmutableList<BranchStateTextUnit> createAssetTextUnits) {

    ImmutableMap<String, Long> md5ToAssetTextUnitIds =
        createAssetTextUnits.stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    BranchStateTextUnit::getMd5, BranchStateTextUnit::getAssetTextUnitId));

    ImmutableList<BranchStateTextUnit> withAssetTextUnitIds =
        currentState.getBranchStateTextUnits().stream()
            .map(
                bstu -> {
                  Long assetTextUnitId = md5ToAssetTextUnitIds.get(bstu.getMd5());
                  if (assetTextUnitId != null) {
                    bstu = bstu.withAssetTextUnitId(assetTextUnitId);
                  }
                  return bstu;
                })
            .collect(ImmutableList.toImmutableList());

    return currentState.withBranchStateTextUnits(withAssetTextUnitIds);
  }

  /**
   * @param assetContent
   * @param stateForNewContent
   * @param currentTask
   * @return state with tm text units ids updated (either from creation or reading existing text
   *     units)
   */
  @Timed("AssetExtractionService.createTextUnitsForNewContent")
  @Pollable(message = "Create new text units")
  CreateTextUnitsResult createTextUnitsForNewContent(
      AssetContent assetContent,
      MultiBranchState stateForNewContent,
      @ParentTask PollableTask currentTask) {
    return retryTemplate.execute(
        context -> {
          if (context.getRetryCount() > 0) {
            logger.info(
                "Assume concurrent modification when creating tm text units for state (re-fetch current tm text unit ids and try to save again), attempt: {}",
                context.getRetryCount());
          }

          boolean updateCache = context.getRetryCount() > 0;
          logger.debug(
              "Read text unit dto from cache with update: {} (first attempt update if missing)",
              updateCache);
          ImmutableMap<String, TextUnitDTO> textUnitDTOsForAssetAndLocaleByMD5 =
              textUnitDTOsCacheService.getTextUnitDTOsForAssetAndLocaleByMD5(
                  assetContent.getAsset().getId(),
                  localeService.getDefaultLocale().getId(),
                  StatusFilter.ALL,
                  true,
                  updateCache ? UpdateType.ALWAYS : UpdateType.IF_MISSING);

          logger.debug("Update the state with tm text unit id from the database");
          MultiBranchState stateForNewContentWithIds =
              stateForNewContent.withBranchStateTextUnits(
                  stateForNewContent.getBranchStateTextUnits().stream()
                      .map(
                          bstu ->
                              Optional.ofNullable(
                                      textUnitDTOsForAssetAndLocaleByMD5.get(bstu.getMd5()))
                                  .map(
                                      textUnitDTO ->
                                          bstu.withTmTextUnitId(textUnitDTO.getTmTextUnitId()))
                                  .orElse(bstu))
                      .collect(ImmutableList.toImmutableList()));

          ImmutableList<BranchStateTextUnit> toCreateTmTextUnits =
              getBranchStateTextUnitsWithoutId(stateForNewContentWithIds);

          final Repository repository = assetContent.getBranch().getRepository();
          // Required to ensure branches with duplicate strings are displayed in the GUI
          if (toCreateTmTextUnits.isEmpty() && repository != null) {
            this.repositoryStatisticsJobScheduler.schedule(repository.getId());
          }

          ImmutableList<BranchStateTextUnit> createdTextUnits =
              createTmTextUnitsInTx(
                  assetContent.getAsset(),
                  toCreateTmTextUnits,
                  assetContent.getBranch().getCreatedByUser());

          ImmutableList<TextUnitDTOMatch> leveragingMatches =
              getLeveragingMatchesForTextUnits(
                  createdTextUnits, textUnitDTOsForAssetAndLocaleByMD5.values().asList());

          MultiBranchState stateWithCreatedIds =
              updateStateWithCreatedIds(stateForNewContentWithIds, createdTextUnits);

          return CreateTextUnitsResult.builder()
              .createdTextUnits(createdTextUnits)
              .leveragingMatches(leveragingMatches)
              .updatedState(stateWithCreatedIds)
              .build();
        });
  }

  MultiBranchState updateStateWithCreatedIds(
      MultiBranchState stateForNewContentWithIds,
      ImmutableList<BranchStateTextUnit> createdTextUnits) {
    ImmutableMap<String, BranchStateTextUnit> createdBranchStateTextUnitsByMd5 =
        createdTextUnits.stream()
            .collect(ImmutableMap.toImmutableMap(BranchStateTextUnit::getMd5, identity()));

    return stateForNewContentWithIds.withBranchStateTextUnits(
        stateForNewContentWithIds.getBranchStateTextUnits().stream()
            .map(
                bstu ->
                    Optional.ofNullable(createdBranchStateTextUnitsByMd5.get(bstu.getMd5()))
                        .map(m -> bstu.withTmTextUnitId(m.getTmTextUnitId()))
                        .orElse(bstu))
            .collect(ImmutableList.toImmutableList()));
  }

  Asset getUndeletedAsset(Asset asset) {
    if (asset.getDeleted()) {
      asset.setDeleted(false);
      asset = assetRepository.save(asset);
    }
    return asset;
  }

  /**
   * Make sure the asset has a last successful extraction and if not create it.
   *
   * <p>This is for legacy support edge cases. Once DB is updated it is not needed anymore since the
   * last successful extraction should be create in {@link
   * com.box.l10n.mojito.service.asset.AssetService#createAsset(Long, String, boolean)}
   *
   * @param asset
   * @return
   */
  AssetExtraction getOrCreateLastSuccessfulAssetExtraction(Asset asset) {
    AssetExtraction assetExtraction;

    if (asset.getLastSuccessfulAssetExtraction() != null) {
      assetExtraction = asset.getLastSuccessfulAssetExtraction();
    } else {
      assetExtraction = createLastSuccessfulAssetExtractionInAsset(asset);
    }
    return assetExtraction;
  }

  @Transactional
  AssetExtraction createLastSuccessfulAssetExtractionInAsset(Asset asset) {
    asset = assetRepository.findById(asset.getId()).get();
    AssetExtraction assetExtraction = new AssetExtraction();
    assetExtraction.setAsset(asset);
    assetExtraction.setPollableTask(null);
    assetExtraction = assetExtractionRepository.save(assetExtraction);

    asset.setLastSuccessfulAssetExtraction(assetExtraction);
    asset.setDeleted(false);
    assetRepository.save(asset);

    return assetExtraction;
  }

  /**
   * Gives a set of modifications to go from the base state to the current state.
   *
   * @param base
   * @param current
   * @return
   */
  Modifications getModifications(MultiBranchState base, MultiBranchState current) {

    ImmutableSet<String> unusedInBase =
        base.getBranchStateTextUnits().stream()
            .filter(e -> e.getBranchNameToBranchDatas().isEmpty())
            .map(BranchStateTextUnit::getMd5)
            .collect(ImmutableSet.toImmutableSet());

    ImmutableSet<String> unusedInCurrent =
        current.getBranchStateTextUnits().stream()
            .filter(e -> e.getBranchNameToBranchDatas().isEmpty())
            .map(BranchStateTextUnit::getMd5)
            .collect(ImmutableSet.toImmutableSet());

    ImmutableSet<String> usedInBase =
        base.getBranchStateTextUnits().stream()
            .filter(e -> !e.getBranchNameToBranchDatas().isEmpty())
            .map(BranchStateTextUnit::getMd5)
            .collect(ImmutableSet.toImmutableSet());

    ImmutableSet<String> usedInCurrent =
        current.getBranchStateTextUnits().stream()
            .filter(e -> !e.getBranchNameToBranchDatas().isEmpty())
            .map(BranchStateTextUnit::getMd5)
            .collect(ImmutableSet.toImmutableSet());

    Sets.SetView<String> removedMd5s = Sets.difference(unusedInCurrent, unusedInBase);
    ImmutableSet<BranchStateTextUnit> removed =
        current.getBranchStateTextUnits().stream()
            .filter(bstu -> removedMd5s.contains(bstu.getMd5()))
            .collect(ImmutableSet.toImmutableSet());
    logger.debug("removed: {}", removed);

    Sets.SetView<String> addedMd5s = Sets.difference(usedInCurrent, usedInBase);

    ImmutableSet<BranchStateTextUnit> added =
        current.getBranchStateTextUnits().stream()
            .filter(bstu -> addedMd5s.contains(bstu.getMd5()))
            .collect(ImmutableSet.toImmutableSet());
    logger.debug("added: {}", added);

    Sets.SetView<String> usedInBoth = Sets.intersection(usedInBase, usedInCurrent);

    // we look into entries that are used in both, and we try to update it

    ImmutableMap<String, Map.Entry<String, BranchData>> baseBothUsedBranchDataByMd5 =
        base.getBranchStateTextUnits().stream()
            .filter(bstu -> usedInBoth.contains(bstu.getMd5()))
            .collect(
                ImmutableMap.toImmutableMap(
                    BranchStateTextUnit::getMd5, this::getFirstBranchDataEntry));

    ImmutableSet<BranchStateTextUnit> updated =
        current.getBranchStateTextUnits().stream()
            .filter(bstu -> usedInBoth.contains(bstu.getMd5()))
            .map(
                bstu -> {
                  Map.Entry<String, BranchData> baseBranchDataEntry =
                      baseBothUsedBranchDataByMd5.get(bstu.getMd5());
                  Map.Entry<String, BranchData> firstBranchDataEntry =
                      getFirstBranchDataEntry(bstu);

                  if (!firstBranchDataEntry.getKey().equals(baseBranchDataEntry.getKey())
                      || !firstBranchDataEntry.getValue().equals(baseBranchDataEntry.getValue())) {
                    return bstu;
                  } else {
                    return null;
                  }
                })
            .filter(Objects::nonNull)
            .collect(ImmutableSet.toImmutableSet());

    logger.debug("updated: {}", updated);

    Modifications modifications =
        Modifications.builder().removed(removed).added(added).updated(updated).build();

    return modifications;
  }

  private Sets.SetView<String> getMd5sForUsedInBoth(
      ImmutableSet<BranchStateTextUnit> usedInBase,
      ImmutableSet<BranchStateTextUnit> usedInCurrent) {
    ImmutableSet<String> md5UsedInBase =
        usedInBase.stream().map(BranchStateTextUnit::getMd5).collect(ImmutableSet.toImmutableSet());

    ImmutableSet<String> md5UsedInCurrent =
        usedInCurrent.stream()
            .map(BranchStateTextUnit::getMd5)
            .collect(ImmutableSet.toImmutableSet());

    return Sets.intersection(md5UsedInBase, md5UsedInCurrent);
  }

  void removeAssetTextUnits(
      AssetExtraction assetExtraction,
      ImmutableSet<BranchStateTextUnit> branchStateTextUnitsToRemove) {
    ImmutableList<Long> assetTextUnitIdsToRemove =
        branchStateTextUnitsToRemove.stream()
            .map(BranchStateTextUnit::getAssetTextUnitId)
            .collect(ImmutableList.toImmutableList());

    Lists.partition(assetTextUnitIdsToRemove, BATCH_SIZE)
        .forEach(
            batchAssetTextUnitIdsToRemove -> {
              assetTextUnitToTMTextUnitRepository.deleteByAssetTextUnitIdIn(
                  batchAssetTextUnitIdsToRemove);
              assetTextUnitRepository.deleteByIdIn(batchAssetTextUnitIdsToRemove);
            });
  }

  void updateAssetTextUnits(
      AssetExtraction assetExtraction, ImmutableSet<BranchStateTextUnit> toUpdate) {

    LoadingCache<String, Branch> branches = getBranchesCache(assetExtraction);

    ImmutableList<AssetTextUnit> assetTextUnits =
        Lists.partition(toUpdate.asList(), BATCH_SIZE).stream()
            .flatMap(updateAssetTextUnitsBatch(branches))
            .collect(ImmutableList.toImmutableList());

    logger.debug("Updated {} asset text units", assetTextUnits.size());
  }

  Function<List<BranchStateTextUnit>, Stream<AssetTextUnit>> updateAssetTextUnitsBatch(
      LoadingCache<String, Branch> branches) {

    return branchStateTextUnits -> {
      ImmutableList<Long> assetTextUnitIds =
          branchStateTextUnits.stream()
              .map(BranchStateTextUnit::getAssetTextUnitId)
              .collect(ImmutableList.toImmutableList());

      ImmutableMap<String, AssetTextUnit> assetTextUnitsByMd5 =
          assetTextUnitRepository.findByIdIn(assetTextUnitIds).stream()
              .collect(ImmutableMap.toImmutableMap(AssetTextUnit::getMd5, identity()));

      ImmutableList<AssetTextUnit> assetTextUnitsUpdated =
          branchStateTextUnits.stream()
              .map(
                  bstu -> {
                    Map.Entry<String, BranchData> firstBranchDataEntry =
                        getFirstBranchDataEntry(bstu);
                    Branch byNameAndRepository =
                        branches.getUnchecked(firstBranchDataEntry.getKey());
                    return Optional.of(assetTextUnitsByMd5.get(bstu.getMd5()))
                        .map(
                            assetTextUnit -> {
                              assetTextUnit.setBranch(byNameAndRepository);
                              assetTextUnit.setUsages(
                                  new HashSet<>(firstBranchDataEntry.getValue().getUsages()));
                              return assetTextUnitRepository.save(assetTextUnit);
                            });
                  })
              .filter(Optional::isPresent)
              .map(Optional::get)
              .collect(ImmutableList.toImmutableList());

      entityManager.flush();
      entityManager.clear();
      return assetTextUnitsUpdated.stream();
    };
  }

  ImmutableList<BranchStateTextUnit> createAssetTextUnits(
      AssetExtraction assetExtraction, ImmutableSet<BranchStateTextUnit> toAdd) {
    // when saving the last successful branch, we can have multiple branches. use a cache to fech
    // the information
    // only once per branch that is actually used.
    LoadingCache<String, Branch> branches = getBranchesCache(assetExtraction);

    ImmutableList<BranchStateTextUnit> branchStateTextUnitsWithAssetTextUnitId =
        Lists.partition(toAdd.asList(), BATCH_SIZE).stream()
            .flatMap(addAssetTextUnitsBatch(assetExtraction, branches))
            .collect(ImmutableList.toImmutableList());

    logger.debug("Added {} asset text units", branchStateTextUnitsWithAssetTextUnitId.size());
    return branchStateTextUnitsWithAssetTextUnitId;
  }

  Function<List<BranchStateTextUnit>, Stream<BranchStateTextUnit>> addAssetTextUnitsBatch(
      AssetExtraction assetExtraction, LoadingCache<String, Branch> branches) {
    return branchStateTextUnits -> {
      ImmutableList<BranchStateTextUnit> assetTextUnitToTMTextUnits =
          branchStateTextUnits.stream()
              .map(
                  bstu -> {
                    Map.Entry<String, BranchData> firstBranchDataEntry =
                        getFirstBranchDataEntry(bstu);
                    Branch byNameAndRepository =
                        branches.getUnchecked(firstBranchDataEntry.getKey());

                    logger.debug(
                        "Add asset text unit, extraction id: {} - name: {}",
                        assetExtraction.getId(),
                        bstu.getName());
                    AssetTextUnit assetTextUnit =
                        createAssetTextUnit(
                            assetExtraction.getId(),
                            bstu.getName(),
                            bstu.getSource(),
                            bstu.getComments(),
                            pluralFormService.findByPluralFormString(bstu.getPluralForm()),
                            bstu.getPluralFormOther(),
                            false,
                            ImmutableSet.copyOf(firstBranchDataEntry.getValue().getUsages()),
                            byNameAndRepository);

                    AssetTextUnitToTMTextUnit assetTextUnitToTMTextUnit =
                        new AssetTextUnitToTMTextUnit();
                    assetTextUnitToTMTextUnit.setAssetExtraction(assetExtraction);
                    assetTextUnitToTMTextUnit.setAssetTextUnit(assetTextUnit);
                    assetTextUnitToTMTextUnit.setTmTextUnit(
                        tmTextUnitRepository.getOne(bstu.getTmTextUnitId()));
                    assetTextUnitToTMTextUnitRepository.save(assetTextUnitToTMTextUnit);

                    return bstu.withAssetTextUnitId(assetTextUnit.getId());
                  })
              .collect(ImmutableList.toImmutableList());

      entityManager.flush();
      entityManager.clear();
      return assetTextUnitToTMTextUnits.stream();
    };
  }

  LoadingCache<String, Branch> getBranchesCache(AssetExtraction assetExtraction) {
    return CacheBuilder.newBuilder()
        .build(
            CacheLoader.from(
                name ->
                    branchRepository.findByNameAndRepository(
                        localBranchToEntityBranchConverter.localBranchNameToEntityBranchName(name),
                        assetExtraction.getAsset().getRepository())));
  }

  Map.Entry<String, BranchData> getFirstBranchDataEntry(BranchStateTextUnit textUnit) {
    return textUnit.getBranchNameToBranchDatas().entrySet().stream()
        .findFirst()
        .orElseThrow(
            () ->
                new RuntimeException(
                    "there must be at least one branch because of previous filtering"));
  }

  @Pollable(message = "Perform leveraging")
  void performLeveraging(
      ImmutableList<TextUnitDTOMatch> matchesForSourceLeveraging,
      @ParentTask PollableTask currentTask) {
    matchesForSourceLeveraging.stream()
        .forEach(
            match -> {
              LeveragerByTmTextUnit leveragerByTmTextUnit =
                  new LeveragerByTmTextUnit(match.getMatch().getTmTextUnitId());
              if (match.getSource().getTmTextUnitId() == null) {
                throw new RuntimeException(
                    "The source must be saved in the database when requesting leveraging");
              }
              TMTextUnit tmTextUnit =
                  tmTextUnitRepository.findById(match.getSource().getTmTextUnitId()).get();
              leveragerByTmTextUnit.performLeveragingFor(
                  new ArrayList<>(Arrays.asList(tmTextUnit)), null, null);
            });
  }

  ImmutableList<TextUnitDTOMatch> getLeveragingMatchesForTextUnits(
      ImmutableList<BranchStateTextUnit> textUnits, ImmutableList<TextUnitDTO> textUnitDTOs) {

    Predicate<TextUnitDTO> notInIdsOfTextUnits = isNotInIdsOfTextUnits(textUnits);
    ImmutableListMultimap<String, TextUnitDTO> candidateByNames =
        textUnitDTOs.stream()
            .filter(notInIdsOfTextUnits)
            .filter(isInNameToCreatePredicate(textUnits))
            .collect(
                ImmutableListMultimap.toImmutableListMultimap(TextUnitDTO::getName, identity()));

    ImmutableListMultimap<String, TextUnitDTO> candidateBySources =
        textUnitDTOs.stream()
            .filter(notInIdsOfTextUnits)
            .filter(isInSourceToCreatePredicate(textUnits))
            .collect(
                ImmutableListMultimap.toImmutableListMultimap(TextUnitDTO::getSource, identity()));

    return textUnits.stream()
        .map(
            bstu -> {
              // This mimimics the current source leveraging - the logic could be improved quite a
              // bit:
              // real detection of refactoring in diff command (if diff command not used probably
              // drop the match based name anyway)
              // coupled with sensible matching here
              TextUnitDTO match = null;
              boolean uniqueMatch = false;
              boolean translationNeededIfUniqueMatch = true;

              if (match == null) {
                ImmutableList<TextUnitDTO> byNameAndContentAndUsed =
                    candidateByNames.get(bstu.getMd5()).stream()
                        .filter(m -> m.getSource().equals(bstu.getSource()))
                        .filter(TextUnitDTO::isUsed)
                        .collect(ImmutableList.toImmutableList());

                match = byNameAndContentAndUsed.stream().findFirst().orElse(null);
                uniqueMatch = byNameAndContentAndUsed.size() == 1;
                translationNeededIfUniqueMatch = false;
              }

              if (match == null) {
                ImmutableList<TextUnitDTO> byNameAndUsed =
                    candidateByNames.get(bstu.getName()).stream()
                        .filter(TextUnitDTO::isUsed)
                        .collect(ImmutableList.toImmutableList());

                match = byNameAndUsed.stream().findFirst().orElse(null);
                uniqueMatch = byNameAndUsed.size() == 1;
                translationNeededIfUniqueMatch = true;
              }

              if (match == null) {
                ImmutableList<TextUnitDTO> byContent =
                    candidateBySources.get(bstu.getSource()).stream()
                        .filter(TextUnitDTO::isUsed)
                        .collect(ImmutableList.toImmutableList());

                match = byContent.stream().findFirst().orElse(null);
                uniqueMatch = byContent.size() == 1;
                translationNeededIfUniqueMatch = true;
              }

              if (match == null) {
                ImmutableList<TextUnitDTO> byNameAndContentAndUnused =
                    candidateByNames.get(bstu.getMd5()).stream()
                        .filter(m -> m.getSource().equals(bstu.getSource()))
                        .filter(Predicates.not(TextUnitDTO::isUsed))
                        .collect(ImmutableList.toImmutableList());

                match = byNameAndContentAndUnused.stream().findFirst().orElse(null);
                uniqueMatch = byNameAndContentAndUnused.size() == 1;
                translationNeededIfUniqueMatch = false;
              }

              return match == null
                  ? null
                  : TextUnitDTOMatch.builder()
                      .source(bstu)
                      .match(match)
                      .uniqueMatch(uniqueMatch)
                      .translationNeededIfUniqueMatch(translationNeededIfUniqueMatch)
                      .build();
            })
        .filter(Objects::nonNull)
        .collect(ImmutableList.toImmutableList());
  }

  Predicate<TextUnitDTO> isNotInIdsOfTextUnits(ImmutableList<BranchStateTextUnit> textUnits) {
    ImmutableSet<Long> idsToCreate =
        textUnits.stream()
            .map(BranchStateTextUnit::getTmTextUnitId)
            .collect(ImmutableSet.toImmutableSet());

    return textUnitDTO -> !idsToCreate.contains(textUnitDTO.getTmTextUnitId());
  }

  Predicate<TextUnitDTO> isInSourceToCreatePredicate(
      ImmutableList<BranchStateTextUnit> textUnitsToCreate) {
    ImmutableSet<String> sourcesToCreate =
        textUnitsToCreate.stream()
            .map(BranchStateTextUnit::getSource)
            .collect(ImmutableSet.toImmutableSet());

    return bstu -> sourcesToCreate.contains(bstu.getSource());
  }

  Predicate<TextUnitDTO> isInNameToCreatePredicate(
      ImmutableList<BranchStateTextUnit> textUnitsToCreates) {

    ImmutableSet<String> namesToCreate =
        textUnitsToCreates.stream()
            .map(BranchStateTextUnit::getName)
            .collect(ImmutableSet.toImmutableSet());

    return tu -> namesToCreate.contains(tu.getName());
  }

  Predicate<BranchStateTextUnit> usedBranchStateTextUnit() {
    return m -> !m.getBranchNameToBranchDatas().isEmpty();
  }

  Predicate<BranchStateTextUnit> unusedBranchStateTextUnit() {
    return m -> m.getBranchNameToBranchDatas().isEmpty();
  }

  /** Returns text units without ids (= tm text unit ids). */
  ImmutableList<BranchStateTextUnit> getBranchStateTextUnitsWithoutId(
      MultiBranchState multiBranchState) {
    return multiBranchState.getBranchStateTextUnits().stream()
        .filter(bstu -> bstu.getTmTextUnitId() == null)
        .collect(ImmutableList.toImmutableList());
  }

  @Transactional
  protected ImmutableList<BranchStateTextUnit> createTmTextUnitsInTx(
      Asset asset, ImmutableList<BranchStateTextUnit> textUnits, User createdByUser) {
    logger.debug(
        "Create TMTextUnits, tmId: {}, assetId: {}",
        asset.getRepository().getTm().getId(),
        asset.getId());

    ZonedDateTime createdDate = ZonedDateTime.now();

    ImmutableList<BranchStateTextUnit> createdTmTextUnits =
        Lists.partition(textUnits, BATCH_SIZE).stream()
            .flatMap(createTmTextUnitsBatch(asset, createdByUser, createdDate))
            .collect(ImmutableList.toImmutableList());

    return createdTmTextUnits;
  }

  Function<List<BranchStateTextUnit>, Stream<? extends BranchStateTextUnit>> createTmTextUnitsBatch(
      Asset asset, User createdByUser, ZonedDateTime createdDate) {
    return textUnits -> {
      ImmutableList<BranchStateTextUnit> subCreatedTmTextUnits =
          textUnits.stream()
              .map(
                  bstu -> {
                    TMTextUnit addTMTextUnit =
                        tmService.addTMTextUnit(
                            asset.getRepository().getTm(),
                            asset,
                            bstu.getName(),
                            bstu.getSource(),
                            bstu.getComments(),
                            createdByUser,
                            createdDate,
                            pluralFormService.findByPluralFormString(bstu.getPluralForm()),
                            bstu.getPluralFormOther());
                    return bstu.withTmTextUnitId(addTMTextUnit.getId());
                  })
              .collect(ImmutableList.toImmutableList());

      entityManager.flush();
      entityManager.clear();
      return subCreatedTmTextUnits.stream();
    };
  }

  List<AssetExtractorTextUnit> getExtractorTextUnitsForAssetContent(
      AssetContent assetContent,
      List<String> filterOptions,
      FilterConfigIdOverride filterConfigIdOverride)
      throws UnsupportedAssetFilterTypeException {
    List<AssetExtractorTextUnit> assetExtractorTextUnits;

    if (assetContent.isExtractedContent()) {
      assetExtractorTextUnits =
          objectMapper.readValueUnchecked(
              assetContent.getContent(), new TypeReference<List<AssetExtractorTextUnit>>() {});
      assetExtractorTextUnits = assetExtractorTextUnits.stream().collect(Collectors.toList());

    } else {
      assetExtractorTextUnits =
          assetExtractor.getAssetExtractorTextUnitsForAsset(
              assetContent.getAsset().getPath(),
              assetContent.getContent(),
              filterConfigIdOverride,
              filterOptions);
    }
    return assetExtractorTextUnits;
  }

  @Pollable(message = "Extracting text units from asset")
  public MultiBranchState convertAssetContentToMultiBranchState(
      AssetContent assetContent,
      FilterConfigIdOverride filterConfigIdOverride,
      List<String> filterOptions,
      @ParentTask PollableTask parentTask)
      throws UnsupportedAssetFilterTypeException {

    List<AssetExtractorTextUnit> assetExtractorTextUnits =
        getExtractorTextUnitsForAssetContent(assetContent, filterOptions, filterConfigIdOverride);
    com.box.l10n.mojito.localtm.merger.Branch branch = convertBranchToLocalTmBranch(assetContent);
    MultiBranchState multiBranchState =
        assetExtractorTextUnitsToMultiBranchStateConverter.convert(assetExtractorTextUnits, branch);
    return multiBranchState;
  }

  com.box.l10n.mojito.localtm.merger.Branch convertBranchToLocalTmBranch(
      AssetContent assetContent) {
    return com.box.l10n.mojito.localtm.merger.Branch.builder()
        .name(
            localBranchToEntityBranchConverter.branchEntityToLocalBranchName(
                assetContent.getBranch()))
        .createdAt(assetContent.getBranch().getCreatedDate())
        .build();
  }

  AssetExtractionByBranch getUndeletedOrCreateAssetExtractionByBranch(AssetContent assetContent) {
    logger.debug("getUndeletedOrCreateAssetExtractionByBranch");
    return retryTemplate.execute(
        context -> {
          return getAssetExtractionByBranch(assetContent.getAsset(), assetContent.getBranch())
              .map(
                  aebb -> {
                    if (aebb.getDeleted()) {
                      aebb.setDeleted(false);
                      aebb = assetExtractionByBranchRepository.save(aebb);
                    }
                    return aebb;
                  })
              .orElseGet(() -> createAssetExtractionForBranch(assetContent));
        });
  }

  @Transactional
  AssetExtractionByBranch createAssetExtractionForBranch(AssetContent assetContent) {
    Asset asset = assetContent.getAsset();
    Branch branch = assetContent.getBranch();

    logger.debug(
        "createAssetExtractionForBranch, asset id: {} and branch name: {}",
        asset.getId(),
        branch.getName());
    AssetExtractionByBranch aebb = new AssetExtractionByBranch();
    aebb.setAsset(asset);
    aebb.setBranch(branch);

    AssetExtraction assetExtraction = new AssetExtraction();
    assetExtraction.setAsset(asset);
    assetExtraction.setContentMd5(assetContent.getContentMd5());
    assetExtraction.setCreatedByUser(assetContent.getBranch().getCreatedByUser());
    assetExtraction = assetExtractionRepository.save(assetExtraction);

    aebb.setAssetExtraction(assetExtraction);
    return assetExtractionByBranchRepository.save(aebb);
  }

  Optional<AssetExtractionByBranch> getAssetExtractionByBranch(Asset asset, Branch branch) {
    return assetExtractionByBranchRepository.findByAssetAndBranch(asset, branch);
  }

  public void deleteAssetBranch(Asset asset, String branchName) {

    Branch branch = branchRepository.findByNameAndRepository(branchName, asset.getRepository());
    AssetExtractionByBranch assetExtractionByBranch =
        getAssetExtractionByBranch(asset, branch)
            .orElseThrow(
                () -> new RuntimeException("There must be an assetExtractionByBranch for delete"));

    retryTemplate.execute(
        context -> {
          if (context.getRetryCount() > 0) {
            logger.info(
                "Assume concurrent modification when deleting asset branch. Attempt: {}",
                context.getRetryCount());
          }

          MultiBranchState lastSuccessfulMultiBranchState =
              multiBranchStateService.getMultiBranchStateForAssetExtractionId(
                  asset.getLastSuccessfulAssetExtraction().getId(),
                  asset.getLastSuccessfulAssetExtraction().getVersion());

          MultiBranchState withBranchRemoved =
              multiBranchStateMerger.removeBranch(
                  lastSuccessfulMultiBranchState,
                  localBranchToEntityBranchConverter.entityBranchNameToLocalBranchName(branchName));

          Modifications modifications =
              getModifications(lastSuccessfulMultiBranchState, withBranchRemoved);
          deleteAssetBranchInTx(
              asset, modifications, withBranchRemoved, assetExtractionByBranch, context);
          return null;
        });
  }

  @Transactional
  void deleteAssetBranchInTx(
      Asset asset,
      Modifications modifications,
      MultiBranchState withBranchRemoved,
      AssetExtractionByBranch assetExtractionByBranch,
      RetryContext context) {
    updateAssetExtractionWithStateInTx(
        asset.getLastSuccessfulAssetExtraction(),
        withBranchRemoved,
        modifications,
        AssetContentMd5s.of());
    assetExtractionByBranch.setDeleted(true);
    assetExtractionByBranchRepository.save(assetExtractionByBranch);
  }

  public PollableFuture<Void> processAssetAsync(
      Long assetContentId,
      Long pushRunId,
      FilterConfigIdOverride filterConfigIdOverride,
      List<String> filterOptions,
      Long parentTaskId)
      throws UnsupportedAssetFilterTypeException,
          InterruptedException,
          AssetExtractionConflictException {

    ProcessAssetJobInput processAssetJobInput = new ProcessAssetJobInput();
    processAssetJobInput.setAssetContentId(assetContentId);
    processAssetJobInput.setPushRunId(pushRunId);
    processAssetJobInput.setFilterConfigIdOverride(filterConfigIdOverride);
    processAssetJobInput.setFilterOptions(filterOptions);

    String pollableMessage =
        MessageFormat.format("Process asset content, id: {0}", assetContentId.toString());

    QuartzJobInfo<ProcessAssetJobInput, Void> quartzJobInfo =
        QuartzJobInfo.newBuilder(ProcessAssetJob.class)
            .withInput(processAssetJobInput)
            .withMessage(pollableMessage)
            .withParentId(parentTaskId)
            .withExpectedSubTaskNumber(5)
            .withScheduler(quartzSchedulerName)
            .build();

    return quartzPollableTaskScheduler.scheduleJob(quartzJobInfo);
  }

  /**
   * Mark the asset extraction as last successful.
   *
   * <p>Make the function {@link Retryable} since this can have concurrent access issue when
   * multiple thread trying to update update the asset at the same time. It is not important which
   * one wins last since in real usage that should not really happen, it is an edge case and the
   * code just need to be safe.
   *
   * <p>This is now only used by tests. Eventually it could be replaced.
   *
   * @param asset The asset to update
   * @param assetExtraction The asset extraction to mark as last successful
   */
  @Retryable
  public void markAssetExtractionAsLastSuccessful(Asset asset, AssetExtraction assetExtraction) {
    logger.debug(
        "Marking asset extraction as last successful, assetExtractionId: {}",
        assetExtraction.getId());
    asset.setLastSuccessfulAssetExtraction(assetExtraction);
    asset.setDeleted(false);
    assetRepository.save(asset);
  }

  @Transactional
  public AssetExtraction createAssetExtraction(Asset asset, PollableTask pollableTask) {
    AssetExtraction assetExtraction = new AssetExtraction();
    assetExtraction.setAsset(asset);
    assetExtraction.setPollableTask(pollableTask);
    assetExtraction = assetExtractionRepository.save(assetExtraction);
    return assetExtraction;
  }

  /**
   * Creates a new AssetTextUnit, and associate it to the given asset extraction.
   *
   * @param assetExtraction the assetExtraction object the TextUnit comes from (must be valid)
   * @param name Name of the TextUnit
   * @param content Content of the TextUnit
   * @param comment Comment for the TextUnit
   * @return The created AssetTextUnit
   */
  public AssetTextUnit createAssetTextUnit(
      AssetExtraction assetExtraction, String name, String content, String comment) {
    Branch branch =
        assetExtraction.getAssetContent() != null
            ? assetExtraction.getAssetContent().getBranch()
            : null;
    return createAssetTextUnit(
        assetExtraction.getId(), name, content, comment, null, null, false, null, branch);
  }

  /**
   * Creates a new AssetTextUnit, and associate it to the given asset extraction.
   *
   * @param assetExtractionId ID of the assetExtraction object the TextUnit comes from (must be
   *     valid)
   * @param name Name of the TextUnit
   * @param content Content of the TextUnit
   * @param comment Comment for the TextUnit
   * @param pluralForm optional plural form
   * @param pluralFormOther optional other plural form
   * @param doNotTranslate to indicate if the TextUnit should be translated
   * @param usages optional usages in the code source
   * @param branch
   * @return The created AssetTextUnit
   */
  @Transactional
  public AssetTextUnit createAssetTextUnit(
      Long assetExtractionId,
      String name,
      String content,
      String comment,
      PluralForm pluralForm,
      String pluralFormOther,
      boolean doNotTranslate,
      Set<String> usages,
      Branch branch) {

    logger.debug(
        "Adding AssetTextUnit for assetExtractionId: {}\nname: {}\ncontent: {}\ncomment: {}\n",
        assetExtractionId,
        name,
        content,
        comment);

    AssetTextUnit assetTextUnit = new AssetTextUnit();
    assetTextUnit.setAssetExtraction(assetExtractionRepository.getOne(assetExtractionId));
    assetTextUnit.setName(name);
    assetTextUnit.setContent(content);
    assetTextUnit.setComment(comment);
    assetTextUnit.setMd5(textUnitUtils.computeTextUnitMD5(name, content, comment));
    assetTextUnit.setContentMd5(DigestUtils.md5Hex(content));
    assetTextUnit.setPluralForm(pluralForm);
    assetTextUnit.setPluralFormOther(pluralFormOther);
    assetTextUnit.setDoNotTranslate(doNotTranslate);
    assetTextUnit.setUsages(usages);
    assetTextUnit.setBranch(branch);

    assetTextUnitRepository.save(assetTextUnit);

    logger.trace("AssetTextUnit saved");

    return assetTextUnit;
  }
}
