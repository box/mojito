package com.box.l10n.mojito.localtm.merger;

import com.box.l10n.mojito.okapi.TextUnitUtils;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** */
@Component
public class AssetExtractorTextUnitsToMultiBranchStateConverter {

  TextUnitUtils textUnitUtils;

  @Autowired
  public AssetExtractorTextUnitsToMultiBranchStateConverter(TextUnitUtils textUnitUtils) {
    this.textUnitUtils = Preconditions.checkNotNull(textUnitUtils);
  }

  public MultiBranchState convert(
      List<AssetExtractorTextUnit> assetExtractorTextUnits, Branch branch) {

    DateTime now = DateTime.now();

    ImmutableList<BranchStateTextUnit> branchStateTextUnitImmutableMap =
        assetExtractorTextUnits.stream()
            .map(
                assetExtractorTextUnit ->
                    convertToBranchStateTextUnit(assetExtractorTextUnit, branch, now))
            .collect(ImmutableList.toImmutableList());

    return MultiBranchState.of()
        .withBranches(ImmutableSet.of(branch))
        .withBranchStateTextUnits(branchStateTextUnitImmutableMap);
  }

  BranchStateTextUnit convertToBranchStateTextUnit(
      AssetExtractorTextUnit assetExtractorTextUnit, Branch branch, DateTime createdDate) {

    BranchData branchData =
        BranchData.of()
            .withUsages(
                assetExtractorTextUnit.getUsages() == null
                    ? ImmutableSet.of()
                    : ImmutableSet.copyOf(assetExtractorTextUnit.getUsages()));

    BranchStateTextUnit branchStateTextUnit =
        BranchStateTextUnit.builder()
            .source(assetExtractorTextUnit.getSource())
            .name(assetExtractorTextUnit.getName())
            .comments(assetExtractorTextUnit.getComments())
            .pluralForm(assetExtractorTextUnit.getPluralForm())
            .pluralFormOther(assetExtractorTextUnit.getPluralFormOther())
            .branchNameToBranchDatas(ImmutableMap.of(branch.getName(), branchData))
            .createdDate(createdDate)
            .md5(
                textUnitUtils.computeTextUnitMD5(
                    assetExtractorTextUnit.getName(),
                    assetExtractorTextUnit.getSource(),
                    assetExtractorTextUnit.getComments()))
            .build();

    return branchStateTextUnit;
  }
}
