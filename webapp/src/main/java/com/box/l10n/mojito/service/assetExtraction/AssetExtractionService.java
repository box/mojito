package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.AssetContent;
import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.AssetExtractionByBranch;
import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.entity.AssetTextUnitToTMTextUnit;
import com.box.l10n.mojito.entity.BaseEntity;
import com.box.l10n.mojito.entity.Branch;
import com.box.l10n.mojito.entity.PluralForm;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.ltm.merger.AssetExtractorTextUnitsToMultiBranchState;
import com.box.l10n.mojito.ltm.merger.BranchData;
import com.box.l10n.mojito.ltm.merger.BranchStateTextUnit;
import com.box.l10n.mojito.ltm.merger.BranchStateTextUnitJson;
import com.box.l10n.mojito.ltm.merger.Match;
import com.box.l10n.mojito.ltm.merger.MultiBranchState;
import com.box.l10n.mojito.ltm.merger.MultiBranchStateJson;
import com.box.l10n.mojito.ltm.merger.MultiBranchStateMerger;
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
import com.box.l10n.mojito.service.blobstorage.Retention;
import com.box.l10n.mojito.service.blobstorage.StructuredBlobStorage;
import com.box.l10n.mojito.service.branch.BranchRepository;
import com.box.l10n.mojito.service.leveraging.LeveragerByTmTextUnit;
import com.box.l10n.mojito.service.pluralform.PluralFormService;
import com.box.l10n.mojito.service.pollableTask.ParentTask;
import com.box.l10n.mojito.service.pollableTask.Pollable;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableFutureTaskResult;
import com.box.l10n.mojito.service.tm.TMRepository;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.TextUnitIdMd5DTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.base.Stopwatch;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.ibm.icu.text.MessageFormat;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Function.identity;

/**
 * Service to manage asset extraction. It processes assets to extract {@link AssetTextUnit}s.
 *
 * @author aloison
 */
@Service
public class AssetExtractionService {

    public static final String PRIMARY_BRANCH = "master";
    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(AssetExtractionService.class);
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

    @Autowired
    FilterOptionsMd5Builder filterOptionsMd5Builder;

    @Autowired
    AssetExtractor assetExtractor;

    @Autowired
    PluralFormService pluralFormService;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    TextUnitUtils textUnitUtils;

    @Autowired
    AssetExtractorTextUnitsToMultiBranchState assetExtractorTextUnitsToMultiBranchState;

    @Autowired
    MultiBranchStateMerger multiBranchStateMerger;

    @Autowired
    TMRepository tmRepository;

    @Autowired
    TMService tmService;

    @Autowired
    TMTextUnitRepository tmTextUnitRepository;


    @Autowired
    AssetTextUnitToTMTextUnitRepository assetTextUnitToTMTextUnitRepository;

    // get content for branch to be processed
    // build current MultiBranchState: CS-1 with branch content
    // fetch the base MultiBranchState BS-1
    // merge CS-1 into BS-1, this becomes the new current state: CS-2
    // create text units based on CS-2
    // CS-2 updated with ids of newly create text units gives new current state: CS-3
    // in retry logic with optimistic locking:
    // - check if BS-1 has changed, if so fetch BS-2 and compute new current state: CS-4 = BS2 + CS-3
    // -
    // create ATM from new state
    @Autowired
    StructuredBlobStorage structuredBlobStorage;

    boolean useNewImplementation = true;

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
            List<String> filterOptions,
            PollableTask currentTask) throws UnsupportedAssetFilterTypeException, InterruptedException, AssetExtractionConflictException {

        if (useNewImplementation) {
            return processAssetNew(assetContentId, filterConfigIdOverride, filterOptions, currentTask);
        }

        logger.debug("Start processing asset content, id: {}", assetContentId);
        AssetContent assetContent = assetContentService.findOne(assetContentId);
        Asset asset = assetContent.getAsset();

        AssetExtraction assetExtraction = createAssetExtraction(assetContent, currentTask, filterOptions);

        List<String> md5sToSkip = getMd5sToSkip(assetContent, asset);

        performAssetExtraction(assetExtraction, filterConfigIdOverride, filterOptions, md5sToSkip, currentTask);

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

        markAssetExtractionAsLastSuccessfulWithRetry(asset, assetExtraction);

        logger.info("Done processing asset content id: {}", assetContentId);

        return new PollableFutureTaskResult<>(asset);
    }

    public PollableFuture<Asset> processAssetNew(
            Long assetContentId,
            FilterConfigIdOverride filterConfigIdOverride,
            List<String> filterOptions,
            PollableTask currentTask) throws UnsupportedAssetFilterTypeException, InterruptedException, AssetExtractionConflictException {


        Stopwatch stopwatch = Stopwatch.createStarted();

        logger.debug("Start processing asset content, id: {}", assetContentId);
        AssetContent assetContent = assetContentService.findOne(assetContentId);
        Asset asset = assetContent.getAsset(); // TODO(perf) move to s3 ?

        if (asset.getDeleted()) {
            asset.setDeleted(false);
            assetRepository.save(asset);
        }

        logAndReset(stopwatch, "done getting asset");

        MultiBranchState stateForNewContent = performAssetExtractionNew(assetContent, filterConfigIdOverride, filterOptions, currentTask);
        // TODO this is still need for the whole system to work but potentially we want to remove it
        // if there is not asset extraction for branch - retrigger the asset processing regarless if the content hasn't changed
        logAndReset(stopwatch, "done performAssetExtractionNew");

        // TODO should this saved independently
        MultiBranchState stateOfLastSuccesfulExtraction = readMultiBranchState(asset);

        logAndReset(stopwatch, "done readMultiBranchState");

        // TODO make branch configurable
        MultiBranchState newStateForLastSuccesfulExtraction = multiBranchStateMerger.merge(stateOfLastSuccesfulExtraction, stateForNewContent, ImmutableList.of("master"));
        logAndReset(stopwatch, "done merging base and current");

        ImmutableMap<String, BranchStateTextUnit> textUnitsToCreate = getTextUnitsToCreate(newStateForLastSuccesfulExtraction);
        logAndReset(stopwatch, "done get text units to create");

        ImmutableList<Match> matchesForSourceLeveraging = getLeveragingMatches(textUnitsToCreate, stateOfLastSuccesfulExtraction);
        logAndReset(stopwatch, "done doing leveraging");

        ImmutableMap<String, Long> createdTextUnits = createTextUnits(textUnitsToCreate, assetContent.getBranch().getCreatedByUser(), asset);
        logAndReset(stopwatch, "done creating text units");

        MultiBranchState newStateForLastSuccesfulExtractionWithIds = getStateWithMergedIds(newStateForLastSuccesfulExtraction, createdTextUnits);
        logAndReset(stopwatch, "done getStateWithMergedIds");

        newStateForLastSuccesfulExtractionWithIds.getMd5ToBranchStateTextUnits().values().stream().filter(t -> t.getId() == null).forEach(t -> logger.error("Missing id for md5: {}", t.getMd5()));

        performLeveraging(matchesForSourceLeveraging);
        AssetExtraction lastSuccessfulAssetExtraction = getOrCreateLastSuccessfulAssetExtraction(asset);

        logAndReset(stopwatch, "get or create last succesful extraction");

        createAssetTextUnitAndMapping(lastSuccessfulAssetExtraction, newStateForLastSuccesfulExtractionWithIds, stateOfLastSuccesfulExtraction);

        // we write the json to blob storage at that point
        // S3 write should be independant and we should rely on an unique ID in the storage, instead if should be save an entity and set in transaction
        writeMultiBranchState(newStateForLastSuccesfulExtractionWithIds, asset);

        logAndReset(stopwatch, "done writeMultiBranchState");

        //TODO Why isn't that before the merged
        AssetExtractionByBranch assetExtractionByBranch = getOrCreateAssetExtractionByBranch(asset, assetContent.getBranch());

        // todo update filter options?

        logAndReset(stopwatch, "get or create last assetExtractionByBranch extraction");

        MultiBranchState stateForNewContentWithId = addids(stateForNewContent, newStateForLastSuccesfulExtractionWithIds);
        stateForNewContentWithId.getMd5ToBranchStateTextUnits().values().stream().filter(t -> t.getId() == null).forEach(t -> logger.error("Missing id for md5 - branch: {}", t.getMd5()));

        MultiBranchState previousStateForBranch = readMultiBranchStateOfBranch(assetExtractionByBranch);
        createAssetTextUnitAndMapping(assetExtractionByBranch.getAssetExtraction(), stateForNewContentWithId, previousStateForBranch);

        writeMultiBranchStateOfBranch(stateForNewContentWithId, assetExtractionByBranch);

        logAndReset(stopwatch, "done mergeCurrentIntoBaseAndCreateTextUnits");


        logger.info("Done processing asset content id: {}", assetContentId);


        justdoSomethingpollable(currentTask);

        return new PollableFutureTaskResult<>(asset);
    }


    AssetExtraction getOrCreateLastSuccessfulAssetExtraction(Asset asset) {
        return retryTemplate.execute(new RetryCallback<AssetExtraction, DataIntegrityViolationException>() {
            @Override
            public AssetExtraction doWithRetry(RetryContext context) throws DataIntegrityViolationException {

                AssetExtraction lastSuccessfulAssetExtraction = asset.getLastSuccessfulAssetExtraction();

                if (lastSuccessfulAssetExtraction == null) {
                    try {
                        logger.info("call createAssetExtraction for lastSuccessfulAssetExtraction, attempt: " + context.getRetryCount());
                        lastSuccessfulAssetExtraction = inTxLastSuccesful(asset);
                        logger.info("Saved lastSuccessfulAssetExtraction extraction: {} for asset: {}", lastSuccessfulAssetExtraction.getId(), asset.getId());
                    } catch (DataIntegrityViolationException dev) {
                        logger.info("getOrCreateLastSuccessfulAssetExtraction, DataIntegrityViolationException", dev);
                        lastSuccessfulAssetExtraction = assetRepository.findById(asset.getId())
                                .orElseThrow(() -> new RuntimeException("There must be an asset still"))
                                .getLastSuccessfulAssetExtraction();

                        if (lastSuccessfulAssetExtraction == null) {
                            throw new RuntimeException("In getOrCreateLastSuccessfulAssetExtraction assumed concurrent modification by last extraction is still null");
                        }
                    }

                }
                logger.info("last successful extraction id: {}", lastSuccessfulAssetExtraction.getId());
                return lastSuccessfulAssetExtraction;
            }
        });
    }

    @Transactional
    AssetExtraction inTxLastSuccesful(Asset asset) {
        logger.info("save in transaction the asset extraction and update the asset");
        asset = assetRepository.findById(asset.getId()).get();
        AssetExtraction assetExtraction = new AssetExtraction();
        assetExtraction.setAsset(asset);
        assetExtraction.setContentMd5("from last succesful");
        assetExtraction.setPollableTask(null);
        assetExtraction = assetExtractionRepository.save(assetExtraction);

        asset.setLastSuccessfulAssetExtraction(assetExtraction);
        asset.setDeleted(false);
        assetRepository.save(asset);

        return assetExtraction;
    }

    /**
     * TODO this is mutating objects --- review
     */
    private MultiBranchState addids(MultiBranchState stateForNewContent, MultiBranchState newStateForLastSuccesfulExtractionWithIds) {

        MultiBranchState mergedUpdated = new MultiBranchState();
        mergedUpdated.setBranches(stateForNewContent.getBranches());
        mergedUpdated.setMd5ToBranchStateTextUnits(stateForNewContent.getMd5ToBranchStateTextUnits().values().stream()
                .map(tu -> {
                    if (tu.getId() == null) {
                        tu.setId(newStateForLastSuccesfulExtractionWithIds.getMd5ToBranchStateTextUnits().get(tu.getMd5()).getId());
                    }
                    return tu;
                }).collect(ImmutableMap.toImmutableMap(BranchStateTextUnit::getMd5, identity()))
        );
        return mergedUpdated;
    }

    @Pollable(message = "remove -- this is just to make a subtask")
    void justdoSomethingpollable(@ParentTask PollableTask parentTask) {
        logger.info("that should mark the second sub task");
    }

    MultiBranchState readMultiBranchState(Asset asset) {
        Optional<String> string = structuredBlobStorage.getString(StructuredBlobStorage.Prefix.MERGE_STATE, "shared_" + asset.getId().toString());
        MultiBranchStateJson multiBranchStateJson = string.map(s -> objectMapper.readValueUnchecked(s, MultiBranchStateJson.class)).orElse(new MultiBranchStateJson());
        return convertJsonToMultiBranchState(multiBranchStateJson);
    }

    MultiBranchState readMultiBranchStateOfBranch(AssetExtractionByBranch assetExtractionByBranch) {
        Optional<String> string = structuredBlobStorage.getString(StructuredBlobStorage.Prefix.MERGE_STATE, "branch_" + assetExtractionByBranch.getId().toString());
        MultiBranchStateJson multiBranchStateJson = string.map(s -> objectMapper.readValueUnchecked(s, MultiBranchStateJson.class)).orElse(new MultiBranchStateJson());
        return convertJsonToMultiBranchState(multiBranchStateJson);
    }

    void writeMultiBranchStateOfBranch(MultiBranchState multiBranchState, AssetExtractionByBranch assetExtractionByBranch) {
        MultiBranchStateJson multiBranchStateJson = convertMultiBranchStateToJson(multiBranchState);
        structuredBlobStorage.put(
                StructuredBlobStorage.Prefix.MERGE_STATE,
                "branch_" + assetExtractionByBranch.getId().toString(),
                objectMapper.writeValueAsStringUnchecked(multiBranchStateJson),
                Retention.PERMANENT);
    }

    private MultiBranchState convertJsonToMultiBranchState(MultiBranchStateJson multiBranchStateJson) {
        MultiBranchState multiBranchState = new MultiBranchState();
        multiBranchState.setBranches(multiBranchStateJson.getBranches());

        ImmutableMap<String, com.box.l10n.mojito.ltm.merger.Branch> branchNamesToBranches = multiBranchState.getBranches().stream()
                .collect(ImmutableMap.toImmutableMap(com.box.l10n.mojito.ltm.merger.Branch::getName, identity()));

        ImmutableMap<String, BranchStateTextUnit> branchStateTextUnits = multiBranchStateJson.getMd5ToBranchStateTextUnits().stream()
                .map(t -> {
                    BranchStateTextUnit branchStateTextUnit = new BranchStateTextUnit();
                    branchStateTextUnit.setComments(t.getComments());
                    branchStateTextUnit.setCreatedDate(t.getCreatedDate());
                    branchStateTextUnit.setId(t.getId());
                    branchStateTextUnit.setMd5(t.getMd5());
                    branchStateTextUnit.setName(t.getName());
                    branchStateTextUnit.setPluralForm(t.getPluralForm());
                    branchStateTextUnit.setPluralFormOther(t.getPluralFormOther());
                    branchStateTextUnit.setSource(t.getSource());

                    ImmutableMap<com.box.l10n.mojito.ltm.merger.Branch, BranchData> branchBranchDataImmutableMap = t.getBranchNamesToBranchDatas().entrySet().stream()
                            .collect(ImmutableMap.toImmutableMap(
                                    e -> new com.box.l10n.mojito.ltm.merger.Branch(e.getKey(), branchNamesToBranches.get(e.getKey()).getCreatedAt()),
                                    Map.Entry::getValue
                            ));

                    branchStateTextUnit.setBranchToBranchDatas(branchBranchDataImmutableMap);
                    return branchStateTextUnit;
                }).collect(ImmutableMap.toImmutableMap(BranchStateTextUnit::getMd5, identity()));

        multiBranchState.setMd5ToBranchStateTextUnits(branchStateTextUnits);

        return multiBranchState;
    }

    /**
     * This should be moved in common eventually -- need to finalize the storage format... seems to be to complicate
     * to have mimic the
     */
    void writeMultiBranchState(MultiBranchState multiBranchState, Asset asset) {
        MultiBranchStateJson multiBranchStateJson = convertMultiBranchStateToJson(multiBranchState);
        structuredBlobStorage.put(
                StructuredBlobStorage.Prefix.MERGE_STATE,
                "shared_" + asset.getId().toString(),
                objectMapper.writeValueAsStringUnchecked(multiBranchStateJson),
                Retention.PERMANENT);
    }

    private MultiBranchStateJson convertMultiBranchStateToJson(MultiBranchState multiBranchState) {
        MultiBranchStateJson multiBranchStateJson = new MultiBranchStateJson();
        multiBranchStateJson.setBranches(multiBranchState.getBranches());

        ImmutableList<BranchStateTextUnitJson> branchStateTextUnitJsons = multiBranchState.getMd5ToBranchStateTextUnits().values().stream()
                .map(t -> {
                    BranchStateTextUnitJson branchStateTextUnitJson = new BranchStateTextUnitJson();
                    branchStateTextUnitJson.setComments(t.getComments());
                    branchStateTextUnitJson.setCreatedDate(t.getCreatedDate());
                    branchStateTextUnitJson.setId(t.getId());
                    branchStateTextUnitJson.setMd5(t.getMd5());
                    branchStateTextUnitJson.setName(t.getName());
                    branchStateTextUnitJson.setPluralForm(t.getPluralForm());
                    branchStateTextUnitJson.setPluralFormOther(t.getPluralFormOther());
                    branchStateTextUnitJson.setSource(t.getSource());

                    ImmutableMap<String, BranchData> branchNamesToBranchDatas = t.getBranchToBranchDatas().entrySet().stream()
                            .collect(ImmutableMap.toImmutableMap(e -> e.getKey().getName(), Map.Entry::getValue));

                    branchStateTextUnitJson.setBranchNamesToBranchDatas(branchNamesToBranchDatas);
                    return branchStateTextUnitJson;
                })
                .collect(ImmutableList.toImmutableList());

        multiBranchStateJson.setMd5ToBranchStateTextUnits(branchStateTextUnitJsons);
        return multiBranchStateJson;
    }


    void createAssetTextUnitAndMapping(AssetExtraction assetExtraction, MultiBranchState newState, MultiBranchState base) {

        Stopwatch stopwatch = Stopwatch.createStarted();

        ImmutableSet<String> unusedInBase = base.getMd5ToBranchStateTextUnits().entrySet().stream()
                .filter(e -> e.getValue().getBranchToBranchDatas().isEmpty())
                .map(Map.Entry::getKey)
                .collect(ImmutableSet.toImmutableSet());

        ImmutableSet<String> unusedInMerged = newState.getMd5ToBranchStateTextUnits().entrySet().stream()
                .filter(e -> e.getValue().getBranchToBranchDatas().isEmpty())
                .map(Map.Entry::getKey)
                .collect(ImmutableSet.toImmutableSet());

        ImmutableSet<String> usedInBase = base.getMd5ToBranchStateTextUnits().entrySet().stream()
                .filter(e -> !e.getValue().getBranchToBranchDatas().isEmpty())
                .map(Map.Entry::getKey)
                .collect(ImmutableSet.toImmutableSet());

        ImmutableSet<String> usedInMerged = newState.getMd5ToBranchStateTextUnits().entrySet().stream()
                .filter(e -> !e.getValue().getBranchToBranchDatas().isEmpty())
                .map(Map.Entry::getKey)
                .collect(ImmutableSet.toImmutableSet());

        logAndReset(stopwatch, "done preping the maps:");

        retryTemplate.execute(new RetryCallback<Void, DataIntegrityViolationException>() {
            @Override
            public Void doWithRetry(RetryContext context) throws DataIntegrityViolationException {

                if (context.getRetryCount() > 0) {
                    logger.warn("Assume concurrent modification happened in createAssetTextUnitAndMappingInTransaction, retry attempt" + context.getRetryCount(), context.getLastThrowable());
                    //TODO mabye here we need to fetch and see what's been created by other process
                }
                createAssetTextUnitAndMappingInTransaction(newState, assetExtraction, usedInBase, unusedInBase, usedInMerged, unusedInMerged);
                return null;
            }

        }, new RecoveryCallback<Void>() {
            @Override
            public Void recover(RetryContext context) throws Exception {
                logger.error("ERRRRORRRRRRRRRRRRRRR", context.getLastThrowable());
                throw new RuntimeException(context.getLastThrowable());
            }
        });
    }


    public void pseud() {
        // get asset content
        // do extraction
        // create text units --> this is done in transaction and is sensitive to concurrent modification ...

        // at this point we know we have all the text units we need
    }


    // TODO(perf) wrap in retry -- CannotAcquireLockException
// not that simple

    //    @Retryable(include = CannotAcquireLockException.class)
    @Transactional
    void createAssetTextUnitAndMappingInTransaction(MultiBranchState newState, AssetExtraction assetExtraction,
                                                    ImmutableSet<String> usedInBase, ImmutableSet<String> unusedInBase,
                                                    ImmutableSet<String> usedInMerged, ImmutableSet<String> unusedInMerged) {


        Stopwatch stopwatch = Stopwatch.createStarted();


        Sets.SetView<String> removed = Sets.difference(unusedInMerged, unusedInBase);
        Sets.SetView<String> added = Sets.difference(usedInMerged, usedInBase);

        removeWithRetry(assetExtraction, removed);
        logAndReset(stopwatch, "Removing unused 1 by 1 normal process:");

        cleanupwithretry(assetExtraction, usedInMerged);

        logAndReset(stopwatch, "Removing ATU from check data:");

        LoadingCache<String, Branch> branches = CacheBuilder.newBuilder().build(CacheLoader.from(name -> {
            if ("$$MOJITO_DEFAULT$$".equals(name)) {
                name = null;
            }
            return branchRepository.findByNameAndRepository(name, assetExtraction.getAsset().getRepository());
        }));

        // TOOD(perf) calling that in cleanupwithretry (something similar is done in createTmTextUnits too) -- just POC ---
        List<TextUnitIdMd5DTO> byAssetExtraction = assetTextUnitRepository.findMd5ByAssetExtraction(assetExtraction);
        ImmutableSet<String> skip = byAssetExtraction.stream().map(TextUnitIdMd5DTO::getMd5).collect(ImmutableSet.toImmutableSet());

        newState.getMd5ToBranchStateTextUnits().values().stream()
                .filter(tu -> added.contains(tu.getMd5()))
                .filter(tu -> !skip.contains(tu.getMd5()))
                .forEach(textUnit -> {
//                    logger.warn("Add asset text unit: {}", objectMapper.writeValueAsStringUnchecked(textUnit));

                    // TODO is the order OK?
                    Map.Entry<com.box.l10n.mojito.ltm.merger.Branch, BranchData> branchData = textUnit.getBranchToBranchDatas().entrySet().stream()
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("there must be at least one branch because of previous filtering"));

                    // TODO cache, avoid lookup if current branch, etc
                    Branch byNameAndRepository = branches.getUnchecked(branchData.getKey().getName());

                    // 2x writes: atu + mappingn - 10k string ... 20k row and also all the content of ATU is useless at that point

                    Preconditions.checkNotNull(assetExtraction.getId());

                    try {
                        logger.info("add asset text unit, extraction id: {} - name: {}", assetExtraction.getId(), textUnit.getName());
                        AssetTextUnit assetTextUnit = createAssetTextUnit(
                                assetExtraction.getId(),
                                textUnit.getName(),
                                textUnit.getSource(),
                                textUnit.getComments(),
                                pluralFormService.findByPluralFormString(textUnit.getPluralForm()),
                                textUnit.getPluralFormOther(),
                                false,
                                ImmutableSet.copyOf(branchData.getValue().getUsages()),
                                byNameAndRepository);

                        AssetTextUnitToTMTextUnit assetTextUnitToTMTextUnit = new AssetTextUnitToTMTextUnit();
                        assetTextUnitToTMTextUnit.setAssetExtraction(assetExtraction);
                        assetTextUnitToTMTextUnit.setAssetTextUnit(assetTextUnit);
                        assetTextUnitToTMTextUnit.setTmTextUnit(tmTextUnitRepository.getOne(textUnit.getId()));
                        assetTextUnitToTMTextUnitRepository.save(assetTextUnitToTMTextUnit);

                    } catch (DataIntegrityViolationException d) {
                        logger.warn("Concurrent modification for createAssetTextUnit: {}, {}", textUnit.getMd5(), textUnit.getName());
                        throw d;
                    }
                });

        logAndReset(stopwatch, "Add ATU normal process:");
    }

    //    @Retryable
    private void cleanupwithretry(AssetExtraction assetExtraction, ImmutableSet<String> usedInMerged) {

        Stopwatch stopwatch = Stopwatch.createStarted();

        /// TODO this is recovery code shouldn't be run all the time
        /// this is costly but help keeping things consistent!
        List<TextUnitIdMd5DTO> byAssetExtraction = assetTextUnitRepository.findMd5ByAssetExtraction(assetExtraction);

        logAndReset(stopwatch, "Getting all text units to check:");

        byAssetExtraction.stream()
                .filter(t -> !usedInMerged.contains(t.getMd5())) // TODO or unused in merged ?? kind of the same
                // TODO removing this can step on a concurrent modification ... this is not
                .forEach(assetTextUnit -> {
                    logger.warn("removing extra text unit: {}", assetTextUnit.getId());
                    assetTextUnitToTMTextUnitRepository.deleteByAssetTextUnitId(assetTextUnit.getId());
                    assetTextUnitRepository.deleteById(assetTextUnit.getId());
                });
    }

    //    @Retryable
    private void removeWithRetry(AssetExtraction lastSuccessfulAssetExtraction, Sets.SetView<String> removed) {
        removed.stream().forEach(md5 -> {
            logger.warn("Remove for md5: {}", md5);

            // for concurrent modification, need to be sure that we're not removing  entries that has been created by other
            assetTextUnitRepository.findByAssetExtractionIdAndMd5(lastSuccessfulAssetExtraction.getId(), md5)
                    .ifPresent(assetTextUnit -> {
                        logger.warn("removing atu: {}", assetTextUnit.getId());
                        assetTextUnitToTMTextUnitRepository.deleteByAssetTextUnitId(assetTextUnit.getId());
                        assetTextUnitRepository.deleteById(assetTextUnit.getId());
                    });
        });
    }

    private void logAndReset(Stopwatch stopwatch, String s) {
        logger.info(s + " {}", stopwatch.elapsed().toString());
        stopwatch.reset().start();
    }

    void performLeveraging(ImmutableList<Match> matchesForSourceLeveraging) {
        matchesForSourceLeveraging.stream()
                .forEach(match -> {
                    LeveragerByTmTextUnit leveragerByTmTextUnit = new LeveragerByTmTextUnit(match.getMatch().getId());
                    TMTextUnit tmTextUnit = tmTextUnitRepository.findById(match.getSource().getId()).get();
                    leveragerByTmTextUnit.performLeveragingFor(new ArrayList<>(Arrays.asList(tmTextUnit)), null, null);
                });
    }

    /**
     * TODO this mutates objects...
     */
    MultiBranchState getStateWithMergedIds(MultiBranchState merged, ImmutableMap<String, Long> createdTextUnits) {
        MultiBranchState mergedUpdated = new MultiBranchState();
        mergedUpdated.setBranches(merged.getBranches());
        mergedUpdated.setMd5ToBranchStateTextUnits(merged.getMd5ToBranchStateTextUnits().values().stream()
                .map(tu -> {
                    if (tu.getId() == null) {
                        tu.setId(createdTextUnits.get(tu.getMd5()));
                    }
                    return tu;
                }).collect(ImmutableMap.toImmutableMap(BranchStateTextUnit::getMd5, identity()))
        );
        return mergedUpdated;
    }

    ImmutableList<Match> getLeveragingMatches(ImmutableMap<String, BranchStateTextUnit> textUnitsToCreate, MultiBranchState base) {

        ImmutableSet<String> namesToCreate = textUnitsToCreate.values().stream()
                .map(BranchStateTextUnit::getName)
//                .peek(n -> logger.info("nametocreate: " + n))
                .collect(ImmutableSet.toImmutableSet());

        ImmutableListMultimap<String, BranchStateTextUnit> candidateByNames = base.getMd5ToBranchStateTextUnits().values().stream()
                .filter(tu -> namesToCreate.contains(tu.getName()))
                .collect(ImmutableListMultimap.toImmutableListMultimap(BranchStateTextUnit::getName, identity()));

        ImmutableSet<String> sourcesToCreate = textUnitsToCreate.values().stream()
                .map(BranchStateTextUnit::getSource)
                .collect(ImmutableSet.toImmutableSet());

        ImmutableListMultimap<String, BranchStateTextUnit> candidateBySources = base.getMd5ToBranchStateTextUnits().values().stream()
                .filter(tu -> sourcesToCreate.contains(tu.getSource()))
                .collect(ImmutableListMultimap.toImmutableListMultimap(BranchStateTextUnit::getSource, identity()));

        return textUnitsToCreate.values().stream()
                .map(tu -> {
                    // This mimimics the current source leveraging - the logic could be improved quite a bit:
                    // real detection of refactoring in diff command (if diff command not used probably drop the match based name anyway)
                    // coupled with sensible matching here
                    BranchStateTextUnit match = null;
                    boolean uniqueMatch = false;
                    boolean translationNeededIfUniqueMatch = true;

                    if (match == null) {
                        ImmutableList<BranchStateTextUnit> byNameAndContentAndUsed = candidateByNames.get(tu.getMd5()).stream()
                                .filter(m -> m.getSource().equals(tu.getSource()))
                                .filter(usedBranchStateTextUnit())
                                .collect(ImmutableList.toImmutableList());

                        match = byNameAndContentAndUsed.stream().findFirst().orElse(null);
                        uniqueMatch = byNameAndContentAndUsed.size() == 1;
                        translationNeededIfUniqueMatch = false;
                    }

                    if (match == null) {
                        ImmutableList<BranchStateTextUnit> byNameAndUsed = candidateByNames.get(tu.getName()).stream()
                                .filter(usedBranchStateTextUnit())
                                .collect(ImmutableList.toImmutableList());

                        match = byNameAndUsed.stream().findFirst().orElse(null);
                        uniqueMatch = byNameAndUsed.size() == 1;
                        translationNeededIfUniqueMatch = true;
                    }

                    if (match == null) {
                        ImmutableList<BranchStateTextUnit> byContent = candidateBySources.get(tu.getSource()).stream()
                                .filter(usedBranchStateTextUnit())
                                .collect(ImmutableList.toImmutableList());

                        match = byContent.stream().findFirst().orElse(null);
                        uniqueMatch = byContent.size() == 1;
                        translationNeededIfUniqueMatch = true;
                    }

                    if (match == null) {
                        ImmutableList<BranchStateTextUnit> byNameAndContentAndUnused = candidateByNames.get(tu.getMd5()).stream()
                                .filter(m -> m.getSource().equals(tu.getSource()))
                                .filter(unusedBranchStateTextUnit())
                                .collect(ImmutableList.toImmutableList());

                        match = byNameAndContentAndUnused.stream().findFirst().orElse(null);
                        uniqueMatch = byNameAndContentAndUnused.size() == 1;
                        translationNeededIfUniqueMatch = false;
                    }

                    if (match != null) {
                        logger.warn("Match: " + ObjectMapper.withIndentedOutput().writeValueAsStringUnchecked(match));
                    }

                    return match == null ? null : new Match(tu, match, uniqueMatch, translationNeededIfUniqueMatch);
                })
                .filter(Objects::nonNull)
                .collect(ImmutableList.toImmutableList());
    }

    Predicate<BranchStateTextUnit> usedBranchStateTextUnit() {
        return m -> !m.getBranchToBranchDatas().isEmpty();
    }

    Predicate<BranchStateTextUnit> unusedBranchStateTextUnit() {
        return m -> m.getBranchToBranchDatas().isEmpty();
    }

    private Comparator<String> equalFirstComparator() {
        return (o1, o2) -> o1.equals(o2) ? -1 : 0;
    }

    /**
     * Text units in current that don't have a matching entry by MD5 in the base state.
     */
    ImmutableMap<String, BranchStateTextUnit> getTextUnitsToCreate(MultiBranchState multiBranchState) {
        return multiBranchState.getMd5ToBranchStateTextUnits().values().stream()
                .filter(tu -> tu.getId() == null)
                .collect(ImmutableMap.toImmutableMap(BranchStateTextUnit::getMd5, identity()));
    }

    /**
     * @param toCreateTmTextUnits
     * @return
     */
    ImmutableMap<String, Long> createTextUnits(final ImmutableMap<String, BranchStateTextUnit> textUnits, User createdByUser, Asset asset) {
        return retryTemplate.execute(new RetryCallback<ImmutableMap<String, Long>, DataIntegrityViolationException>() {
            @Override
            public ImmutableMap<String, Long> doWithRetry(RetryContext context) throws DataIntegrityViolationException {

                ImmutableMap<String, BranchStateTextUnit> attemptTextUnits = textUnits;
                ImmutableMap<String, Long> recovered = ImmutableMap.of();

                if (context.getRetryCount() > 0) {
                    logger.warn("Assume concurrent modification happened, need to re-filter the list");

                    // TODO More optimized way to filter existing one.... micrometer this?
                    // TODO this doesn't recover the branch information!! re-read from base in that case? instead of using the DB?
                    // this would be catch by the final merge?
                    recovered = tmTextUnitRepository.getTextUnitIdMd5DTOByAssetId(asset.getId()).stream()
                            .collect(ImmutableMap.toImmutableMap(TextUnitIdMd5DTO::getMd5, TextUnitIdMd5DTO::getId));

                    final ImmutableMap<String, Long> finalRecovered = recovered;
                    attemptTextUnits = textUnits.values().stream().filter(t -> !finalRecovered.containsKey(t.getMd5()))
                            .collect(ImmutableMap.toImmutableMap(BranchStateTextUnit::getMd5, identity()));
                }

                ImmutableMap<String, Long> tmTextUnits = createTmTextUnits(attemptTextUnits, createdByUser, asset);

                ImmutableMap<String, Long> result = Stream.concat(tmTextUnits.entrySet().stream(), recovered.entrySet().stream())
                        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

                return result;
            }
        });
    }

    @Transactional
    protected ImmutableMap<String, Long> createTmTextUnits(ImmutableMap<String, BranchStateTextUnit> textUnits, User createdByUser, Asset asset) {
        logger.debug("Create TMTextUnit, tmId: {}, assetId: {}", asset.getId());

        ImmutableMap<String, Long> createdTmTextUnits = textUnits.values().stream()
                .map(bstu -> {
                    TMTextUnit addTMTextUnit = tmService.addTMTextUnit(
                            asset.getRepository().getTm(),
                            asset,
                            bstu.getName(),
                            bstu.getSource(),
                            bstu.getComments(),
                            createdByUser,
                            null,
                            pluralFormService.findByPluralFormString(bstu.getPluralForm()),
                            bstu.getPluralFormOther());

                    return addTMTextUnit;
                })
                .collect(ImmutableMap.toImmutableMap(TMTextUnit::getMd5, BaseEntity::getId));

        return createdTmTextUnits;
    }


    @Transactional // TODO restrict to only db writting
    @Pollable(message = "Extracting text units from asset")
    public void performAssetExtraction(
            AssetExtraction assetExtraction,
            FilterConfigIdOverride filterConfigIdOverride,
            List<String> filterOptions,
            List<String> md5sToSkip,
            @ParentTask PollableTask parentTask) throws UnsupportedAssetFilterTypeException {


        AssetContent assetContent = assetExtraction.getAssetContent();
        Asset asset = assetExtraction.getAsset();

        List<AssetExtractorTextUnit> assetExtractorTextUnits;

        if (assetContent.isExtractedContent()) {
            assetExtractorTextUnits = objectMapper.readValueUnchecked(assetContent.getContent(), new TypeReference<List<AssetExtractorTextUnit>>() {
            });
            assetExtractorTextUnits = assetExtractorTextUnits.stream().filter(assetExtractorTextUnit -> {
                String md5 = textUnitUtils.computeTextUnitMD5(
                        assetExtractorTextUnit.getName(),
                        assetExtractorTextUnit.getSource(),
                        assetExtractorTextUnit.getComments());
                return !md5sToSkip.contains(md5);
            }).collect(Collectors.toList());

        } else {
            assetExtractorTextUnits = assetExtractor.getAssetExtractorTextUnitsForAsset(asset.getPath(),
                    assetContent.getContent(), filterConfigIdOverride, filterOptions, md5sToSkip);
        }

        assetExtractorTextUnits.forEach(textUnit -> {
            createAssetTextUnit(
                    assetExtraction.getId(),
                    textUnit.getName(),
                    textUnit.getSource(),
                    textUnit.getComments(),
                    pluralFormService.findByPluralFormString(textUnit.getPluralForm()),
                    textUnit.getPluralFormOther(),
                    false,
                    textUnit.getUsages(),
                    assetExtraction.getAssetContent().getBranch());
        });
    }

    @Pollable(message = "Extracting text units from asset")
    public MultiBranchState performAssetExtractionNew(
            AssetContent assetContent,
            FilterConfigIdOverride filterConfigIdOverride,
            List<String> filterOptions,
            @ParentTask PollableTask parentTask) throws UnsupportedAssetFilterTypeException {

        Stopwatch stopwatch = Stopwatch.createStarted();

        Asset asset = assetContent.getAsset();

        List<AssetExtractorTextUnit> assetExtractorTextUnits;

        if (assetContent.isExtractedContent()) {
            assetExtractorTextUnits = objectMapper.readValueUnchecked(assetContent.getContent(), new TypeReference<List<AssetExtractorTextUnit>>() {
            });
        } else {
            assetExtractorTextUnits = assetExtractor.getAssetExtractorTextUnitsForAsset(asset.getPath(),
                    assetContent.getContent(), filterConfigIdOverride, filterOptions, Collections.emptyList());
            logAndReset(stopwatch, "done getAssetExtractorTextUnitsForAsset:");
        }

        com.box.l10n.mojito.ltm.merger.Branch branch = new com.box.l10n.mojito.ltm.merger.Branch();
        branch.setName(assetContent.getBranch().getName() == null ? "$$MOJITO_DEFAULT$$" : assetContent.getBranch().getName());
        branch.setCreatedAt(assetContent.getBranch().getCreatedDate().toDate()); // TODO align names and format

        MultiBranchState multiBranchState = assetExtractorTextUnitsToMultiBranchState.convert(assetExtractorTextUnits, branch);
        multiBranchState.setBranches(ImmutableSet.of(branch));
        return multiBranchState;
    }

    List<String> getMd5sToSkip(AssetContent assetContent, Asset asset) {
        List<String> skipMd5s = Collections.emptyList();

        if (!isNullOrPrimaryBranch(assetContent)) {
            skipMd5s = assetTextUnitRepository.findMd5ByAssetExtractionAndBranch(asset.getLastSuccessfulAssetExtraction(), assetContent.getBranch());
        }

        return skipMd5s;
    }

    boolean isNullOrPrimaryBranch(AssetContent assetContent) {
        return PRIMARY_BRANCH.equals(assetContent.getBranch().getName());
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

                AssetExtractionByBranch assetExtractionByBranch = getOrCreateAssetExtractionByBranch(asset, branch);

                assetExtractionByBranch.setAssetExtraction(assetExtraction);
                assetExtractionByBranch.setDeleted(false);

                assetExtractionByBranch = assetExtractionByBranchRepository.save(assetExtractionByBranch);

                return assetExtractionByBranch;
            }
        });
    }

    AssetExtractionByBranch getOrCreateAssetExtractionByBranch(Asset asset, Branch branch) {
        logger.info("getOrCreateAssetExtractionByBranch");
        AssetExtractionByBranch assetExtractionByBranch = getAssetExtractionByBranch(asset, branch).orElseGet(() -> {
            logger.info("getOrCreateAssetExtractionByBranch create aebb");
            try {
                return inTxBranch(asset, branch);
            } catch (DataIntegrityViolationException dev) {
                logger.info("DataIntegrityViolationException in getOrCreateAssetExtractionByBranch", dev);
                return getAssetExtractionByBranch(asset, branch).orElseThrow(() -> {
                    String msg = "Assummed concurrent modification in getOrCreateAssetExtractionByBranch, re-fetch should not be null";
                    logger.error(msg, dev);
                    return new RuntimeException(msg, dev);
                });
            }
        });

        return assetExtractionByBranch;
    }

    @Transactional
    AssetExtractionByBranch inTxBranch(Asset asset, Branch branch) {
        logger.info("inTxBranch");
        AssetExtractionByBranch aebb = new AssetExtractionByBranch();
        aebb.setAsset(asset);
        aebb.setBranch(branch);
        AssetExtraction assetExtraction = new AssetExtraction();
        assetExtraction.setAsset(asset);
        assetExtraction.setContentMd5("from processing a branch");
        aebb.setAssetExtraction(assetExtraction);
        assetExtractionRepository.save(assetExtraction);
        return assetExtractionByBranchRepository.save(aebb);
    }

    Optional<AssetExtractionByBranch> getAssetExtractionByBranch(Asset asset, Branch branch) {
        return assetExtractionByBranchRepository.findByAssetAndBranch(asset, branch);
    }

    public boolean hasMoreActiveBranches(Asset asset, Branch branch) {
        int numberOfBranch = assetExtractionByBranchRepository.countByAssetAndDeletedFalseAndBranchNot(asset, branch);
        logger.debug("There are {} additional branches", numberOfBranch);
        return numberOfBranch > 0;
    }

    public String getMergedContentMd5(List<AssetExtractionByBranch> sordedAssetExtractionByBranches) {

        String appendedContentMd5 = sordedAssetExtractionByBranches.stream()
                .map(AssetExtractionByBranch::getAssetExtraction)
                .map(AssetExtraction::getContentMd5)
                .collect(Collectors.joining());

        String md5 = DigestUtils.md5Hex(appendedContentMd5);
        logger.debug("getMergedContentMd5, res: {}", md5);
        return md5;
    }

    public String getMergedFilterOptionsMd5(List<AssetExtractionByBranch> sordedAssetExtractionByBranches) {

        final AtomicBoolean overriden = new AtomicBoolean(false);

        String appendedFilterOptionMd5 = sordedAssetExtractionByBranches.stream()
                .map(AssetExtractionByBranch::getAssetExtraction)
                .map(AssetExtraction::getFilterOptionsMd5)
                .collect(Collectors.joining());

        String md5 = DigestUtils.md5Hex(appendedFilterOptionMd5);
        logger.debug("getMergedFilterOptionsMd5, res: {}", md5);
        return md5;
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
        List<AssetExtractionByBranch> sortedAssetExtractionByBranches = getSordedAssetExtractionByBranches(asset);

        logger.debug("Create a new asset extraction that will contain the merge");
        AssetExtraction mergedAssetExtraction = createAssetExtraction(asset, pollableTask);

        mergedAssetExtraction.setContentMd5(getMergedContentMd5(sortedAssetExtractionByBranches));
        mergedAssetExtraction.setFilterOptionsMd5(getMergedFilterOptionsMd5(sortedAssetExtractionByBranches));

        HashMap<String, AssetTextUnit> mergedAssetTextUnits = new LinkedHashMap<>();

        for (AssetExtractionByBranch assetExtractionByBranch : sortedAssetExtractionByBranches) {
            logger.debug("Start processing branch: {} for asset: {}", assetExtractionByBranch.getBranch().getName(), assetExtractionByBranch.getAsset().getPath());
            AssetExtraction assetExtraction = assetExtractionByBranch.getAssetExtraction();

            logger.debug("Get asset text units of the branch to be merged");
            List<AssetTextUnit> assetTextUnitsToMerge = assetTextUnitRepository.findByAssetExtraction(assetExtraction);

            for (AssetTextUnit assetTextUnit : assetTextUnitsToMerge) {
                AssetTextUnit copy = copyAssetTextUnit(assetTextUnit);
                copy.setBranch(assetExtractionByBranch.getBranch());
                copy.setAssetExtraction(mergedAssetExtraction);
                mergedAssetTextUnits.putIfAbsent(copy.getMd5(), copy);
            }
        }

        assetTextUnitRepository.saveAll(mergedAssetTextUnits.values());

        Long mergedAssetExtractionId = mergedAssetExtraction.getId();
        Long tmId = asset.getRepository().getTm().getId();
        long mapExactMatches = assetMappingService.mapExactMatches(mergedAssetExtractionId, tmId, asset.getId());
        logger.debug("{} text units were mapped for the merged extraction with id: {} and tmId: {}", mapExactMatches, mergedAssetExtractionId, tmId);

        return mergedAssetExtraction;
    }


    public List<AssetExtractionByBranch> getSordedAssetExtractionByBranches(Asset asset) {
        List<AssetExtractionByBranch> assetExtractionByBranches = assetExtractionByBranchRepository.findByAssetAndDeletedFalse(asset);
        assetExtractionByBranches = sortedAssetExtractionByBranches(assetExtractionByBranches);
        logger.debug("getSordedAssetExtractionByBranches, number of branches: {}", assetExtractionByBranches.size());
        return assetExtractionByBranches;
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

        if (useNewImplementation) {
            deleteAssetBranchNew(asset, branchName);
            return;
        }

        Branch branch = branchRepository.findByNameAndRepository(branchName, asset.getRepository());
        AssetExtractionByBranch assetExtractionByBranch = getAssetExtractionByBranch(asset, branch)
                .orElseThrow(() -> new RuntimeException("There must be an assetExtractionByBranch for delete"));
        assetExtractionByBranch.setDeleted(true);
        assetExtractionByBranchRepository.save(assetExtractionByBranch);
        AssetExtraction assetExtractionForMultipleBranches = createAssetExtractionForMultipleBranches(asset, null);
        markAssetExtractionAsLastSuccessfulWithRetry(asset, assetExtractionForMultipleBranches);
    }

    public void deleteAssetBranchNew(Asset asset, String branchName) {
        Branch branch = branchRepository.findByNameAndRepository(branchName, asset.getRepository());
        AssetExtractionByBranch assetExtractionByBranch = getAssetExtractionByBranch(asset, branch)
                .orElseThrow(() -> new RuntimeException("There must be an assetExtractionByBranch for delete"));
        assetExtractionByBranch.setDeleted(true);
        assetExtractionByBranchRepository.save(assetExtractionByBranch);

        MultiBranchState multiBranchState = readMultiBranchState(asset);
        MultiBranchState branchRemoved = multiBranchStateMerger.removeBranch(multiBranchState, branchName);

        createAssetTextUnitAndMapping(asset.getLastSuccessfulAssetExtraction(), branchRemoved, multiBranchState);
        // need to update the md5?
    }

    AssetTextUnit copyAssetTextUnit(AssetTextUnit assetTextUnit) {

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

    public PollableFuture<Void> processAssetAsync(
            Long assetContentId,
            FilterConfigIdOverride filterConfigIdOverride,
            List<String> filterOptions,
            Long parentTaskId) throws UnsupportedAssetFilterTypeException, InterruptedException, AssetExtractionConflictException {

        ProcessAssetJobInput processAssetJobInput = new ProcessAssetJobInput();
        processAssetJobInput.setAssetContentId(assetContentId);
        processAssetJobInput.setFilterConfigIdOverride(filterConfigIdOverride);
        processAssetJobInput.setFilterOptions(filterOptions);

        String pollableMessage = MessageFormat.format("Process asset content, id: {0}", assetContentId.toString());

        QuartzJobInfo quartzJobInfo = QuartzJobInfo.newBuilder(ProcessAssetJob.class)
                .withInput(processAssetJobInput)
                .withMessage(pollableMessage)
                .withParentId(parentTaskId)
                .withExpectedSubTaskNumber(2)
                .build();

        return quartzPollableTaskScheduler.scheduleJob(quartzJobInfo);
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
    public void markAssetExtractionAsLastSuccessfulWithRetry(Asset asset, AssetExtraction assetExtraction) {
        markAssetExtractionAsLastSuccessful(asset, assetExtraction);
    }

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
     * @param asset         The asset associated to the AssetExtraction
     * @param filterOptions
     * @return The created assetExtraction instance
     */
    @Transactional
    public AssetExtraction createAssetExtraction(AssetContent assetContent, PollableTask pollableTask, List<String> filterOptions) {
        AssetExtraction assetExtraction = new AssetExtraction();

        assetExtraction.setAsset(assetContent.getAsset());
        assetExtraction.setAssetContent(assetContent);
        assetExtraction.setContentMd5(assetContent.getContentMd5());
        assetExtraction.setFilterOptionsMd5(filterOptionsMd5Builder.md5(filterOptions));
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

    public void recreateMergedAssetExtraction(Asset asset, Branch branch) {
        MultiBranchState previousState = readMultiBranchState(asset);

        MultiBranchState newState = new MultiBranchState();

        ImmutableSet<com.box.l10n.mojito.ltm.merger.Branch> newBranches = previousState.getBranches().stream()
                .filter(b -> !(branch.getName() == null ? "$$MOJITO_DEFAULT$$" : branch.getName()).equals(b.getName()))
                .collect(ImmutableSet.toImmutableSet());

        newState.setBranches(newBranches);

        ImmutableMap<String, BranchStateTextUnit> branchStateTextUnitImmutableMap = previousState.getMd5ToBranchStateTextUnits().values().stream()
                .map(btu -> {
                    BranchStateTextUnit branchStateTextUnit = new BranchStateTextUnit();
                    branchStateTextUnit.setId(btu.getId());
                    branchStateTextUnit.setMd5(btu.getMd5());
                    branchStateTextUnit.setComments(btu.getComments());
                    branchStateTextUnit.setCreatedDate(btu.getCreatedDate());
                    branchStateTextUnit.setSource(btu.getSource());
                    branchStateTextUnit.setName(btu.getName());
                    branchStateTextUnit.setPluralFormOther(btu.getPluralFormOther());
                    branchStateTextUnit.setPluralForm(btu.getPluralForm());

                    branchStateTextUnit.setBranchToBranchDatas(
                            btu.getBranchToBranchDatas().entrySet().stream()
                                    .filter(entry ->
                                            !(branch.getName() == null ? "$$MOJITO_DEFAULT$$" : branch.getName()).equals(entry.getKey().getName()))
                                    .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue))
                    );

                    return branchStateTextUnit;
                }).collect(ImmutableMap.toImmutableMap(BranchStateTextUnit::getMd5, identity()));

        newState.setMd5ToBranchStateTextUnits(branchStateTextUnitImmutableMap);

        createAssetTextUnitAndMapping(asset.getLastSuccessfulAssetExtraction(), newState, previousState);
        writeMultiBranchState(newState, asset);
    }
}
