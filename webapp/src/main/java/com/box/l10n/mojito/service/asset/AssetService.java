package com.box.l10n.mojito.service.asset;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetContent;
import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.AssetExtractionByBranch;
import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.rest.asset.FilterConfigIdOverride;
import com.box.l10n.mojito.security.AuditorAwareImpl;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionByBranchRepository;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import com.box.l10n.mojito.service.assetExtraction.extractor.UnsupportedAssetFilterTypeException;
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
import com.google.common.base.Preconditions;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static com.box.l10n.mojito.rest.asset.AssetSpecification.*;
import static com.box.l10n.mojito.specification.Specifications.distinct;
import static com.box.l10n.mojito.specification.Specifications.ifParamNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Service to manage assets. It takes care of adding new assets as well as
 * starting the extraction of {@link AssetTextUnit}s from the asset.
 *
 * @author aloison
 */
@Service
public class AssetService {

    /**
     * logger
     */
    static Logger logger = getLogger(AssetService.class);

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    AssetExtractionByBranchRepository assetExtractionByBranchRepository;

    @Autowired
    AssetExtractionService assetExtractionService;

    @Autowired
    AssetContentService assetContentService;

    @Autowired
    BranchRepository branchRepository;

    @Autowired
    BranchService branchService;

    @Autowired
    RepositoryRepository repositoryRepository;

    @Autowired
    PollableTaskService pollableTaskService;

    @Autowired
    UserService userService;

    @Autowired
    AuditorAwareImpl auditorAware;

    /**
     * Adds an {@link Asset} to a {@link Repository}.
     * <p/>
     * The asset is added only if an asset with the same path does not already
     * exist. After the asset is added, it starts the extraction job to get text
     * units.
     *
     * @param repositoryId           {@link Repository#id} of the repository that will
     *                               contain the asset
     * @param assetContent           Content of the asset
     * @param assetPath              Remote path of the asset
     * @param branch
     * @param filterConfigIdOverride Optional, can be null. Allows to specify a
     *                               specific Okapi filter to use to process the asset
     * @return The created asset
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public PollableFuture<Asset> addOrUpdateAssetAndProcessIfNeeded(
            Long repositoryId,
            String assetContent,
            String assetPath,
            String branch,
            String branchCreatedByUsername,
            FilterConfigIdOverride filterConfigIdOverride) throws ExecutionException, InterruptedException, UnsupportedAssetFilterTypeException {
        return addOrUpdateAssetAndProcessIfNeeded(repositoryId, assetContent, assetPath, branch, branchCreatedByUsername, filterConfigIdOverride, PollableTask.INJECT_CURRENT_TASK);
    }

    /**
     * See
     * {@link AssetService#addOrUpdateAssetAndProcessIfNeeded(Long, String, String)}
     *
     * @param repositoryId {@link Repository#id} of the repository that will
     *                     contain the asset
     * @param assetContent Content of the asset
     * @param assetPath    Remote path of the asset
     * @param branch
     * @param currentTask  The current task, injected
     * @return The created asset
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Pollable(expectedSubTaskNumber = 2)
    private PollableFuture<Asset> addOrUpdateAssetAndProcessIfNeeded(
            Long repositoryId,
            String assetContent,
            String assetPath,
            String branchName,
            String branchCreatedByUsername,
            FilterConfigIdOverride filterConfigIdOverride,
            @InjectCurrentTask PollableTask currentTask) throws InterruptedException, ExecutionException, UnsupportedAssetFilterTypeException {

        PollableFutureTaskResult<Asset> pollableFutureTaskResult = new PollableFutureTaskResult<>();

        logger.debug("Process asset: {}, branch: {}", assetPath, branchName);

        Asset asset = assetRepository.findByPathAndRepositoryId(assetPath, repositoryId);

        if (asset != null && Boolean.TRUE.equals(asset.getVirtual())) {
            throw new AssetUpdateException("Update on an Asset can't be performed on virtual asset");
        }

        if (asset == null) {
            logger.debug("createAssetWithPollable, repositoryId: {}, assetPath: {}", repositoryId, assetPath);
            asset = createAssetWithPollable(repositoryId, assetPath, currentTask);
        } else {
            logger.debug("Asset already exists, repositoryId: {}, assetPath: {}, decrease number of sub tasks", repositoryId, assetPath);
            pollableFutureTaskResult.setExpectedSubTaskNumberOverride(1);
        }

        User branchCreatedByUser = branchCreatedByUsername != null ? userService.getOrCreatePartialBasicUser(branchCreatedByUsername) : auditorAware.getCurrentAuditor();

        Branch branch = branchService.getOrCreateBranch(asset.getRepository(), branchName, branchCreatedByUser);

        AssetExtractionByBranch assetExtractionByBranch = assetExtractionByBranchRepository.findByAssetAndBranch(asset, branch);

        if (isAssetUpdateNeeded(assetExtractionByBranch, assetContent)) {
            AssetContent assetContentEntity = assetContentService.createAssetContent(asset, assetContent, branch);
            assetExtractionService.processAssetAsync(assetContentEntity.getId(), filterConfigIdOverride, currentTask.getId());
        } else {
            undeleteAssetIfDeleted(assetExtractionByBranch);
            logger.debug("Asset content has not changed. Reset number of expected sub task to 0");
            pollableFutureTaskResult.setExpectedSubTaskNumberOverride(0);
            pollableFutureTaskResult.setMessageOverride("Asset content has not changed");
        }

        pollableFutureTaskResult.setResult(asset);
        return pollableFutureTaskResult;
    }


    public Asset createAsset(Long repositoryId, String assetPath, boolean virtualContent) {
        logger.debug("Create assest for repository id: {}, path: {}", repositoryId, assetPath);

        Asset asset = new Asset();

        Repository repository = repositoryRepository.findOne(repositoryId);

        asset.setRepository(repository);
        asset.setVirtual(virtualContent);
        asset.setPath(assetPath);

        asset = assetRepository.save(asset);
        return asset;
    }


    /**
     * Creates an {@link Asset}. Note that this function does not check if an
     * item with the same content already exists. It also does not process the
     * asset.
     *
     * @param repositoryId {@link Repository#id} of the repository that will
     *                     contain the asset
     * @param assetPath    Remote path of the asset
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
     * @param repositoryId {@link Repository#id} of the repository that will
     *                     contain the asset
     * @param assetContent Content of the asset
     * @param assetPath    Remote path of the assetTMServiceTest
     * @param parentTask   The parent task to be updated
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
     * <p>
     * First compare the asset content with the new content. If the contents are
     * the same check that the last successful extraction correspond to the
     * latest version of the asset. If not it means an issue happen before and
     * that regardless of the content being the same, the asset needs to updated
     * and reprocessed.
     *
     * @param assetContent    The asset to be compared
     * @param newAssetContent The content to be compared
     * @return true if the content of the asset is different from the new
     * content, false otherwise
     */
    private boolean isAssetUpdateNeeded(AssetExtractionByBranch assetExtractionByBranch, String newAssetContent) {

        boolean assetProcessingNeeded = false;

        if (assetExtractionByBranch == null) {
            logger.debug("No active asset extraction, processing needed");
            assetProcessingNeeded = true;
        } else if (!assetExtractionByBranch.getAssetExtraction().getContentMd5().equals(DigestUtils.md5Hex(newAssetContent))) {
            logger.debug("Content has changed, processing needed");
            assetProcessingNeeded = true;
        }

        return assetProcessingNeeded;
    }

    /**
     * @param asset
     * @param assetContent
     */
    @Transactional
    private void undeleteAssetIfDeleted(AssetExtractionByBranch assetExtractionByBranch) {
        Preconditions.checkNotNull(assetExtractionByBranch, "Can't undelete for null branch");

        logger.debug("undeleteAssetIfDeleted");

        Asset asset = assetExtractionByBranch.getAsset();

        if (asset.getDeleted()) {
            logger.debug("Undelete asset: {}", asset.getId());
            asset.setDeleted(false);
            assetRepository.save(asset);
        }

        if (assetExtractionByBranch.getDeleted()) {
            if (asset.getLastSuccessfulAssetExtraction() == null || asset.getLastSuccessfulAssetExtraction().getId() == assetExtractionByBranch.getAssetExtraction().getId()) {
                logger.debug("This asset extraction is the last sucessful for the asset, just undelete the asset which was done before");
            } else {
                logger.debug("Undeleted an active asset extraction, recompute for multiple branches");
                assetExtractionService.createAssetExtractionForMultipleBranches(asset, null);
            }

            assetExtractionByBranch.setDeleted(false);
            assetExtractionByBranchRepository.save(assetExtractionByBranch);
        }
    }

    /**
     * Deletes an {@link Asset} by the {@link Asset#id}. It performs logical
     * delete.
     *
     * @param asset
     */
    @Transactional
    public void deleteAsset(Asset asset) {
        logger.debug("Delete an asset with path: {}", asset.getPath());

        asset.setDeleted(true);
        assetRepository.save(asset);

        int numberOfAssetExtractionByBranchUpdated = assetExtractionByBranchRepository.setDeletedTrue(asset);

        logger.debug("Deleted asset with path: {} (Number of AssetExtractionByBranchUpdated: {})", asset.getPath(), numberOfAssetExtractionByBranchUpdated);
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
            Asset asset = assetRepository.findOne(assetId);
            if (asset != null) {
                deleteAsset(asset);
            }
        }

        logger.debug("Deleted assets {}", assetIds.toString());
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
        Asset asset = assetRepository.findOne(assetId);
        if (asset != null) {
            Branch branch = branchRepository.findOne(branchId);
            AssetExtractionByBranch assetExtractionByBranch = assetExtractionByBranchRepository.findByAssetAndBranch(asset, branch);

            if (assetExtractionByBranch == null) {
                logger.debug("Asset extraction for asset: {} and branch: {} doesn't exist", asset.getPath(), branchId);
            } else {
                if (assetExtractionByBranch.getDeleted()) {
                    logger.debug("Asset extraction for asset: {} and branch: {} already deleted", asset.getPath(), branchId);
                } else {
                    logger.debug("Mark asset extraction for asset: {} and branch: {} as deleted", asset.getPath(), branchId);
                    assetExtractionByBranch.setDeleted(true);
                    assetExtractionByBranchRepository.save(assetExtractionByBranch);

                    List<AssetExtractionByBranch> assetExtractionByBranches = assetExtractionByBranchRepository.findByAssetAndDeletedFalse(asset);
                    if (!assetExtractionByBranches.isEmpty()) {
                        logger.debug("Some branch are still present for asset, create new assset exraction");
                        AssetExtraction assetExtraction = assetExtractionService.createAssetExtractionForMultipleBranches(asset, null);
                        assetExtractionService.markAssetExtractionAsLastSuccessful(asset, assetExtraction);
                    }
                }
            }

            logger.debug("Check if the asset must be deleted");
            List<AssetExtractionByBranch> assetExtractionByBranches = assetExtractionByBranchRepository.findByAssetAndDeletedFalse(asset);

            if (assetExtractionByBranches.isEmpty()) {
                logger.debug("All branch are deleted or removed, delete asset");
                deleteAsset(asset);
            }
        }
    }

    public List<Asset> findAll(Long repositoryId, String path, Boolean deleted, Boolean virtual, Long branchId) {

        logger.debug("Find all assets for repositoryId: {}, path: {}, deleted: {}, virtual: {}, branchId: {}",
                repositoryId, path, deleted, virtual, branchId);

        Specifications<Asset> assetSpecifications = distinct(ifParamNotNull(repositoryIdEquals(repositoryId)))
                .and(ifParamNotNull(pathEquals(path)))
                .and(ifParamNotNull(deletedEquals(deleted)))
                .and(ifParamNotNull(virtualEquals(virtual)))
                .and(ifParamNotNull(branchId(branchId, deleted)));

        List<Asset> all = assetRepository.findAll(assetSpecifications);
        return all;
    }
}