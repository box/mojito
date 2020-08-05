package com.box.l10n.mojito.ltm.merger;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MultiBranchStateMerger {

    /**
     * This merges the "toMergeState" state into the receiving state "intoState".
     *
     * @param intoState
     * @param toMergeState
     * @param priorityBranchNames
     * @return
     */
   public MultiBranchState merge(MultiBranchState intoState, MultiBranchState toMergeState, ImmutableList<String> priorityBranchNames) {
        MultiBranchState newState = new MultiBranchState();

        newState.setBranches(mergeBranchesByPriorityThenCreatedDateThenName(intoState.getBranches(), toMergeState.getBranches(), priorityBranchNames));

        Map<String, BranchStateTextUnit> intoStateUpdated = intoState.getMd5ToBranchStateTextUnits().values().stream()
                .map(intoStateTextUnit -> {
                    BranchStateTextUnit updatedIntoStateTextUnit = copy(intoStateTextUnit);
                    updatedIntoStateTextUnit.setBranchToBranchDatas(getSortedNewBranchesAndDataForBaseTextUnit(intoStateTextUnit, toMergeState, priorityBranchNames));
                    return updatedIntoStateTextUnit;
                })
                .collect(ImmutableMap.toImmutableMap(BranchStateTextUnit::getMd5, Function.identity()));

        List<BranchStateTextUnit> fromToMergeState = toMergeState.getMd5ToBranchStateTextUnits().values().stream()
                .filter(tu -> !intoStateUpdated.keySet().contains(tu.getMd5()))
                .map(tu -> {
                    BranchStateTextUnit branchStateTextUnit = copy(tu); // If it was immutable we wouldn't need those copy
                    return branchStateTextUnit; // TODO this is not tested
                })
                .collect(Collectors.toList());

        //TODO(perf) ordering breaks test and partially makes sense -- need review
        ImmutableMap<String, BranchStateTextUnit> all = Stream.concat(intoStateUpdated.values().stream(), fromToMergeState.stream())
//                .sorted(Comparator.comparing(BranchStateTextUnit::getCreatedDate, Comparator.nullsLast(Comparator.naturalOrder()))
//                        .thenComparing(BranchStateTextUnit::getMd5, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(ImmutableMap.toImmutableMap(BranchStateTextUnit::getMd5, Function.identity()));

        newState.setMd5ToBranchStateTextUnits(all);
        return newState;

    }

    ImmutableMap<Branch, BranchData> getSortedNewBranchesAndDataForBaseTextUnit(
            BranchStateTextUnit intoStateTextUnit,
            MultiBranchState toMergeState,
            ImmutableList<String> priorityBranchNames) {
        ImmutableMap<Branch, BranchData> newBranchesAndDataForBaseTextUnit = getNewBranchesAndDataForBaseTextUnit(intoStateTextUnit, toMergeState);
        ImmutableMap<Branch, BranchData> sorted = sortBranchesAndData(newBranchesAndDataForBaseTextUnit, priorityBranchNames);
        return sorted;
    }

    ImmutableMap<Branch, BranchData> sortBranchesAndData(ImmutableMap<Branch, BranchData> branchesAndData, ImmutableList<String> priorityBranchNames) {
        return branchesAndData.entrySet().stream()
                .sorted(Map.Entry.comparingByKey((getBranchComparator(priorityBranchNames))))
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Check for matching text unit in the merge state. If it doesn't exist, remove the branches of the merge state
     * from the base text unit (this covers updates in a branch that removes a text units). If it exists, add the
     * branches (and their data) of the text unit to merge (could be in multiple branch but maybe not in all branch of
     * the merge state).
     *
     * @param baseBranchStateTextUnit
     * @param toMergeState
     * @return
     */
    ImmutableMap<Branch, BranchData> getNewBranchesAndDataForBaseTextUnit(BranchStateTextUnit baseBranchStateTextUnit, MultiBranchState toMergeState) {

        Preconditions.checkNotNull(baseBranchStateTextUnit);
        Preconditions.checkNotNull(toMergeState);

        ImmutableMap<Branch, BranchData> branchNamesToBranchDatas;

        BranchStateTextUnit toMergeBranchStateTextUnit = toMergeState.getMd5ToBranchStateTextUnits().get(baseBranchStateTextUnit.getMd5());

        if (toMergeBranchStateTextUnit == null) {
            branchNamesToBranchDatas = baseBranchStateTextUnit.getBranchToBranchDatas().entrySet().stream()
                    .filter(branchBranchDataEntry -> !toMergeState.getBranches().contains(branchBranchDataEntry.getKey()))
                    .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
        } else {
             branchNamesToBranchDatas = Stream.concat(
                    baseBranchStateTextUnit.getBranchToBranchDatas().entrySet().stream(),
                    toMergeBranchStateTextUnit.getBranchToBranchDatas().entrySet().stream())
                    .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue, (branchData, branchData2) -> branchData2));
        }

        return branchNamesToBranchDatas;
    }

    /**
     * First prioritize the branches that are provided, then order them by createdAt and then by name
     *
     * @param branches1
     * @param branches2
     * @param priorityBranchNames
     * @return
     */
    ImmutableSet<Branch> mergeBranchesByPriorityThenCreatedDateThenName(ImmutableSet<Branch> branches1, ImmutableSet<Branch> branches2, ImmutableList<String> priorityBranchNames) {

        Preconditions.checkNotNull(branches1);
        Preconditions.checkNotNull(branches2);
        Preconditions.checkNotNull(priorityBranchNames);

        ImmutableSet<Branch> collect = Stream.concat(branches1.stream(), branches2.stream()).collect(ImmutableSet.toImmutableSet());
        return sortBranchesByPriorityThenCreatedDateThenName(collect, priorityBranchNames);
    }

    /**
     * Sort branches based on provided names, then bey createdAt and finally by name
     *
     * @param branches1
     * @param branches2
     * @param priorityBranchNames
     * @return
     */
    ImmutableSet<Branch> sortBranchesByPriorityThenCreatedDateThenName(ImmutableSet<Branch> branches, ImmutableList<String> priorityBranchNames) {

        Preconditions.checkNotNull(branches);
        Preconditions.checkNotNull(priorityBranchNames);

        ImmutableSet<Branch> sorted = branches.stream()
                .sorted(getBranchComparator(priorityBranchNames))
                .collect(ImmutableSet.toImmutableSet());

        return sorted;
    }

    Comparator<Branch> getBranchComparator(ImmutableList<String> priorityBranchNames) {
        return Comparator
                .comparing((Branch branch) -> -priorityBranchNames.reverse().indexOf(branch.getName()))
                .thenComparing(Branch::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Branch::getName, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    BranchStateTextUnit copy(BranchStateTextUnit source) {
        BranchStateTextUnit updatedIntoStateTextUnit = new BranchStateTextUnit();
        updatedIntoStateTextUnit.setId(source.getId());
        updatedIntoStateTextUnit.setName(source.getName());
        updatedIntoStateTextUnit.setSource(source.getSource());
        updatedIntoStateTextUnit.setComments(source.getComments());
        updatedIntoStateTextUnit.setPluralForm(source.getPluralForm());
        updatedIntoStateTextUnit.setPluralFormOther(source.getPluralFormOther());
        updatedIntoStateTextUnit.setBranchToBranchDatas(source.getBranchToBranchDatas());
        updatedIntoStateTextUnit.setMd5(source.getMd5());
        return updatedIntoStateTextUnit;
    }
}
