package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.assetExtraction.extractor.AssetExtractor;
import com.box.l10n.mojito.service.assetExtraction.extractor.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.service.assetTextUnit.AssetTextUnitRepository;
import com.box.l10n.mojito.service.pollableTask.InjectCurrentTask;
import com.box.l10n.mojito.service.pollableTask.MsgArg;
import com.box.l10n.mojito.service.pollableTask.ParentTask;
import com.box.l10n.mojito.service.pollableTask.Pollable;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableFutureTaskResult;
import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.concurrent.Future;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to manage asset extraction. It processes assets to extract {@link AssetTextUnit}s.
 *
 * @author aloison
 */
@Service
public class AssetExtractionService {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(AssetExtractionService.class);

    @Autowired
    AssetExtractor assetExtractor;

    @Autowired
    AssetMappingService assetMappingService;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    AssetExtractionRepository assetExtractionRepository;

    @Autowired
    AssetTextUnitRepository assetTextUnitRepository;


    /**
     * If the asset type is supported, starts the text units extraction for the given asset.
     *
     * @param assetId {@link Asset#id} to extract text units from
     * @param parentTask The parent task to be updated
     * @param currentTask The current task, injected
     * @return A {@link Future}
     * @throws UnsupportedAssetFilterTypeException If asset type not supported
     * @throws java.lang.InterruptedException
     * @throws com.box.l10n.mojito.service.assetExtraction.AssetExtractionConflictException
     */
    @Pollable(async = true, message = "Process asset: {assetId}", expectedSubTaskNumber = 2)
    public PollableFuture<Asset> processAsset(
            @MsgArg(name = "assetId") Long assetId,
            @ParentTask PollableTask parentTask,
            @InjectCurrentTask PollableTask currentTask) throws UnsupportedAssetFilterTypeException, InterruptedException, AssetExtractionConflictException {
        
        logger.info("Start processing asset id: {}", assetId);
        Asset asset = assetRepository.findOne(assetId);
          
        waitForCurrentAssetExtractionToFinish(asset);
        
        AssetExtraction assetExtraction = createAssetExtraction(asset, currentTask);

        assetExtractor.performAssetExtraction(assetExtraction, currentTask);

        assetMappingService.mapAssetTextUnitAndCreateTMTextUnit(assetExtraction.getId(), asset.getRepository().getTm().getId(), assetId, currentTask);

        markAssetExtractionAsLastSuccessful(asset, assetExtraction);

        logger.info("Done processing asset id: {}", assetId);

        return new PollableFutureTaskResult<>(asset);
    }
    
    /**
     * Mark the asset extraction as last successful
     *
     * @param asset           The asset to update
     * @param assetExtraction The asset extraction to mark as last successful
     *
     * NOTE: Reason for making this public:  The service is stateless. In the method there are
     * 3 lines of code each of them are public. So technically speaking there is no implication
     * at all of exposing this method because everything can already be done anyway.
     * Granted this won't be used directly in the code but that's not such as big deal.
     */
    @VisibleForTesting
    @Transactional
    public void markAssetExtractionAsLastSuccessful(Asset asset, AssetExtraction assetExtraction) {
        // Once all text units have been extracted, make sure the asset points to the right assetExtraction
        logger.debug("Marking asset extraction as last successful, assetExtractionId: {}", assetExtraction.getId());
        asset.setLastSuccessfulAssetExtraction(assetExtraction);
        assetRepository.save(asset);
    }

    /**
     * Creates an AssetExtraction associated to the given asset
     *
     * @param asset The asset associated to the AssetExtraction
     * @return The created assetExtraction instance
     */
    @Transactional
    public AssetExtraction createAssetExtraction(Asset asset, PollableTask pollableTask) {
        AssetExtraction assetExtraction = new AssetExtraction();
        assetExtraction.setAsset(asset);
        assetExtraction.setContentMd5(asset.getContentMd5());
        assetExtraction.setPollableTask(pollableTask);
        assetExtractionRepository.save(assetExtraction);

        return assetExtraction;
    }

    /**
     * Creates a new AssetTextUnit, and associate it to the given asset extraction.
     *
     * @param assetExtractionId ID of the assetExtraction object the TextUnit comes from (must be valid)
     * @param name              Name of the TextUnit
     * @param content           Content of the TextUnit
     * @param comment           Comment for the TextUnit
     * @return The created AssetTextUnit
     */
    @Transactional
    public AssetTextUnit createAssetTextUnit(Long assetExtractionId, String name, String content, String comment) {

        logger.debug("Adding AssetTextUnit for assetExtractionId: {}\nname: {}\ncontent: {}\ncomment: {}\n", assetExtractionId, name, content, comment);

        AssetTextUnit assetTextUnit = new AssetTextUnit();
        assetTextUnit.setAssetExtraction(assetExtractionRepository.getOne(assetExtractionId));
        assetTextUnit.setName(name);
        assetTextUnit.setContent(content);
        assetTextUnit.setComment(comment);
        assetTextUnit.setMd5(DigestUtils.md5Hex(name + content + comment));
        assetTextUnit.setContentMd5(DigestUtils.md5Hex(content));

        assetTextUnitRepository.save(assetTextUnit);

        logger.trace("AssetTextUnit saved");

        return assetTextUnit;
    }
    
    /**
     * Waits if there are other asset extraction running for the same asset.
     *
     * @param asset 
     */
    private void waitForCurrentAssetExtractionToFinish(Asset asset) throws InterruptedException {
        int retry = 1;
        int maxRetry = 5; 
        List<AssetExtraction> runningAssetExtractions = assetExtractionRepository.findByAssetAndPollableTaskIsNotNullAndPollableTaskFinishedDateIsNull(asset);
        while (!runningAssetExtractions.isEmpty()) {
            if (retry <= maxRetry) {
                Thread.sleep(retry * 1000);
                retry++;
                runningAssetExtractions = assetExtractionRepository.findByAssetAndPollableTaskIsNotNullAndPollableTaskFinishedDateIsNull(asset);
            } else {
                throw new AssetExtractionConflictException("Retry exhausted while waiting for an existing asset extraction for asset " + asset.getId() + " to finish.");
            }
        }
    }
}
