package com.box.l10n.mojito.service.asset;

import static com.box.l10n.mojito.quartz.QuartzSchedulerManager.DEFAULT_SCHEDULER_NAME;
import static com.box.l10n.mojito.rest.asset.AssetSpecification.branchId;
import static com.box.l10n.mojito.rest.asset.AssetSpecification.deletedEquals;
import static com.box.l10n.mojito.rest.asset.AssetSpecification.pathEquals;
import static com.box.l10n.mojito.rest.asset.AssetSpecification.repositoryIdEquals;
import static com.box.l10n.mojito.rest.asset.AssetSpecification.virtualEquals;
import static com.box.l10n.mojito.specification.Specifications.distinct;
import static com.box.l10n.mojito.specification.Specifications.ifParamNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetContent;
import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.AssetExtractionByBranch;
import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.okapi.FilterConfigIdOverride;
import com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.security.AuditorAwareImpl;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionByBranchRepository;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionRepository;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import com.box.l10n.mojito.service.assetcontent.AssetContentService;
import com.box.l10n.mojito.service.branch.BranchRepository;
import com.box.l10n.mojito.service.branch.BranchService;
import com.box.l10n.mojito.service.pollableTask.InjectCurrentTask;
import com.box.l10n.mojito.service.pollableTask.MsgArg;
import com.box.l10n.mojito.service.pollableTask.ParentTask;
import com.box.l10n.mojito.service.pollableTask.Pollable;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableFutureTaskResult;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.security.user.UserService;
import com.google.common.base.Joiner;
import java.text.MessageFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to manage assets. It takes care of adding new assets as well as starting the extraction
 * of {@link AssetTextUnit}s from the asset.
 *
 * @author aloison
 */
@Service
public class AssetService {

  /** logger */
  static Logger logger = getLogger(AssetService.class);

  @Autowired AssetRepository assetRepository;

  @Autowired AssetExtractionByBranchRepository assetExtractionByBranchRepository;

  @Autowired AssetExtractionRepository assetExtractionRepository;

  @Autowired AssetExtractionService assetExtractionService;

  @Autowired AssetContentService assetContentService;

  @Autowired BranchRepository branchRepository;

  @Autowired BranchService branchService;

  @Autowired RepositoryRepository repositoryRepository;

  @Autowired PollableTaskService pollableTaskService;

  @Autowired UserService userService;

  @Autowired AuditorAwareImpl auditorAware;

  @Autowired FilterOptionsMd5Builder filterOptionsMd5Builder;

  @Autowired QuartzPollableTaskScheduler quartzPollableTaskScheduler;

  @Value("${l10n.assetService.quartz.schedulerName:" + DEFAULT_SCHEDULER_NAME + "}")
  String schedulerName;

  /**
   * Adds an {@link Asset} to a {@link Repository}.
   *
   * <p>The asset is added only if an asset with the same path does not already exist. After the
   * asset is added, it starts the extraction job to get text units.
   *
   * @param repositoryId {@link Repository#id} of the repository that will contain the asset
   * @param assetPath Remote path of the asset
   * @param assetContent Content of the asset
   * @param extractedContent
   * @param branch
   * @param filterConfigIdOverride Optional, can be null. Allows to specify a specific Okapi filter
   *     to use to process the asset
   * @param branchNotifierIds
   * @return The created asset
   * @throws ExecutionException
   * @throws InterruptedException
   */
  public PollableFuture<Asset> addOrUpdateAssetAndProcessIfNeeded(
      Long repositoryId,
      String assetPath,
      String assetContent,
      boolean extractedContent,
      String branch,
      String branchCreatedByUsername,
      Set<String> branchNotifierIds,
      Long pushRunId,
      FilterConfigIdOverride filterConfigIdOverride,
      List<String> filterOptions)
      throws ExecutionException, InterruptedException, UnsupportedAssetFilterTypeException {
    return addOrUpdateAssetAndProcessIfNeeded(
        repositoryId,
        assetPath,
        assetContent,
        extractedContent,
        branch,
        branchCreatedByUsername,
        branchNotifierIds,
        pushRunId,
        filterConfigIdOverride,
        filterOptions,
        PollableTask.INJECT_CURRENT_TASK);
  }

  /**
   * See {@link AssetService#addOrUpdateAssetAndProcessIfNeeded(Long, String, String)}
   *
   * @param branch
   * @param repositoryId {@link Repository#id} of the repository that will contain the asset
   * @param assetPath Remote path of the asset
   * @param assetContent Content of the asset
   * @param extractedContent
   * @param currentTask The current task, injected
   * @return The created asset
   * @throws ExecutionException
   * @throws InterruptedException
   */
  @Pollable(expectedSubTaskNumber = 2)
  private PollableFuture<Asset> addOrUpdateAssetAndProcessIfNeeded(
      Long repositoryId,
      String assetPath,
      String assetContent,
      boolean extractedContent,
      String branchName,
      String branchCreatedByUsername,
      Set<String> branchNotifierIds,
      Long pushRunId,
      FilterConfigIdOverride filterConfigIdOverride,
      List<String> filterOptions,
      @InjectCurrentTask PollableTask currentTask)
      throws InterruptedException, ExecutionException, UnsupportedAssetFilterTypeException {

    PollableFutureTaskResult<Asset> pollableFutureTaskResult = new PollableFutureTaskResult<>();

    logger.debug("Process asset: {}, branch: {}", assetPath, branchName);

    Asset asset = assetRepository.findByPathAndRepositoryId(assetPath, repositoryId);

    if (asset != null && Boolean.TRUE.equals(asset.getVirtual())) {
      throw new AssetUpdateException("Update on an Asset can't be performed on virtual asset");
    }

    if (asset == null) {
      logger.debug(
          "createAssetWithPollable, repositoryId: {}, assetPath: {}", repositoryId, assetPath);
      asset = createAssetWithPollable(repositoryId, assetPath, currentTask);
    } else {
      logger.debug(
          "Asset already exists, repositoryId: {}, assetPath: {}, decrease number of sub tasks",
          repositoryId,
          assetPath);
      pollableFutureTaskResult.setExpectedSubTaskNumberOverride(1);
    }

    User branchCreatedByUser =
        branchCreatedByUsername != null
            ? userService.getOrCreatePartialBasicUser(branchCreatedByUsername)
            : auditorAware.getCurrentAuditor().orElse(null);
    logger.debug("Branch created by username: {}", branchCreatedByUser.getUsername());

    Branch branch =
        branchService.getUndeletedOrCreateBranch(
            asset.getRepository(), branchName, branchCreatedByUser, branchNotifierIds);

    AssetExtractionByBranch assetExtractionByBranch =
        assetExtractionByBranchRepository.findByAssetAndBranch(asset, branch).orElse(null);

    if (isAssetProcessingNeeded(assetExtractionByBranch, assetContent, filterOptions, pushRunId)) {
      AssetContent assetContentEntity =
          assetContentService.createAssetContent(asset, assetContent, extractedContent, branch);
      assetExtractionService.processAssetAsync(
          assetContentEntity.getId(),
          pushRunId,
          filterConfigIdOverride,
          filterOptions,
          currentTask.getId());
    } else {
      logger.debug(
          "Asset ({}) processing not needed. Reset number of expected sub task to 0", assetPath);
      pollableFutureTaskResult.setExpectedSubTaskNumberOverride(0);
      pollableFutureTaskResult.setMessageOverride("Asset content has not changed");
    }

    pollableFutureTaskResult.setResult(asset);
    return pollableFutureTaskResult;
  }

  /**
   * Must be done in transaction to be sure that there is a last successful asset extraction in the
   * asset.
   *
   * @param repositoryId
   * @param assetPath
   * @param virtualContent
   * @return
   */
  @Transactional
  public Asset createAsset(Long repositoryId, String assetPath, boolean virtualContent) {
    logger.debug("Create assest for repository id: {}, path: {}", repositoryId, assetPath);

    Asset asset = new Asset();

    Repository repository = repositoryRepository.findById(repositoryId).orElse(null);

    asset.setRepository(repository);
    asset.setVirtual(virtualContent);
    asset.setPath(assetPath);

    asset = assetRepository.save(asset);

    AssetExtraction assetExtraction = new AssetExtraction();
    assetExtraction.setAsset(asset);
    assetExtraction = assetExtractionRepository.save(assetExtraction);

    asset.setLastSuccessfulAssetExtraction(assetExtraction);
    asset = assetRepository.save(asset);

    return asset;
  }

  /**
   * Creates an {@link Asset}. Note that this function does not check if an item with the same
   * content already exists. It also does not process the asset.
   *
   * @param repositoryId {@link Repository#id} of the repository that will contain the asset
   * @param assetPath Remote path of the asset
   * @param assetContent Content of the asset
   * @return The created asset
   */
  public Asset createAssetWithContent(Long repositoryId, String assetPath, String assetContent) {
    Asset asset = createAsset(repositoryId, assetPath, false);
    assetContentService.createAssetContent(asset, assetContent);
    return asset;
  }

  /**
   * See {@link AssetService#createAssetWithContent(Long, String, String)}
   *
   * @param repositoryId {@link Repository#id} of the repository that will contain the asset
   * @param assetContent Content of the asset
   * @param assetPath Remote path of the assetTMServiceTest
   * @param parentTask The parent task to be updated
   * @return The created asset
   */
  @Pollable(message = "Creating asset: {assetPath}")
  @Transactional
  private Asset createAssetWithPollable(
      Long repositoryId,
      @MsgArg(name = "assetPath") String assetPath,
      @ParentTask PollableTask parentTask) {

    return createAsset(repositoryId, assetPath, Boolean.FALSE);
  }

  /**
   * Indicates if an asset needs to be updated.
   *
   * <p>First compare the asset content with the new content and the options used for the
   * extraction. If the contents are the same check that the last successful extraction correspond
   * to the latest version of the asset. If not it means an issue happen before and that regardless
   * of the content being the same, the asset needs to updated and reprocessed.
   *
   * @param assetContent The asset to be compared
   * @param optionsMd5
   * @param newAssetContent The content to be compared
   * @param pushRunId
   * @return true if the content of the asset is different from the new content, false otherwise
   */
  private boolean isAssetProcessingNeeded(
      AssetExtractionByBranch assetExtractionByBranch,
      String newAssetContent,
      List<String> newFilterOptions,
      Long pushRunId) {

    boolean assetProcessingNeeded = false;

    if (assetExtractionByBranch == null) {
      logger.debug("No active asset extraction, processing needed");
      assetProcessingNeeded = true;
    } else if (assetExtractionByBranch.getDeleted()) {
      logger.debug("Asset extraction deleted, processing needed");
      assetProcessingNeeded = true;
    } else if (!DigestUtils.md5Hex(newAssetContent)
        .equals(assetExtractionByBranch.getAssetExtraction().getContentMd5())) {
      logger.debug("Content has changed, processing needed");
      assetProcessingNeeded = true;
    } else if (!filterOptionsMd5Builder
        .md5(newFilterOptions)
        .equals(assetExtractionByBranch.getAssetExtraction().getFilterOptionsMd5())) {
      logger.debug("filter options have changed, processing needed");
      assetProcessingNeeded = true;
    } else if (pushRunId != null) {
      logger.debug("PushRun capture is enabled, processing needed.");
      assetProcessingNeeded = true;
    } else {
      logger.debug("Asset processing not needed");
    }

    return assetProcessingNeeded;
  }

  /**
   * Deletes an {@link Asset} by the {@link Asset#id}. It performs logical delete.
   *
   * @param asset
   */
  @Transactional
  public void deleteAsset(Asset asset) {
    logger.debug("Delete an asset with path: {}", asset.getPath());

    asset.setDeleted(true);
    assetRepository.save(asset);

    int numberOfAssetExtractionByBranchUpdated =
        assetExtractionByBranchRepository.setDeletedTrue(asset);

    // when this is marked as deleted, we need to recompute the global extraction

    logger.debug(
        "Deleted asset with path: {} (Number of AssetExtractionByBranchUpdated: {})",
        asset.getPath(),
        numberOfAssetExtractionByBranchUpdated);
  }

  /**
   * Deletes multiple {@link Asset} by the list of {@link Asset#id}
   *
   * @param assetIds
   */
  @Transactional
  public void deleteAssets(Set<Long> assetIds) {

    logger.debug("Delete assets {}", assetIds.toString());

    for (Long assetId : assetIds) {
      Asset asset = assetRepository.findById(assetId).orElse(null);
      if (asset != null) {
        deleteAsset(asset);
      }
    }

    logger.debug("Deleted assets {}", assetIds.toString());
  }

  public PollableFuture<Void> asyncDeleteAssetsOfBranch(Set<Long> assetIds, Long branchId) {
    DeleteAssetsOfBranchJobInput deleteAssetsOfBranchJobInput = new DeleteAssetsOfBranchJobInput();
    deleteAssetsOfBranchJobInput.setAssetIds(assetIds);
    deleteAssetsOfBranchJobInput.setBranchId(branchId);
    String pollableMessage =
        MessageFormat.format(
            " - Delete assetIds: [{0}] in branch: {1}",
            Joiner.on(",").join(assetIds), branchId.toString());

    QuartzJobInfo<DeleteAssetsOfBranchJobInput, Void> quartzJobInfo =
        QuartzJobInfo.newBuilder(DeleteAssetsOfBranchJob.class)
            .withInput(deleteAssetsOfBranchJobInput)
            .withMessage(pollableMessage)
            .withScheduler(schedulerName)
            .build();
    return quartzPollableTaskScheduler.scheduleJob(quartzJobInfo);
  }

  public void deleteAssetsOfBranch(Set<Long> assetIds, Long branchId) {
    if (logger.isDebugEnabled()) {
      logger.debug("Delete assets {} for branch id: {}", assetIds.toString(), branchId);
    }

    for (Long assetId : assetIds) {
      deleteAssetOfBranch(assetId, branchId);
    }

    logger.debug("Deleted assets {} for branch id: {}", assetIds.toString(), branchId);
  }

  public void deleteAssetOfBranch(Long assetId, Long branchId) {

    logger.debug("deleteAssetOfBranch: asset id: {}, branch id: {}", assetId, branchId);
    Asset asset = assetRepository.findById(assetId).orElse(null);
    if (asset != null) {
      Branch branch = branchRepository.findById(branchId).orElse(null);
      AssetExtractionByBranch assetExtractionByBranch =
          assetExtractionByBranchRepository.findByAssetAndBranch(asset, branch).orElse(null);

      if (assetExtractionByBranch == null) {
        logger.debug(
            "Asset extraction for asset: {} and branch: {} doesn't exist",
            asset.getPath(),
            branchId);
      } else {
        if (assetExtractionByBranch.getDeleted()) {
          logger.debug(
              "Asset extraction for asset: {} and branch: {} already deleted",
              asset.getPath(),
              branchId);
        } else {
          logger.debug(
              "Delete asset branch for asset: {} and branch: {} as deleted",
              asset.getPath(),
              branchId);
          assetExtractionService.deleteAssetBranch(asset, branch.getName());
        }
      }

      logger.debug("Check if the asset must be deleted");
      List<AssetExtractionByBranch> assetExtractionByBranches =
          assetExtractionByBranchRepository.findByAssetAndDeletedFalse(asset);

      if (assetExtractionByBranches.isEmpty()) {
        logger.debug("All branch are deleted or removed, delete asset");
        deleteAsset(asset);
      }
    }
  }

  public List<Asset> findAll(
      Long repositoryId, String path, Boolean deleted, Boolean virtual, Long branchId) {

    logger.debug(
        "Find all assets for repositoryId: {}, path: {}, deleted: {}, virtual: {}, branchId: {}",
        repositoryId,
        path,
        deleted,
        virtual,
        branchId);

    Specification<Asset> assetSpecifications =
        distinct(ifParamNotNull(repositoryIdEquals(repositoryId)))
            .and(ifParamNotNull(pathEquals(path)))
            .and(ifParamNotNull(deletedEquals(deleted)))
            .and(ifParamNotNull(virtualEquals(virtual)))
            .and(ifParamNotNull(branchId(branchId, deleted)));

    List<Asset> all = assetRepository.findAll(assetSpecifications);
    return all;
  }

  public Set<Long> findAllAssetIds(
      Long repositoryId, String path, Boolean deleted, Boolean virtual, Long branchId) {
    return findAll(repositoryId, path, deleted, virtual, branchId).stream()
        .map(Asset::getId)
        .collect(Collectors.toSet());
  }
}
