package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetContent;
import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.AssetExtractionByBranch;
import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.PluralForm;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.rest.asset.FilterConfigIdOverride;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.assetExtraction.extractor.AssetExtractor;
import com.box.l10n.mojito.service.assetExtraction.extractor.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.service.assetTextUnit.AssetTextUnitRepository;
import com.box.l10n.mojito.service.assetcontent.AssetContentService;
import com.box.l10n.mojito.service.branch.BranchRepository;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableFutureTaskResult;
import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import com.ibm.icu.text.MessageFormat;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

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

    public static final String PRIMARY_BRANCH = "master";

    @Autowired
    AssetExtractor assetExtractor;

    @Autowired
    AssetMappingService assetMappingService;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    AssetContentService assetContentService;

    @Autowired
    AssetExtractionRepository assetExtractionRepository;

    @Autowired
    AssetTextUnitRepository assetTextUnitRepository;

    @Autowired
    AssetExtractionByBranchRepository assetExtractionByBranchRepository;

    @Autowired
    BranchRepository branchRepository;

    @Autowired
    QuartzPollableTaskScheduler quartzPollableTaskScheduler;

    @Autowired
    RetryTemplate retryTemplate;

    /**
     * If the asset type is supported, starts the text units extraction for the given asset.
     *
     * @param parentTask             The parent task to be updated
     * @param assetContentId         {@link Asset#id} to extract text units from
     * @param filterConfigIdOverride
     * @param filterOptions
     * @param currentTask            The current task, injected
     * @return A {@link Future}
     * @throws UnsupportedAssetFilterTypeException                                          If asset type not supported
     * @throws java.lang.InterruptedException
     * @throws com.box.l10n.mojito.service.assetExtraction.AssetExtractionConflictException
     */
    public PollableFuture<Asset> processAsset(
            Long assetContentId,
            FilterConfigIdOverride filterConfigIdOverride,
            String filterOptions,
            PollableTask currentTask) throws UnsupportedAssetFilterTypeException, InterruptedException, AssetExtractionConflictException {

        logger.debug("Start processing asset content, id: {}", assetContentId);
        AssetContent assetContent = assetContentService.findOne(assetContentId);
        Asset asset = assetContent.getAsset();

        AssetExtraction assetExtraction = createAssetExtraction(assetContent, currentTask);

        assetExtractor.performAssetExtraction(assetExtraction, filterConfigIdOverride, filterOptions, currentTask);

        assetMappingService.mapAssetTextUnitAndCreateTMTextUnit(
                assetExtraction.getId(),
                asset.getRepository().getTm().getId(),
                asset.getId(),
                assetContent.getBranch().getCreatedByUser(),
                currentTask);

        markAssetExtractionForBranch(assetExtraction);

        if (hasMoreActiveBranches(asset, assetContent.getBranch())) {
            logger.debug("Need to support branches, create a merged asset extraction for the branches");
            assetExtraction = createAssetExtractionForMultipleBranches(asset, currentTask);
        } else {
            logger.debug("No need to support branch, use current extraction as latest sucessful");
        }

        markAssetExtractionAsLastSuccessful(asset, assetExtraction);

        logger.info("Done processing asset content id: {}", assetContentId);

        return new PollableFutureTaskResult<>(asset);
    }

    /**
     * Marks the provided asset extraction as current extraction for the branch.
     * <p>
     * Make the function {@link Retryable} since this can have concurrent access issue when multiple thread trying to save
     * the active asset extraction at the same time. It is not important which one wins last since in real usage
     * that should not really happen, it is an edge case and the code just need to be safe.
     *
     * @param assetExtraction
     */
    private void markAssetExtractionForBranch(final AssetExtraction assetExtraction) {

        final Asset asset = assetExtraction.getAsset();
        final Branch branch = assetExtraction.getAssetContent().getBranch();

        logger.debug("markAssetExtractionForBranch, assetId: {}, branch: {}", asset.getId(), branch.getName());

        retryTemplate.execute(new RetryCallback<AssetExtractionByBranch, DataIntegrityViolationException>() {
            @Override
            public AssetExtractionByBranch doWithRetry(RetryContext context) throws DataIntegrityViolationException {

                if (context.getRetryCount() > 0) {
                    logger.debug("Concurrent modification happened when saving the active asset extraction, retry");
                }

                AssetExtractionByBranch assetExtractionByBranch = assetExtractionByBranchRepository.findByAssetAndBranch(assetExtraction.getAsset(), branch);

                if (assetExtractionByBranch == null) {
                    assetExtractionByBranch = new AssetExtractionByBranch();
                    assetExtractionByBranch.setAssetExtraction(assetExtraction);
                    assetExtractionByBranch.setAsset(assetExtraction.getAsset());
                    assetExtractionByBranch.setBranch(branch);
                }

                assetExtractionByBranch.setAssetExtraction(assetExtraction);
                assetExtractionByBranch.setDeleted(false);

                assetExtractionByBranch = assetExtractionByBranchRepository.save(assetExtractionByBranch);

                return assetExtractionByBranch;
            }
        });
    }

    boolean hasMoreActiveBranches(Asset asset, Branch branch) {
        int numberOfBranch = assetExtractionByBranchRepository.countByAssetAndDeletedFalseAndBranchNot(asset, branch);
        logger.debug("There are {} additional branches", numberOfBranch);
        return numberOfBranch > 0;
    }

    /**
     * Creates an asset extraction that is a merge of the latest (non deleted) asset extraction of each branch of
     * the asset.
     *
     * @param asset
     * @param pollableTask
     * @return
     */
    public AssetExtraction createAssetExtractionForMultipleBranches(Asset asset, PollableTask pollableTask) {

        logger.debug("Get branches to be merged");
        List<AssetExtractionByBranch> assetExtractionByBranches = assetExtractionByBranchRepository.findByAssetAndDeletedFalse(asset);
        assetExtractionByBranches = sortedAssetExtractionByBranches(assetExtractionByBranches);

        StringBuilder mergedAssetExtractionMd5Builder = new StringBuilder();

        logger.debug("Create a new asset extraction that will contain the merge");
        AssetExtraction mergedAssetExtraction = createAssetExtraction(asset, pollableTask);

        HashMap<String, AssetTextUnit> mergedAssetTextUnits = new LinkedHashMap<>();

        for (AssetExtractionByBranch assetExtractionByBranch : assetExtractionByBranches) {
            logger.debug("Start processing branch: {} for asset: {}", assetExtractionByBranch.getBranch().getName(), assetExtractionByBranch.getAsset().getPath());
            mergedAssetExtractionMd5Builder.append(assetExtractionByBranch.getAssetExtraction().getContentMd5());

            List<AssetTextUnit> assetTextUnitsToMerge;

            if (mergedAssetTextUnits.keySet().isEmpty()) {
                logger.debug("Get all asset text units");
                assetTextUnitsToMerge = assetTextUnitRepository.findByAssetExtraction(assetExtractionByBranch.getAssetExtraction());

            } else {
                logger.debug("Get filtered asset text units (exclude the ones already merged)");
                assetTextUnitsToMerge = assetTextUnitRepository.findByMd5NotInAndAssetExtraction(mergedAssetTextUnits.keySet(),
                        assetExtractionByBranch.getAssetExtraction());
            }

            for (AssetTextUnit assetTextUnit : assetTextUnitsToMerge) {
                AssetTextUnit copy = copyAssetTextUnit(assetTextUnit);
                copy.setBranch(assetExtractionByBranch.getBranch());
                copy.setAssetExtraction(mergedAssetExtraction);
                mergedAssetTextUnits.put(copy.getMd5(), copy);
            }
        }

        mergedAssetExtraction.setContentMd5(DigestUtils.md5Hex(mergedAssetExtractionMd5Builder.toString()));

        assetTextUnitRepository.save(mergedAssetTextUnits.values());

        Long mergedAssetExtractionId = mergedAssetExtraction.getId();
        Long tmId = asset.getRepository().getTm().getId();
        long mapExactMatches = assetMappingService.mapExactMatches(mergedAssetExtractionId, tmId, asset.getId());
        logger.debug("{} text units were mapped for the merged extraction with id: {} and tmId: {}", mapExactMatches, mergedAssetExtractionId, tmId);

        return mergedAssetExtraction;
    }

    /**
     * For now we hard code putting master first. This is really git/project specific. Later this could be configurable
     *
     * @param assetExtractionByBranches
     */
    private List<AssetExtractionByBranch> sortedAssetExtractionByBranches(List<AssetExtractionByBranch> assetExtractionByBranches) {

        Ordering<AssetExtractionByBranch> byName = Ordering.natural().onResultOf(new Function<AssetExtractionByBranch, Comparable>() {
            @Nullable
            @Override
            public Comparable apply(@Nullable AssetExtractionByBranch input) {
                return PRIMARY_BRANCH.equals(input.getBranch().getName());
            }
        }).reverse();


        Ordering<AssetExtractionByBranch> byDate = Ordering.natural().nullsLast().onResultOf(new Function<AssetExtractionByBranch, Comparable>() {
            @Nullable
            @Override
            public Comparable apply(@Nullable AssetExtractionByBranch input) {
                return input.getCreatedDate();
            }
        });

        return byName.compound(byDate).sortedCopy(assetExtractionByBranches);
    }


    public void deleteAssetBranch(Asset asset, String branchName) {
        Branch branch = branchRepository.findByNameAndRepository(branchName, asset.getRepository());
        AssetExtractionByBranch assetExtractionByBranch = assetExtractionByBranchRepository.findByAssetAndBranch(asset, branch);
        assetExtractionByBranch.setDeleted(true);
        assetExtractionByBranchRepository.save(assetExtractionByBranch);
        AssetExtraction assetExtractionForMultipleBranches = createAssetExtractionForMultipleBranches(asset, null);
        markAssetExtractionAsLastSuccessful(asset, assetExtractionForMultipleBranches);
    }

    public AssetTextUnit copyAssetTextUnit(AssetTextUnit assetTextUnit) {

        logger.debug("copyAssetTextUnit with id: {} and name: {}", assetTextUnit.getId(), assetTextUnit.getName());

        AssetTextUnit copy = new AssetTextUnit();

        copy.setName(assetTextUnit.getName());
        copy.setDoNotTranslate(assetTextUnit.isDoNotTranslate());
        copy.setAssetExtraction(assetTextUnit.getAssetExtraction());
        copy.setComment(assetTextUnit.getComment());
        copy.setContent(assetTextUnit.getContent());
        copy.setContentMd5(assetTextUnit.getContentMd5());
        copy.setCreatedByUser(assetTextUnit.getCreatedByUser());
        copy.setCreatedDate(assetTextUnit.getCreatedDate());
        copy.setMd5(assetTextUnit.getMd5());
        copy.setPluralForm(assetTextUnit.getPluralForm());
        copy.setPluralFormOther(assetTextUnit.getPluralFormOther());
        copy.setUsages(new HashSet<>(assetTextUnit.getUsages()));
        copy.setBranch(assetTextUnit.getBranch());

        return copy;
    }

    public PollableFuture processAssetAsync(
            Long assetContentId,
            FilterConfigIdOverride filterConfigIdOverride,
            String filterOptions,
            Long parentTaskId) throws UnsupportedAssetFilterTypeException, InterruptedException, AssetExtractionConflictException {

        ProcessAssetJobInput processAssetJobInput = new ProcessAssetJobInput();
        processAssetJobInput.setAssetContentId(assetContentId);
        processAssetJobInput.setFilterConfigIdOverride(filterConfigIdOverride);
        processAssetJobInput.setFilterOptions(filterOptions);

        String pollableMessage = MessageFormat.format("Process asset: {0}", assetContentId);

        return quartzPollableTaskScheduler.scheduleJob(ProcessAssetJob.class, processAssetJobInput, parentTaskId, pollableMessage, 2);
    }

    /**
     * Mark the asset extraction as last successful.
     * <p>
     * Make the function {@link Retryable} since this can have concurrent access issue when multiple thread trying to update
     * update the asset at the same time. It is not important which one wins last since in real usage
     * that should not really happen, it is an edge case and the code just need to be safe.
     *
     * @param asset           The asset to update
     * @param assetExtraction The asset extraction to mark as last successful
     */
    @Retryable
    public void markAssetExtractionAsLastSuccessful(Asset asset, AssetExtraction assetExtraction) {
        logger.debug("Marking asset extraction as last successful, assetExtractionId: {}", assetExtraction.getId());
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
     * Creates an AssetExtraction associated to the given asset
     *
     * @param asset The asset associated to the AssetExtraction
     * @return The created assetExtraction instance
     */
    @Transactional
    public AssetExtraction createAssetExtraction(AssetContent assetContent, PollableTask pollableTask) {
        AssetExtraction assetExtraction = new AssetExtraction();

        assetExtraction.setAsset(assetContent.getAsset());
        assetExtraction.setAssetContent(assetContent);
        assetExtraction.setContentMd5(assetContent.getContentMd5());
        assetExtraction.setPollableTask(pollableTask);
        assetExtraction = assetExtractionRepository.save(assetExtraction);

        return assetExtraction;
    }

    /**
     * Creates a new AssetTextUnit, and associate it to the given asset extraction.
     *
     * @param assetExtraction the assetExtraction object the TextUnit comes from (must be valid)
     * @param name            Name of the TextUnit
     * @param content         Content of the TextUnit
     * @param comment         Comment for the TextUnit
     * @return The created AssetTextUnit
     */
    public AssetTextUnit createAssetTextUnit(AssetExtraction assetExtraction, String name, String content, String comment) {
        Branch branch = assetExtraction.getAssetContent() != null ? assetExtraction.getAssetContent().getBranch() : null;
        return createAssetTextUnit(assetExtraction.getId(), name, content, comment, null, null, false, null, branch);
    }

    /**
     * Creates a new AssetTextUnit, and associate it to the given asset extraction.
     *
     * @param assetExtractionId ID of the assetExtraction object the TextUnit comes from (must be valid)
     * @param name              Name of the TextUnit
     * @param content           Content of the TextUnit
     * @param comment           Comment for the TextUnit
     * @param pluralForm        optional plural form
     * @param pluralFormOther   optional other plural form
     * @param doNotTranslate    to indicate if the TextUnit should be translated
     * @param usages            optional usages in the code source
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

        logger.debug("Adding AssetTextUnit for assetExtractionId: {}\nname: {}\ncontent: {}\ncomment: {}\n", assetExtractionId, name, content, comment);

        AssetTextUnit assetTextUnit = new AssetTextUnit();
        assetTextUnit.setAssetExtraction(assetExtractionRepository.getOne(assetExtractionId));
        assetTextUnit.setName(name);
        assetTextUnit.setContent(content);
        assetTextUnit.setComment(comment);
        assetTextUnit.setMd5(DigestUtils.md5Hex(name + content + comment));
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
