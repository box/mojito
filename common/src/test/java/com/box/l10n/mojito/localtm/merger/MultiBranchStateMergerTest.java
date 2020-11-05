package com.box.l10n.mojito.localtm.merger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.joda.time.DateTime;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class MultiBranchStateMergerTest {

    static Stream<Arguments> merge() {

        Branch branch1 = Branch.builder()
                .name("branch1")
                .createdAt(new DateTime(2020, 7, 10, 0, 0))
                .build();

        Branch branch2 = Branch.builder()
                .name("branch2")
                .createdAt(new DateTime(2020, 7, 10, 0, 0))
                .build();

        Branch branch3 = Branch.builder()
                .name("branch3")
                .createdAt(new DateTime(2020, 6, 10, 0, 0))
                .build();

        BranchStateTextUnit state1BranchStateTextUnit1 = createBranchStateTextUnit("MD5HASH1", createBranchMap(branch1, branch2));

        BranchStateTextUnit state1BranchStateTextUnit2 = createBranchStateTextUnit("MD5HASH2", createBranchMap(branch1));

        MultiBranchState state1 = MultiBranchState.of()
                .withBranches(ImmutableSet.of(branch1, branch2))
                .withBranchStateTextUnits(ImmutableList.of(state1BranchStateTextUnit1, state1BranchStateTextUnit2));

        BranchStateTextUnit state2BranchStateTextUnit2 = createBranchStateTextUnit("MD5HASH2", createBranchMap(branch3));

        MultiBranchState state2 = MultiBranchState.of()
                .withBranches(ImmutableSet.of(branch3))
                .withBranchStateTextUnits(ImmutableList.of(state2BranchStateTextUnit2));

        BranchStateTextUnit merged1BranchStateTextUnit1 = createBranchStateTextUnit("MD5HASH1", createBranchMap(branch1, branch2));

        BranchStateTextUnit merged1BranchStateTextUnit2 = createBranchStateTextUnit("MD5HASH2", createBranchMap(branch3, branch1));

        MultiBranchState merge1 = MultiBranchState.of()
                .withBranches(ImmutableSet.of(branch3, branch1, branch2))
                .withBranchStateTextUnits(ImmutableList.of(
                        merged1BranchStateTextUnit1,
                        merged1BranchStateTextUnit2));

        MultiBranchState merge1Priority = MultiBranchState.of()
                .withBranches(ImmutableSet.of(branch2, branch3, branch1))
                .withBranchStateTextUnits(ImmutableList.of(
                        merged1BranchStateTextUnit1,
                        merged1BranchStateTextUnit2));

        return Stream.of(
                Arguments.of(
                        "Empty states",
                        MultiBranchState.of(),
                        MultiBranchState.of(),
                        ImmutableSet.of(),
                        MultiBranchState.of()
                ),
                Arguments.of(
                        "States with basic data",
                        state1,
                        state2,
                        ImmutableSet.of(),
                        merge1
                ),
                Arguments.of(
                        "States with basic data and priority branch",
                        state1,
                        state2,
                        ImmutableSet.of("branch2"),
                        merge1Priority
                )
        );
    }

    static BranchStateTextUnit createBranchStateTextUnit(String md5HASH1, ImmutableMap<String, BranchData> branchMap) {
        return BranchStateTextUnit.builder().md5(md5HASH1).branchNameToBranchDatas(branchMap).build();
    }

    static ImmutableMap<String, BranchData> createBranchMap(Branch... branches) {
        return Arrays.stream(branches)
                .collect(ImmutableMap.toImmutableMap(
                        Branch::getName,
                        b -> BranchData.of())
                );
    }

    static Stream<Arguments> mergeBranchesByPriorityThenCreatedDateThenName() {
        Branch branch1 = Branch.builder()
                .name("branch1")
                .createdAt(new DateTime(2020, 7, 10, 0, 0))
                .build();

        Branch branch2 = Branch.builder()
                .name("branch2")
                .createdAt(new DateTime(2020, 7, 10, 0, 0))
                .build();

        Branch masterBranch = Branch.builder()
                .name("master")
                .createdAt(new DateTime(2020, 8, 10, 0, 0))
                .build();

        Branch branch3 = Branch.builder()
                .name("branch3")
                .createdAt(new DateTime(2020, 6, 10, 0, 0))
                .build();

        return Stream.of(
                Arguments.of(
                        "Empty lists",
                        ImmutableSet.of(),
                        ImmutableSet.of(),
                        ImmutableSet.of(),
                        ImmutableSet.of()
                ),
                Arguments.of(
                        "Empty list & 1 branch",
                        ImmutableSet.of(),
                        ImmutableSet.of(branch1),
                        ImmutableSet.of(),
                        ImmutableSet.of(branch1)
                ),
                Arguments.of(
                        "1 branch & empty list",
                        ImmutableSet.of(branch1),
                        ImmutableSet.of(),
                        ImmutableSet.of(),
                        ImmutableSet.of(branch1)
                ),
                Arguments.of(
                        "1 branch & 1 branch 1",
                        ImmutableSet.of(branch1),
                        ImmutableSet.of(branch1),
                        ImmutableSet.of(),
                        ImmutableSet.of(branch1)
                ),
                Arguments.of(
                        "Simple date order check",
                        ImmutableSet.of(branch3),
                        ImmutableSet.of(branch1),
                        ImmutableSet.of(),
                        ImmutableSet.of(branch3, branch1)
                ),
                Arguments.of(
                        "Date order and then name",
                        ImmutableSet.of(branch1, masterBranch, branch3),
                        ImmutableSet.of(branch2),
                        ImmutableSet.of(),
                        ImmutableSet.of(branch3, branch1, branch2, masterBranch)
                ),
                Arguments.of(
                        "Priority first, then date and finally name",
                        ImmutableSet.of(branch1, masterBranch, branch3),
                        ImmutableSet.of(branch2),
                        ImmutableSet.of("master"),
                        ImmutableSet.of(masterBranch, branch3, branch1, branch2)
                ),
                Arguments.of(
                        "Priorities first, then date and finally name",
                        ImmutableSet.of(branch1, masterBranch, branch3),
                        ImmutableSet.of(branch2),
                        ImmutableSet.of("master", "branch2"),
                        ImmutableSet.of(masterBranch, branch2, branch3, branch1)
                )
        );
    }

    static Stream<Arguments> sortBranchesByPriorityThenCreatedDateThenName() {
        Branch branch1 = Branch.builder()
                .name("branch1")
                .createdAt(new DateTime(2020, 7, 10, 0, 0))
                .build();

        Branch branch2 = Branch.builder()
                .name("branch2")
                .createdAt(new DateTime(2020, 7, 10, 0, 0))
                .build();

        Branch masterBranch = Branch.builder()
                .name("master")
                .createdAt(new DateTime(2020, 8, 10, 0, 0))
                .build();

        Branch branch3 = Branch.builder()
                .name("branch3")
                .createdAt(new DateTime(2020, 6, 10, 0, 0))
                .build();

        return Stream.of(
                Arguments.of(
                        "Empty lists",
                        ImmutableSet.of(),
                        ImmutableSet.of(),
                        ImmutableSet.of()
                ),
                Arguments.of(
                        "1 branch",
                        ImmutableSet.of(branch1),
                        ImmutableSet.of(),
                        ImmutableSet.of(branch1)
                ),
                Arguments.of(
                        "2 branches same date, different name",
                        ImmutableSet.of(branch1, branch2),
                        ImmutableSet.of(),
                        ImmutableSet.of(branch1, branch2)
                ),
                Arguments.of(
                        "2 branches same date, different name - reverse",
                        ImmutableSet.of(branch2, branch1),
                        ImmutableSet.of(),
                        ImmutableSet.of(branch1, branch2)
                ),
                Arguments.of(
                        "Order based on date",
                        ImmutableSet.of(branch2, branch3, branch1),
                        ImmutableSet.of(),
                        ImmutableSet.of(branch3, branch1, branch2)
                ),
                Arguments.of(
                        "Order based on priority",
                        ImmutableSet.of(branch2, branch3, masterBranch, branch1),
                        ImmutableSet.of("master"),
                        ImmutableSet.of(masterBranch, branch3, branch1, branch2)
                ),
                Arguments.of(
                        "Order based on priorities",
                        ImmutableSet.of(branch2, branch3, masterBranch, branch1),
                        ImmutableSet.of("branch2", "branch1"),
                        ImmutableSet.of(branch2, branch1, branch3, masterBranch)
                )
        );
    }

    static Stream<Arguments> mergerMultiBranchStateIntoBranchStateTextUnit() {
        Branch branch1 = Branch.builder()
                .name("branch1")
                .createdAt(new DateTime(2020, 7, 10, 0, 0))
                .build();

        Branch branch2 = Branch.builder()
                .name("branch2")
                .createdAt(new DateTime(2020, 7, 10, 0, 0))
                .build();

        Branch branch3 = Branch.builder()
                .name("branch3")
                .createdAt(new DateTime(2020, 6, 10, 0, 0))
                .build();

        BranchStateTextUnit baseBranchStateTextUnit = createBranchStateTextUnit("MD5HASH", createBranchMap(branch1, branch2));

        MultiBranchState toMergeState = MultiBranchState.of()
                .withBranches(ImmutableSet.of(branch2, branch3))
                .withBranchStateTextUnits(ImmutableList.of());

        BranchStateTextUnit match = createBranchStateTextUnit("MD5HASH", createBranchMap(branch3));

        MultiBranchState toMergeStateWithMatch = MultiBranchState.of()
                .withBranches(ImmutableSet.of(branch2, branch3))
                .withBranchStateTextUnits(ImmutableList.of(match));

        MultiBranchState toMergeStateWithNewUsage = MultiBranchState.of()
                .withBranches(ImmutableSet.of(branch2, branch3))
                .withBranchStateTextUnits(ImmutableList.of(
                        createBranchStateTextUnit("MD5HASH", ImmutableMap.of(
                                "branch2",
                                BranchData.of().withUsages(ImmutableSet.of("new usage"))))));

        return Stream.of(
                Arguments.of(
                        "No match by md5 in merge state, remove the merge state branches from baseTextUnit",
                        baseBranchStateTextUnit,
                        toMergeState,
                        createBranchStateTextUnit("MD5HASH", createBranchMap(branch1))
                ),
                Arguments.of(
                        "Match by md5 in merge state, add the merge state branches to baseTextUnit",
                        baseBranchStateTextUnit,
                        toMergeStateWithMatch,
                        createBranchStateTextUnit("MD5HASH", createBranchMap(branch1, branch2, branch3))
                ),
                Arguments.of(
                        "Match by md5 in merge state, new usage",
                        baseBranchStateTextUnit,
                        toMergeStateWithNewUsage,
                        createBranchStateTextUnit("MD5HASH", ImmutableMap.of(
                                "branch1",
                                BranchData.of(),
                                "branch2",
                                BranchData.of().withUsages(ImmutableSet.of("new usage")))))
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

    static Stream<Arguments> sortBranchDataByBranch() {
        Branch branch1 = Branch.builder()
                .name("branch1")
                .createdAt(new DateTime(2020, 7, 10, 0, 0))
                .build();

        Branch branch2 = Branch.builder()
                .name("branch2")
                .createdAt(new DateTime(2020, 7, 10, 0, 0))
                .build();

        Branch masterBranch = Branch.builder()
                .name("master")
                .createdAt(new DateTime(2020, 8, 10, 0, 0))
                .build();

        Branch branch3 = Branch.builder()
                .name("branch3")
                .createdAt(new DateTime(2020, 6, 10, 0, 0))
                .build();

        return Stream.of(
                Arguments.of(
                        "Empty lists",
                        ImmutableMap.of(),
                        ImmutableSet.of(),
                        ImmutableMap.of()
                ),
                Arguments.of(
                        "1 branch",
                        createBranchMap(branch1),
                        ImmutableSet.of(),
                        createBranchMap(branch1)
                ),
                Arguments.of(
                        "2 branches - no order, don't change",
                        createBranchMap(branch1, branch2),
                        ImmutableSet.of(),
                        createBranchMap(branch1, branch2)
                ),
                Arguments.of(
                        "2 branches - same order, don't change",
                        createBranchMap(branch1, branch2),
                        ImmutableSet.of(branch1, branch2),
                        createBranchMap(branch1, branch2)
                ),
                Arguments.of(
                        "2 branches - different order",
                        createBranchMap(branch2, branch1),
                        ImmutableSet.of(branch1, branch2),
                        createBranchMap(branch1, branch2)
                ),
                Arguments.of(
                        "3 branches - no order",
                        createBranchMap(branch2, branch3, branch1),
                        ImmutableSet.of(),
                        createBranchMap(branch2, branch3, branch1)
                ),
                Arguments.of(
                        "3 branches - partial order",
                        createBranchMap(branch2, branch3, branch1),
                        ImmutableSet.of(branch2, branch1),
                        createBranchMap(branch2, branch1, branch3)
                ),
                Arguments.of(
                        "3 branches - full order",
                        createBranchMap(branch2, branch3, branch1),
                        ImmutableSet.of(branch3, branch2, branch1),
                        createBranchMap(branch3, branch2, branch1)
                ),
                Arguments.of(
                        "4 branches - full order",
                        createBranchMap(branch2, branch3, masterBranch, branch1),
                        ImmutableSet.of(masterBranch, branch3, branch2, branch1),
                        createBranchMap(masterBranch, branch3, branch2, branch1)
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    public void merge(String message, MultiBranchState intoState, MultiBranchState toMerge, ImmutableSet<String> priorityBranchNames, MultiBranchState expected) {
        MultiBranchStateMerger multiBranchStateMerger = new MultiBranchStateMerger();
        MultiBranchState result = multiBranchStateMerger.merge(toMerge, intoState, priorityBranchNames);
        assertThat(result).as(message).usingRecursiveComparison().isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource
    public void mergeBranchesByPriorityThenCreatedDateThenName(
            String message,
            ImmutableSet<Branch> branch,
            ImmutableSet<Branch> branch2,
            ImmutableSet<String> priorities,
            ImmutableSet<Branch> expectedBranch) {

        MultiBranchStateMerger multiBranchStateMerger = new MultiBranchStateMerger();

        ImmutableSet<Branch> branches = multiBranchStateMerger.mergeBranchesByPriorityThenCreatedDateThenName(
                branch,
                branch2,
                priorities
        );

        assertThat(branches.asList()).as(message).usingRecursiveComparison().isEqualTo(expectedBranch.asList());
    }

    @ParameterizedTest
    @MethodSource
    public void sortBranchesByPriorityThenCreatedDateThenName(
            String message,
            ImmutableSet<Branch> branches,
            ImmutableSet<String> sortedBranchNames,
            ImmutableSet<Branch> expectedBranch) {

        MultiBranchStateMerger multiBranchStateMerger = new MultiBranchStateMerger();

        ImmutableSet<Branch> result = multiBranchStateMerger.sortBranchesByPriorityThenCreatedDateThenName(
                branches,
                sortedBranchNames
        );
        assertThat(result.asList()).as(message).isEqualTo(expectedBranch.asList());
    }

    @ParameterizedTest
    @MethodSource
    public void sortBranchDataByBranch(
            String message,
            ImmutableMap<String, BranchData> branchData,
            ImmutableSet<Branch> sortedBranches,
            ImmutableMap<String, BranchData> expected) {
        MultiBranchStateMerger multiBranchStateMerger = new MultiBranchStateMerger();
        ImmutableMap<String, BranchData> result = multiBranchStateMerger.sortBranchDataByBranch(branchData, sortedBranches);
        assertThat(result.keySet().asList()).as(message).usingRecursiveComparison().isEqualTo(expected.keySet().asList());
    }

    @ParameterizedTest
    @MethodSource
    public void mergerMultiBranchStateIntoBranchStateTextUnit(
            String message,
            BranchStateTextUnit baseBranchStateTextUnit,
            MultiBranchState toMergeState,
            BranchStateTextUnit expected) {

        MultiBranchStateMerger multiBranchStateMerger = new MultiBranchStateMerger();
        BranchStateTextUnit result = multiBranchStateMerger.mergerMultiBranchStateIntoBranchStateTextUnit(
                toMergeState, ImmutableSet.of()).apply(baseBranchStateTextUnit);
        assertEquals(message, expected, result);
    }
}