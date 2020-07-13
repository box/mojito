package com.box.l10n.mojito.ltm.merger;

import com.box.l10n.mojito.json.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

import java.util.Date;

public class MultiBranchStateDiffBuilderTest {

    @Test
    public void mergeCurrentIntoBaseAndCreateTextUnits() {
        MultiBranchStateDiffBuilder multiBranchStateDiffBuilder = new MultiBranchStateDiffBuilder();
        MultiBranchState base = new MultiBranchState();
        MultiBranchState current = new MultiBranchState();
        Branch branch = new Branch();
        branch.setName("master");
        branch.setCreatedAt(new Date(2020, 7, 14));
        current.setBranches(ImmutableSet.of(branch));
        BranchStateTextUnit branchStateTextUnit = new BranchStateTextUnit();
        branchStateTextUnit.setName("name-1");
        branchStateTextUnit.setSource("source-1");
        branchStateTextUnit.setComments("comment-1");
        branchStateTextUnit.setMd5("MD5-1");
        branchStateTextUnit.setBranchToBranchDatas(ImmutableMap.of(branch, new BranchData()));
        current.setMd5ToBranchStateTextUnits(ImmutableMap.of(branchStateTextUnit.getMd5(), branchStateTextUnit));

        System.out.println("-------- building merged");
        MultiBranchState merged = multiBranchStateDiffBuilder.mergeCurrentIntoBaseAndCreateTextUnits(base, current, ImmutableList.of());

        ObjectMapper objectMapper = ObjectMapper.withIndentedOutput();
        System.out.println(objectMapper.writeValueAsStringUnchecked(merged));

        BranchData branchData = new BranchData();
        branchData.setUsages(ImmutableList.of("file.po:15"));
        branchStateTextUnit.setBranchToBranchDatas(ImmutableMap.of(branch, branchData));


        MultiBranchState current2 = new MultiBranchState();
        Branch branch2 = new Branch();
        branch2.setName("master");
        branch2.setCreatedAt(new Date(2020, 7, 14));
        current2.setBranches(ImmutableSet.of(branch2));
        BranchStateTextUnit branchStateTextUnit2 = new BranchStateTextUnit();
        branchStateTextUnit2.setName("name-1");
        branchStateTextUnit2.setSource("source-1");
        branchStateTextUnit2.setComments("comment-1");
        branchStateTextUnit2.setMd5("MD5-1");
        BranchData branchData2 = new BranchData();
        branchData2.setUsages(ImmutableList.of("file.po:15"));
        branchStateTextUnit2.setBranchToBranchDatas(ImmutableMap.of(branch2, branchData2));
        current2.setMd5ToBranchStateTextUnits(ImmutableMap.of(branchStateTextUnit2.getMd5(), branchStateTextUnit2));


        System.out.println("-------- building merged2");
        System.out.println("merged: " + objectMapper.writeValueAsStringUnchecked(merged));
        System.out.println("current2: " + objectMapper.writeValueAsStringUnchecked(current2));

        MultiBranchState merged2 = multiBranchStateDiffBuilder.mergeCurrentIntoBaseAndCreateTextUnits(merged, current2, ImmutableList.of());
        System.out.println(objectMapper.writeValueAsStringUnchecked(merged2));


        MultiBranchState current3 = new MultiBranchState();
        Branch branch3 = new Branch();
        branch3.setName("branch1");
        branch3.setCreatedAt(new Date(2020, 7, 14));
        current3.setBranches(ImmutableSet.of(branch3));
        BranchStateTextUnit branchStateTextUnit3 = new BranchStateTextUnit();
        branchStateTextUnit3.setName("name-1");
        branchStateTextUnit3.setSource("source-1b");
        branchStateTextUnit3.setComments("comment-1");
        branchStateTextUnit3.setMd5("MD5-1b");
        BranchData branchData3 = new BranchData();
        branchData3.setUsages(ImmutableList.of("file.po:25"));
        branchStateTextUnit3.setBranchToBranchDatas(ImmutableMap.of(branch3, branchData3));
        current3.setMd5ToBranchStateTextUnits(ImmutableMap.of(branchStateTextUnit3.getMd5(), branchStateTextUnit3));


        System.out.println("-------- building merged3");
        System.out.println("merged2: " + objectMapper.writeValueAsStringUnchecked(merged2));
        System.out.println("current3: " + objectMapper.writeValueAsStringUnchecked(current3));


        MultiBranchState merged3 = multiBranchStateDiffBuilder.mergeCurrentIntoBaseAndCreateTextUnits(merged2, current3, ImmutableList.of("branch1"));
        System.out.println(objectMapper.writeValueAsStringUnchecked(merged3));


    }

}