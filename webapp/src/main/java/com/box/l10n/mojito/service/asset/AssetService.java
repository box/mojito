package com.box.l10n.mojito.service.asset;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.rest.asset.FilterConfigIdOverride;
import com.box.l10n.mojito.rest.asset.SourceAsset;
import com.box.l10n.mojito.service.assetExtraction.AssetExtractionService;
import com.box.l10n.mojito.service.pollableTask.InjectCurrentTask;
import com.box.l10n.mojito.service.pollableTask.MsgArg;
import com.box.l10n.mojito.service.pollableTask.ParentTask;
import com.box.l10n.mojito.service.pollableTask.Pollable;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableFutureTaskResult;
import com.box.l10n.mojito.service.pollableTask.PollableTaskService;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    AssetExtractionService assetExtractionService;

    @Autowired
    RepositoryRepository repositoryRepository;

    @Autowired
    PollableTaskService pollableTaskService;

    /**
     * Adds an {@link Asset} to a {@link Repository}.
     * <p/>
     * The asset is added only if an asset with the same path does not already
     * exist. After the asset is added, it starts the extraction job to get text
     * units.
     *
     * @param repositoryId {@link Repository#id} of the repository that will
     * contain the asset
     * @param assetContent Content of the asset
     * @param assetPath Remote path of the asset
     * @param filterConfigIdOverride Optional, can be null. Allows to specify
     * a specific Okapi filter to use to process the asset
     * @return The created asset
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public PollableFuture<Asset> addOrUpdateAssetAndProcessIfNeeded(
            Long repositoryId, 
            String assetContent, 
            String assetPath, 
            FilterConfigIdOverride filterConfigIdOverride) throws ExecutionException, InterruptedException {
        return addOrUpdateAssetAndProcessIfNeeded(repositoryId, assetContent, assetPath, filterConfigIdOverride, PollableTask.INJECT_CURRENT_TASK);
    }

    /**
     * See
     * {@link AssetService#addOrUpdateAssetAndProcessIfNeeded(Long, String, String)}
     *
     * @param repositoryId {@link Repository#id} of the repository that will
     * contain the asset
     * @param assetContent Content of the asset
     * @param assetPath Remote path of the asset
     * @param currentTask The current task, injected
     * @return The created asset
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Pollable(expectedSubTaskNumber = 2)
    private PollableFuture<Asset> addOrUpdateAssetAndProcessIfNeeded(
            Long repositoryId,
            String assetContent,
            String assetPath,
            FilterConfigIdOverride filterConfigIdOverride,
            @InjectCurrentTask PollableTask currentTask) throws InterruptedException, ExecutionException {

        PollableFutureTaskResult<Asset> pollableFutureTaskResult = new PollableFutureTaskResult<>();

        // Check if the an asset with the same path already exists
        Asset asset = assetRepository.findByPathAndRepositoryId(assetPath, repositoryId);

        // if an asset for the given path does not already exists or if its contents changed, start the extraction
        if (asset == null) {
            asset = createAsset(repositoryId, assetContent, assetPath, currentTask);
            assetExtractionService.processAsset(asset.getId(), filterConfigIdOverride, currentTask, PollableTask.INJECT_CURRENT_TASK);
        } else if (isAssetUpdateNeeded(asset, assetContent)) {
            updateAssetContent(asset, assetContent, currentTask);
            assetExtractionService.processAsset(asset.getId(), filterConfigIdOverride, currentTask, PollableTask.INJECT_CURRENT_TASK);
        } else {
            undeleteAssetIfDeleted(asset);
            logger.debug("Asset content has not changed. Reset number of expected sub task to 0");
            pollableFutureTaskResult.setExpectedSubTaskNumberOverride(0);
            pollableFutureTaskResult.setMessageOverride("Asset content has not changed");
        }

        pollableFutureTaskResult.setResult(asset);
        return pollableFutureTaskResult;
    }

    /**
     * Creates an {@link Asset}. Note that this function does not check if an
     * item with the same content already exists. It also does not process the
     * asset.
     *
     * @param repositoryId {@link Repository#id} of the repository that will
     * contain the asset
     * @param assetContent Content of the asset
     * @param assetPath Remote path of the asset
     * @return The created asset
     * @throws AssetCreationException If failed to create asset
     */
    public Asset createAsset(Long repositoryId, String assetContent, String assetPath) throws AssetCreationException {

        try {
            return createAsset(repositoryId, assetContent, assetPath, null);
        } catch (Exception e) {
            throw new AssetCreationException(e.getMessage(), e.getCause());
        }
    }

    /**
     * See {@link AssetService#createAsset(Long, String, String)}
     *
     * @param repositoryId {@link Repository#id} of the repository that will
     * contain the asset
     * @param assetContent Content of the asset
     * @param assetPath Remote path of the asset
     * @param parentTask The parent task to be updated
     * @return The created asset
     */
    @Pollable(message = "Creating asset: {assetPath}")
    @Transactional
    private Asset createAsset(Long repositoryId, String assetContent, @MsgArg(name = "assetPath") String assetPath, @ParentTask PollableTask parentTask) {
        Asset asset = new Asset();

        asset.setRepository(repositoryRepository.getOne(repositoryId));
        asset.setContent(assetContent);
        asset.setContentMd5(DigestUtils.md5Hex(assetContent));
        asset.setPath(assetPath);

        assetRepository.save(asset);

        return asset;
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
     * @param asset The asset to be compared
     * @param newAssetContent The content to be compared
     * @return true if the content of the asset is different from the new
     * content, false otherwise
     */
    private boolean isAssetUpdateNeeded(Asset asset, String newAssetContent) {

        boolean assetProcessingNeeded = false;

        if (!asset.getContentMd5().equals(DigestUtils.md5Hex(newAssetContent))) {
            logger.debug("Content has changed, processing needed");
            assetProcessingNeeded = true;
        } else if (asset.getLastSuccessfulAssetExtraction() == null) {
            logger.debug("No asset extraction, processing needed");
            assetProcessingNeeded = true;
        } else if (!asset.getContentMd5().equals(asset.getLastSuccessfulAssetExtraction().getContentMd5())) {
            logger.debug("The last successful extraction is older than the last asset version, processing needed");
            assetProcessingNeeded = true;
        }

        return assetProcessingNeeded;
    }

    /**
     * @param asset The asset to be updated
     * @param newAssetContent The new asset content
     * @param parentTask The parent task to be updated
     */
    @Transactional
    @Pollable(message = "Updating asset: {assetPath}")
    private void updateAssetContent(
            @MsgArg(name = "assetPath", accessor = "getPath") Asset asset,
            String newAssetContent,
            @ParentTask PollableTask parentTask) {

        asset.setContent(newAssetContent);
        asset.setContentMd5(DigestUtils.md5Hex(newAssetContent));
        asset.setDeleted(false);
        assetRepository.save(asset);
    }
    
    /**
     * 
     * @param asset 
     */
    @Transactional
    private void undeleteAssetIfDeleted(Asset asset) {
        if (asset.getDeleted()) {
            asset.setDeleted(false);
            assetRepository.save(asset);
        }
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
        
        logger.debug("Deleted asset with path: {}", asset.getPath());

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

}
