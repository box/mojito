package com.box.l10n.mojito.ltm.merger;

import com.box.l10n.mojito.okapi.TextUnitUtils;
import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

public class AssetExtractorTextUnitsToMultiBranchStateTest {

    @Test
    public void convert() {

        List<AssetExtractorTextUnit> assetExtractorTextUnits = IntStream.range(0, 2).mapToObj(i -> {
            AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
            assetExtractorTextUnit.setName("name-" + i);
            assetExtractorTextUnit.setSource("source-" + i);
            assetExtractorTextUnit.setComments("comment-" + i);
            return assetExtractorTextUnit;
        }).collect(Collectors.toList());

        Branch branch1 = new Branch();
        branch1.setName("branch1");
        branch1.setCreatedAt(new Date(2020, 7, 14));

        AssetExtractorTextUnitsToMultiBranchState assetExtractorTextUnitsToMultiBranchState = new AssetExtractorTextUnitsToMultiBranchState(new TextUnitUtils());
        MultiBranchState multiBranchState = assetExtractorTextUnitsToMultiBranchState.convert(assetExtractorTextUnits, branch1);

        List<String> branchNames = multiBranchState.getBranches().stream().map(Branch::getName).collect(Collectors.toList());
        assertEquals(Arrays.asList("branch1"), branchNames);
        assertEquals(Arrays.asList("name-0", "name-1"), multiBranchState.getMd5ToBranchStateTextUnits().values().stream().map(BranchStateTextUnit::getName).collect(Collectors.toList()));
        assertEquals(Arrays.asList(branch1), multiBranchState.getMd5ToBranchStateTextUnits().values().stream().map(BranchStateTextUnit::getBranchToBranchDatas).map(Map::keySet).flatMap(Set::stream).distinct().collect(Collectors.toList()));
    }

    @Test
    public void convertToBranchStateTextUnit() {

        AssetExtractorTextUnitsToMultiBranchState assetExtractorTextUnitsToMultiBranchState = new AssetExtractorTextUnitsToMultiBranchState(new TextUnitUtils());

        AssetExtractorTextUnit assetExtractorTextUnit = new AssetExtractorTextUnit();
        assetExtractorTextUnit.setName("name_one");
        assetExtractorTextUnit.setSource("source");
        assetExtractorTextUnit.setComments("comment");
        assetExtractorTextUnit.setPluralForm("one");
        assetExtractorTextUnit.setPluralFormOther("name_other");
        assetExtractorTextUnit.setUsages(Sets.newHashSet("location1", "location2"));

        Date createdDate = new Date(2020, 7, 15);

        Branch branch = new Branch();
        branch.setName("branch1");
        branch.setCreatedAt(new Date(2020, 7, 14));

        BranchStateTextUnit branchStateTextUnit = assetExtractorTextUnitsToMultiBranchState.convertToBranchStateTextUnit(assetExtractorTextUnit, branch, createdDate);

        assertEquals("name_one", branchStateTextUnit.getName());
        assertEquals("source", branchStateTextUnit.getSource());
        assertEquals("comment", branchStateTextUnit.getComments());
        assertEquals("one", branchStateTextUnit.getPluralForm());
        assertEquals("name_other", branchStateTextUnit.getPluralFormOther());
        assertEquals("fdd3068a96e82cf878da4df88505faa8", branchStateTextUnit.getMd5());
        assertEquals(createdDate, branchStateTextUnit.getCreatedDate());
        assertEquals(ImmutableSet.of(branch), branchStateTextUnit.getBranchToBranchDatas().keySet());
        assertEquals(Arrays.asList("location1", "location2"), branchStateTextUnit.getBranchToBranchDatas().get(branch).getUsages());
    }

}