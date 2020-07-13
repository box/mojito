package com.box.l10n.mojito.ltm.merger;

import com.box.l10n.mojito.json.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class MultiBranchStateMergerTest {

    static Stream<Arguments> merge() {
        Branch branch1 = new Branch();
        branch1.setName("branch1");
        branch1.setCreatedAt(new Date(2020, 7, 10));

        Branch branch2 = new Branch();
        branch2.setName("branch2");
        branch2.setCreatedAt(new Date(2020, 7, 10));

        Branch branch3 = new Branch();
        branch3.setName("branch3");
        branch3.setCreatedAt(new Date(2020, 6, 10));

        BranchStateTextUnit state1BranchStateTextUnit1 = new BranchStateTextUnit();
        state1BranchStateTextUnit1.setMd5("MD5HASH1");
        state1BranchStateTextUnit1.setBranchToBranchDatas(createBranchMap(branch1, branch2));

        BranchStateTextUnit state1BranchStateTextUnit2 = new BranchStateTextUnit();
        state1BranchStateTextUnit2.setMd5("MD5HASH2");
        state1BranchStateTextUnit2.setBranchToBranchDatas(createBranchMap(branch1));

        MultiBranchState state1 = new MultiBranchState();
        state1.setBranches(ImmutableSet.of(branch1, branch2));
        state1.setMd5ToBranchStateTextUnits(ImmutableMap.of(
                state1BranchStateTextUnit1.getMd5(), state1BranchStateTextUnit1,
                state1BranchStateTextUnit2.getMd5(), state1BranchStateTextUnit2));

        BranchStateTextUnit state2BranchStateTextUnit2 = new BranchStateTextUnit();
        state2BranchStateTextUnit2.setMd5("MD5HASH2");
        state2BranchStateTextUnit2.setBranchToBranchDatas(createBranchMap(branch3));

        MultiBranchState state2 = new MultiBranchState();
        state2.setBranches(ImmutableSet.of(branch3));
        state2.setMd5ToBranchStateTextUnits(ImmutableMap.of(state2BranchStateTextUnit2.getMd5(), state2BranchStateTextUnit2));

        BranchStateTextUnit merged1BranchStateTextUnit1 = new BranchStateTextUnit();
        merged1BranchStateTextUnit1.setMd5("MD5HASH1");
        merged1BranchStateTextUnit1.setBranchToBranchDatas(createBranchMap(branch1, branch2));

        BranchStateTextUnit merged1BranchStateTextUnit2 = new BranchStateTextUnit();
        merged1BranchStateTextUnit2.setMd5("MD5HASH2");
        merged1BranchStateTextUnit2.setBranchToBranchDatas(createBranchMap(branch3, branch1));

        MultiBranchState merge1 = new MultiBranchState();
        merge1.setBranches(ImmutableSet.of(branch3, branch1, branch2));
        merge1.setMd5ToBranchStateTextUnits(ImmutableMap.of(
                merged1BranchStateTextUnit1.getMd5(), merged1BranchStateTextUnit1,
                merged1BranchStateTextUnit2.getMd5(), merged1BranchStateTextUnit2));

        MultiBranchState merge1Priority = new MultiBranchState();
        merge1Priority.setBranches(ImmutableSet.of(branch2, branch3, branch1));
        merge1Priority.setMd5ToBranchStateTextUnits(ImmutableMap.of(
                merged1BranchStateTextUnit1.getMd5(), merged1BranchStateTextUnit1,
                merged1BranchStateTextUnit2.getMd5(), merged1BranchStateTextUnit2));

        return Stream.of(
                Arguments.of(
                        "Empty states",
                        new MultiBranchState(),
                        new MultiBranchState(),
                        ImmutableList.of(),
                        new MultiBranchState()
                ),
                Arguments.of(
                        "States with basic data",
                        state1,
                        state2,
                        ImmutableList.of(),
                        merge1
                ),
                Arguments.of(
                        "States with basic data and priority branch",
                        state1,
                        state2,
                        ImmutableList.of("branch2"),
                        merge1Priority
                )
        );
    }

    static ImmutableMap<Branch, BranchData> createBranchMap(Branch... branches) {
        return Arrays.stream(branches)
                .collect(ImmutableMap.toImmutableMap(Function.identity(), b -> new BranchData())
                );
    }

    static Stream<Arguments> mergeBranchesByPriorityThenCreatedDateThenName() {
        Branch branch1 = new Branch();
        branch1.setName("branch1");
        branch1.setCreatedAt(new Date(2020, 7, 10));

        Branch branch2 = new Branch();
        branch2.setName("branch2");
        branch2.setCreatedAt(new Date(2020, 7, 10));

        Branch masterBranch = new Branch();
        masterBranch.setName("master");
        masterBranch.setCreatedAt(new Date(2020, 8, 10));

        Branch branch3 = new Branch();
        branch3.setName("branch3");
        branch3.setCreatedAt(new Date(2020, 6, 10));

        return Stream.of(
                Arguments.of(
                        "Empty lists",
                        ImmutableSet.of(),
                        ImmutableSet.of(),
                        ImmutableList.of(),
                        ImmutableList.of()
                ),
                Arguments.of(
                        "Empty list & 1 branch",
                        ImmutableSet.of(),
                        ImmutableSet.of(branch1),
                        ImmutableList.of(),
                        ImmutableList.of("branch1")
                ),
                Arguments.of(
                        "1 branch & empty list",
                        ImmutableSet.of(branch1),
                        ImmutableSet.of(),
                        ImmutableList.of(),
                        ImmutableList.of("branch1")
                ),
                Arguments.of(
                        "1 branch & 1 branch 1",
                        ImmutableSet.of(branch1),
                        ImmutableSet.of(branch1),
                        ImmutableList.of(),
                        ImmutableList.of("branch1")
                ),
                Arguments.of(
                        "Simple date order check",
                        ImmutableSet.of(branch3),
                        ImmutableSet.of(branch1),
                        ImmutableList.of(),
                        ImmutableList.of("branch3", "branch1")
                ),
                Arguments.of(
                        "Date order and then name",
                        ImmutableSet.of(branch1, masterBranch, branch3),
                        ImmutableSet.of(branch2),
                        ImmutableList.of(),
                        ImmutableList.of("branch3", "branch1", "branch2", "master")
                ),
                Arguments.of(
                        "Priority first, then date and finally name",
                        ImmutableSet.of(branch1, masterBranch, branch3),
                        ImmutableSet.of(branch2),
                        ImmutableList.of("master"),
                        ImmutableList.of("master", "branch3", "branch1", "branch2")
                ),
                Arguments.of(
                        "Priorities first, then date and finally name",
                        ImmutableSet.of(branch1, masterBranch, branch3),
                        ImmutableSet.of(branch2),
                        ImmutableList.of("master", "branch2"),
                        ImmutableList.of("master", "branch2", "branch3", "branch1")
                )
        );
    }

    static Stream<Arguments> sortBranchesByPriorityThenCreatedDateThenName() {
        Branch branch1 = new Branch();
        branch1.setName("branch1");
        branch1.setCreatedAt(new Date(2020, 7, 10));

        Branch branch2 = new Branch();
        branch2.setName("branch2");
        branch2.setCreatedAt(new Date(2020, 7, 10));

        Branch masterBranch = new Branch();
        masterBranch.setName("master");
        masterBranch.setCreatedAt(new Date(2020, 8, 10));

        Branch branch3 = new Branch();
        branch3.setName("branch3");
        branch3.setCreatedAt(new Date(2020, 6, 10));

        return Stream.of(
                Arguments.of(
                        "Empty lists",
                        ImmutableSet.of(),
                        ImmutableList.of(),
                        ImmutableList.of()
                ),
                Arguments.of(
                        "1 branch",
                        ImmutableSet.of(branch1),
                        ImmutableList.of(),
                        ImmutableList.of("branch1")
                ),
                Arguments.of(
                        "2 branches same date, different name",
                        ImmutableSet.of(branch1, branch2),
                        ImmutableList.of(),
                        ImmutableList.of("branch1", "branch2")
                ),
                Arguments.of(
                        "2 branches same date, different name - reverse",
                        ImmutableSet.of(branch2, branch1),
                        ImmutableList.of(),
                        ImmutableList.of("branch1", "branch2")
                ),
                Arguments.of(
                        "Order based on date",
                        ImmutableSet.of(branch2, branch3, branch1),
                        ImmutableList.of(),
                        ImmutableList.of("branch3", "branch1", "branch2")
                ),
                Arguments.of(
                        "Order based on priority",
                        ImmutableSet.of(branch2, branch3, masterBranch, branch1),
                        ImmutableList.of("master"),
                        ImmutableList.of("master", "branch3", "branch1", "branch2")
                ),
                Arguments.of(
                        "Order based on priorities",
                        ImmutableSet.of(branch2, branch3, masterBranch, branch1),
                        ImmutableList.of("branch2", "branch1"),
                        ImmutableList.of("branch2", "branch1", "branch3", "master")
                )
        );
    }

    static Stream<Arguments> getNewBranchesForBaseTextUnit() {
        Branch branch1 = new Branch();
        branch1.setName("branch1");
        branch1.setCreatedAt(new Date(2020, 7, 10));

        Branch branch2 = new Branch();
        branch2.setName("branch2");
        branch2.setCreatedAt(new Date(2020, 7, 10));

        Branch branch3 = new Branch();
        branch3.setName("branch3");
        branch3.setCreatedAt(new Date(2020, 6, 10));

        BranchStateTextUnit baseBranchStateTextUnit = new BranchStateTextUnit();
        baseBranchStateTextUnit.setMd5("MD5HASH");
        baseBranchStateTextUnit.setBranchToBranchDatas(createBranchMap(branch1, branch2));

        MultiBranchState toMergeState = new MultiBranchState();
        toMergeState.setBranches(ImmutableSet.of(branch2, branch3));


        BranchStateTextUnit match = new BranchStateTextUnit();
        match.setMd5("MD5HASH");
        match.setBranchToBranchDatas(createBranchMap(branch3));

        MultiBranchState toMergeStateWithMatch = new MultiBranchState();
        toMergeStateWithMatch.setBranches(ImmutableSet.of(branch2, branch3));
        toMergeStateWithMatch.setMd5ToBranchStateTextUnits(ImmutableMap.of("MD5HASH", match));

        return Stream.of(
                Arguments.of(
                        "No match by md5 in merge state, remove the merge state branches from baseTextUnit",
                        baseBranchStateTextUnit,
                        toMergeState,
                        ImmutableList.of("branch1")
                ),
                Arguments.of(
                        "Match by md5 in merge state, add the merge state branches to baseTextUnit",
                        baseBranchStateTextUnit,
                        toMergeStateWithMatch,
                        ImmutableList.of("branch1", "branch2", "branch3")
                )
        );
    }

    /**
     * to check the order for hashsets.
     *
     * @param c
     * @param <T>
     * @return
     */
    static <T> List<T> toList(Collection<? extends T> c) {
        return c.stream().collect(Collectors.toList());
    }

    static Stream<Arguments> sortBranchesAndData() {
        Branch branch1 = new Branch();
        branch1.setName("branch1");
        branch1.setCreatedAt(new Date(2020, 7, 10));

        Branch branch2 = new Branch();
        branch2.setName("branch2");
        branch2.setCreatedAt(new Date(2020, 7, 10));

        Branch masterBranch = new Branch();
        masterBranch.setName("master");
        masterBranch.setCreatedAt(new Date(2020, 8, 10));

        Branch branch3 = new Branch();
        branch3.setName("branch3");
        branch3.setCreatedAt(new Date(2020, 6, 10));

        return Stream.of(
                Arguments.of(
                        "Empty lists",
                        ImmutableMap.of(),
                        ImmutableList.of(),
                        ImmutableList.of()
                ),
                Arguments.of(
                        "1 branch",
                        createBranchMap(branch1),
                        ImmutableList.of(),
                        ImmutableList.of("branch1")
                ),
                Arguments.of(
                        "2 branches same date, different name",
                        createBranchMap(branch1, branch2),
                        ImmutableList.of(),
                        ImmutableList.of("branch1", "branch2")
                ),
                Arguments.of(
                        "2 branches same date, different name - reverse",
                        createBranchMap(branch2, branch1),
                        ImmutableList.of(),
                        ImmutableList.of("branch1", "branch2")
                ),
                Arguments.of(
                        "Order based on date",
                        createBranchMap(branch2, branch3, branch1),
                        ImmutableList.of(),
                        ImmutableList.of("branch3", "branch1", "branch2")
                ),
                Arguments.of(
                        "Order based on priority",
                        createBranchMap(branch2, branch3, masterBranch, branch1),
                        ImmutableList.of("master"),
                        ImmutableList.of("master", "branch3", "branch1", "branch2")
                ),
                Arguments.of(
                        "Order based on priorities",
                        createBranchMap(branch2, branch3, masterBranch, branch1),
                        ImmutableList.of("branch2", "branch1"),
                        ImmutableList.of("branch2", "branch1", "branch3", "master")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    public void merge(String message, MultiBranchState multiBranchState1, MultiBranchState multiBranchState2, ImmutableList<String> priorityBranchNames, MultiBranchState expected) {
        MultiBranchStateMerger multiBranchStateMerger = new MultiBranchStateMerger();
        MultiBranchState result = multiBranchStateMerger.merge(multiBranchState1, multiBranchState2, priorityBranchNames);

        System.out.println(new ObjectMapper().writeValueAsStringUnchecked(result));
        System.out.println("----");
        System.out.println(new ObjectMapper().writeValueAsStringUnchecked(expected));

        expected.getMd5ToBranchStateTextUnits().values().stream().map(e -> e.getBranchToBranchDatas()).map(branchBranchDataImmutableMap -> branchBranchDataImmutableMap.keySet()).flatMap(Set::stream).map(Branch::getName).forEach(System.out::println);
        System.out.println("----");
        result.getMd5ToBranchStateTextUnits().values().stream().map(e -> e.getBranchToBranchDatas()).map(branchBranchDataImmutableMap -> branchBranchDataImmutableMap.keySet()).flatMap(Set::stream).map(Branch::getName).forEach(System.out::println);


        assertEquals(message, toList(expected.getBranches()), toList(result.getBranches()));
        assertEquals(message, expected.getMd5ToBranchStateTextUnits().keySet().toArray(), result.getMd5ToBranchStateTextUnits().keySet().toArray());
        assertEquals(message, expected.getMd5ToBranchStateTextUnits().values().toArray(), result.getMd5ToBranchStateTextUnits().values().toArray());
    }

    @ParameterizedTest
    @MethodSource
    public void mergeBranchesByPriorityThenCreatedDateThenName(
            String message,
            ImmutableSet<Branch> branch,
            ImmutableSet<Branch> branch2,
            ImmutableList<String> priorities,
            ImmutableList<String> expectedBranchNames) {

        MultiBranchStateMerger multiBranchStateMerger = new MultiBranchStateMerger();

        ImmutableSet<Branch> branches = multiBranchStateMerger.mergeBranchesByPriorityThenCreatedDateThenName(
                branch,
                branch2,
                priorities
        );

        List<String> branchNames = branches.stream().map(Branch::getName).collect(Collectors.toList());

        assertEquals(message, expectedBranchNames, branchNames);
    }

    @ParameterizedTest
    @MethodSource
    public void sortBranchesByPriorityThenCreatedDateThenName(
            String message,
            ImmutableSet<Branch> branches,
            ImmutableList<String> priorities,
            ImmutableList<String> expectedBranchNames) {

        MultiBranchStateMerger multiBranchStateMerger = new MultiBranchStateMerger();

        ImmutableSet<Branch> result = multiBranchStateMerger.sortBranchesByPriorityThenCreatedDateThenName(
                branches,
                priorities
        );

        ImmutableList<String> resultNames = result.stream().map(Branch::getName).collect(ImmutableList.toImmutableList());

        assertEquals(message, expectedBranchNames, resultNames);
    }

    @ParameterizedTest
    @MethodSource
    public void sortBranchesAndData(
            String message,
            ImmutableMap<Branch, BranchData> branches,
            ImmutableList<String> priorities,
            ImmutableList<String> expectedBranchNames) {

        MultiBranchStateMerger multiBranchStateMerger = new MultiBranchStateMerger();

        ImmutableMap<Branch, BranchData> result = multiBranchStateMerger.sortBranchesAndData(branches, priorities);

        ImmutableList<String> resultNames = result.keySet().stream().map(Branch::getName).collect(ImmutableList.toImmutableList());

        assertEquals(message, expectedBranchNames, resultNames);
    }

    @ParameterizedTest
    @MethodSource
    public void getNewBranchesForBaseTextUnit(String message, BranchStateTextUnit baseBranchStateTextUnit, MultiBranchState toMergeState, ImmutableList<String> expected) {

        MultiBranchStateMerger multiBranchStateMerger = new MultiBranchStateMerger();
        ImmutableMap<Branch, BranchData> newBranchesForBaseTextUnit = multiBranchStateMerger.getNewBranchesAndDataForBaseTextUnit(baseBranchStateTextUnit, toMergeState);
        List<String> branchNames = newBranchesForBaseTextUnit.keySet().stream().map(Branch::getName).collect(Collectors.toList());
        assertEquals(message, expected, branchNames);
    }
}