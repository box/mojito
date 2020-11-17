package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.localtm.merger.Branch;
import com.box.l10n.mojito.localtm.merger.BranchData;
import com.box.l10n.mojito.localtm.merger.BranchStateTextUnit;
import com.box.l10n.mojito.localtm.merger.MultiBranchState;
import com.box.l10n.mojito.okapi.TextUnitUtils;
import com.box.l10n.mojito.service.assetTextUnit.AssetTextUnitRepository;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.micrometer.core.annotation.Timed;
import org.checkerframework.checker.units.qual.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MultiBranchStateService {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(MultiBranchStateService.class);

    MultiBranchStateBlobStorage multiBranchStateBlobStorage;

    AssetTextUnitRepository assetTextUnitRepository;

    AssetTextUnitToTMTextUnitRepository assetTextUnitToTMTextUnitRepository;

    TextUnitUtils textUnitUtils;

    LocalBranchToEntityBranchConverter localBranchToEntityBranchConverter;

    public MultiBranchStateService(MultiBranchStateBlobStorage multiBranchStateBlobStorage,
                                   AssetTextUnitRepository assetTextUnitRepository,
                                   AssetTextUnitToTMTextUnitRepository assetTextUnitToTMTextUnitRepository,
                                   TextUnitUtils textUnitUtils,
                                   LocalBranchToEntityBranchConverter localBranchToEntityBranchConverter) {
        this.multiBranchStateBlobStorage = Preconditions.checkNotNull(multiBranchStateBlobStorage);
        this.assetTextUnitRepository = Preconditions.checkNotNull(assetTextUnitRepository);
        this.assetTextUnitToTMTextUnitRepository = Preconditions.checkNotNull(assetTextUnitToTMTextUnitRepository);
        this.textUnitUtils = Preconditions.checkNotNull(textUnitUtils);
        this.localBranchToEntityBranchConverter = Preconditions.checkNotNull(localBranchToEntityBranchConverter);
    }

    @Timed("MultiBranchStateService.getMultiBranchStateForAssetExtractionId")
    public MultiBranchState getMultiBranchStateForAssetExtractionId(long assetExtractionId, long version) {
        return multiBranchStateBlobStorage.getMultiBranchStateForAssetExtractionId(assetExtractionId, version)
                .orElseGet(() -> {
                    logger.debug("No MultiBranchState for asset extraction id: {}", assetExtractionId);
                    return getAndSaveInitialMultiBranchStateFromDatabase(assetExtractionId, version);
                });
    }

    public void deleteMultiBranchStateForAssetExtractionId(long assetExtractionId, long version) {
        multiBranchStateBlobStorage.deleteMultiBranchStateForAssetExtractionId(assetExtractionId, version);
    }

    @Timed("MultiBranchStateService.putMultiBranchStateForAssetExtractionId")
    public void putMultiBranchStateForAssetExtractionId(MultiBranchState multiBranchState, long assetExtractionId, long version) {
        multiBranchStateBlobStorage.putMultiBranchStateForAssetExtractionId(multiBranchState, assetExtractionId, version);
    }

    MultiBranchState getAndSaveInitialMultiBranchStateFromDatabase(long assetExtractionId, long version) {
        MultiBranchState multiBranchStateForAssetExtractionId = getInitialMultiBranchStateFromDatabase(assetExtractionId, version);
        logger.debug("Save the initial state for asset extraction id: {}", assetExtractionId);
        multiBranchStateBlobStorage.putMultiBranchStateForAssetExtractionId(multiBranchStateForAssetExtractionId, assetExtractionId, 0);
        return multiBranchStateForAssetExtractionId;
    }

    /**
     * Build an initial MultiBranchState for an asset extraction from the database.
     * <p>
     * This is needed to migrate from the old extraction logic to the new one.
     * <p>
     * When working on new repositories (or assets) the data returned by the database will be empty, and the MultiBranchState
     * will also be empty, so it should have minor impact later on the new implementation. In other words, if not
     * a migrated repository, this code is not needed.
     *
     * @param assetExtractionId
     * @param version
     * @return
     */
    @Timed("MultiBranchStateService.getInitialMultiBranchStateFromDatabase")
    MultiBranchState getInitialMultiBranchStateFromDatabase(long assetExtractionId, long version) {

        logger.debug("Get initial MultiBranchState for asset extraction id: {}", assetExtractionId);
        List<AssetTextUnit> assetTextUnits = assetTextUnitRepository.findByAssetExtractionId(assetExtractionId);

        ImmutableList<BranchStateTextUnit> branchStateTextUnits = assetTextUnits.stream()
                .map(assetTextUnit -> {
                    BranchData branchData = BranchData.of()
                            .withUsages(assetTextUnit.getUsages() == null ?
                                    ImmutableSet.of() :
                                    ImmutableSet.copyOf(assetTextUnit.getUsages()));

                    String branchName = localBranchToEntityBranchConverter.branchEntityToLocalBranchName(assetTextUnit.getBranch());

                    ImmutableMap<String, BranchData> branchNameToBranchDatas = ImmutableMap.of(branchName, branchData);

                    BranchStateTextUnit.Builder builder = BranchStateTextUnit.builder();

                    // TODO(perf) should we batch the lookup: assetTextUnitToTMTextUnitRepository???
                    builder
                            .tmTextUnitId(assetTextUnitToTMTextUnitRepository.findTmTextUnitId(assetExtractionId, assetTextUnit.getId()).orElse(null))
                            .assetTextUnitId(assetTextUnit.getId())
                            .source(assetTextUnit.getContent())
                            .name(assetTextUnit.getName())
                            .comments(assetTextUnit.getComment())
                            .createdDate(assetTextUnit.getCreatedDate())
                            .branchNameToBranchDatas(branchNameToBranchDatas)
                            .md5(textUnitUtils.computeTextUnitMD5(
                                    assetTextUnit.getName(),
                                    assetTextUnit.getContent(),
                                    assetTextUnit.getComment()));

                    if (assetTextUnit.getPluralForm() != null) {
                        builder.pluralForm(assetTextUnit.getPluralForm().getName())
                                .pluralFormOther(assetTextUnit.getPluralFormOther());
                    }

                    return builder.build();
                })
                .collect(ImmutableList.toImmutableList());

        ImmutableSet<Branch> branches = assetTextUnits.stream()
                .map(AssetTextUnit::getBranch)
                .map(b -> localBranchToEntityBranchConverter.convertEntityBranchToLocaleBranch(b))
                .collect(ImmutableSet.toImmutableSet());

        return MultiBranchState.of()
                .withBranchStateTextUnits(branchStateTextUnits)
                .withBranches(branches);
    }

}
