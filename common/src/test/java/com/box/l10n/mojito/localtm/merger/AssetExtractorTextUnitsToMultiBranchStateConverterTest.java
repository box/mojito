package com.box.l10n.mojito.localtm.merger;

import static org.assertj.core.api.Assertions.assertThat;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.okapi.TextUnitUtils;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

public class AssetExtractorTextUnitsToMultiBranchStateConverterTest {

  @Test
  public void convert() {

    List<AssetExtractorTextUnit> assetExtractorTextUnits =
        IntStream.range(0, 2)
            .mapToObj(
                i -> {
                  AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
                  assetExtractorTextUnit.setName("name-" + i);
                  assetExtractorTextUnit.setSource("source-" + i);
                  assetExtractorTextUnit.setComments("comment-" + i);
                  return assetExtractorTextUnit;
                })
            .collect(Collectors.toList());

    Branch branch1 =
        Branch.builder()
            .name("branch1")
            .createdAt(JSR310Migration.newDateTimeCtor(2020, 7, 14, 0, 0))
            .build();

    AssetExtractorTextUnitsToMultiBranchStateConverter
        assetExtractorTextUnitsToMultiBranchStateConverter =
            new AssetExtractorTextUnitsToMultiBranchStateConverter(new TextUnitUtils());
    MultiBranchState multiBranchState =
        assetExtractorTextUnitsToMultiBranchStateConverter.convert(
            assetExtractorTextUnits, branch1);

    assertThat(multiBranchState.getBranches())
        .extracting(Branch::getName)
        .containsExactly("branch1");

    assertThat(multiBranchState.getBranchStateTextUnits())
        .extracting(BranchStateTextUnit::getName)
        .containsExactly("name-0", "name-1");

    assertThat(multiBranchState.getBranchStateTextUnits())
        .extracting(BranchStateTextUnit::getBranchNameToBranchDatas)
        .flatExtracting(Map::keySet)
        .containsOnly(branch1.getName());
  }

  @Test
  public void convertToBranchStateTextUnit() {

    AssetExtractorTextUnitsToMultiBranchStateConverter
        assetExtractorTextUnitsToMultiBranchStateConverter =
            new AssetExtractorTextUnitsToMultiBranchStateConverter(new TextUnitUtils());

    AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
    assetExtractorTextUnit.setName("name_one");
    assetExtractorTextUnit.setSource("source");
    assetExtractorTextUnit.setComments("comment");
    assetExtractorTextUnit.setPluralForm("one");
    assetExtractorTextUnit.setPluralFormOther("name_other");
    assetExtractorTextUnit.setUsages(Sets.newHashSet("location1", "location2"));

    ZonedDateTime createdDate = JSR310Migration.newDateTimeCtor(2020, 7, 15, 0, 0);

    Branch branch =
        Branch.builder()
            .name("branch1")
            .createdAt(JSR310Migration.newDateTimeCtor(2020, 7, 14, 0, 0))
            .build();

    BranchStateTextUnit branchStateTextUnit =
        assetExtractorTextUnitsToMultiBranchStateConverter.convertToBranchStateTextUnit(
            assetExtractorTextUnit, branch, createdDate);

    assertThat(branchStateTextUnit)
        .usingRecursiveComparison()
        .isEqualTo(
            BranchStateTextUnit.builder()
                .name("name_one")
                .source("source")
                .comments("comment")
                .pluralForm("one")
                .pluralFormOther("name_other")
                .md5("fdd3068a96e82cf878da4df88505faa8")
                .createdDate(createdDate)
                .branchNameToBranchDatas(
                    ImmutableMap.of(
                        branch.getName(),
                        BranchData.of().withUsages(ImmutableSet.of("location1", "location2"))))
                .build());
  }
}
