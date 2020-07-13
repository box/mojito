package com.box.l10n.mojito.ltm.merger;

import com.box.l10n.mojito.okapi.TextUnitUtils;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Component
public class AssetExtractorTextUnitsToMultiBranchState {

    TextUnitUtils textUnitUtils;

    @Autowired
    public AssetExtractorTextUnitsToMultiBranchState(TextUnitUtils textUnitUtils) {
        this.textUnitUtils = textUnitUtils;
    }

    public MultiBranchState convert(List<AssetExtractorTextUnit> assetExtractorTextUnits, Branch branch) {

        MultiBranchState multiBranchState = new MultiBranchState();
        multiBranchState.setBranches(ImmutableSet.of(branch));

        Date branchStateTextUnitCreated = new Date();

        ImmutableMap<String, BranchStateTextUnit> branchStateTextUnitLinkedHashMap = assetExtractorTextUnits.stream().map(assetExtractorTextUnit -> {
            return convertToBranchStateTextUnit(assetExtractorTextUnit, branch, branchStateTextUnitCreated);
        }).collect(ImmutableMap.toImmutableMap(BranchStateTextUnit::getMd5, Function.identity()));

        multiBranchState.setMd5ToBranchStateTextUnits(branchStateTextUnitLinkedHashMap);
        return multiBranchState;
    }

    BranchStateTextUnit convertToBranchStateTextUnit(AssetExtractorTextUnit assetExtractorTextUnit, Branch branch, Date createdDate) {
        BranchStateTextUnit branchStateTextUnit = new BranchStateTextUnit();

        branchStateTextUnit.setSource(assetExtractorTextUnit.getSource());
        branchStateTextUnit.setName(assetExtractorTextUnit.getName());
        branchStateTextUnit.setComments(assetExtractorTextUnit.getComments());

        branchStateTextUnit.setPluralForm(assetExtractorTextUnit.getPluralForm());
        branchStateTextUnit.setPluralFormOther(assetExtractorTextUnit.getPluralFormOther());

        BranchData branchData = new BranchData();
        branchData.getUsages().addAll(Optional.ofNullable(assetExtractorTextUnit.getUsages()).orElse(Collections.emptySet())); // TODO clean that up
        branchStateTextUnit.setBranchToBranchDatas(ImmutableMap.of(branch, branchData));

        branchStateTextUnit.setCreatedDate(createdDate); // TODO not needed? ie superflux? because it won't be in sync with DB?
        branchStateTextUnit.setMd5(textUnitUtils.computeTextUnitMD5(
                branchStateTextUnit.getName(),
                branchStateTextUnit.getSource(),
                branchStateTextUnit.getComments()));

        return branchStateTextUnit;
    }
}
