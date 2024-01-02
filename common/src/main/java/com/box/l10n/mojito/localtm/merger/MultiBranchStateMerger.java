package com.box.l10n.mojito.localtm.merger;

import com.box.l10n.mojito.collect.ImmutableMapCollectors;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class MultiBranchStateMerger {

  /**
   * This merges the "toMergeState" state into the receiving state "intoState".
   *
   * <p>"id"s are NOT updated during the merge. it is is expected they are kept unchanged, the md5
   * is the natural key used for the merged. ids from toMergeState will be ignored, ids in intoState
   * will be kept as it.
   *
   * @param toMergeState the state to merge into intoState
   * @param intoState the state in which toMergeState will be merge into
   * @param priorityBranchNames list of priority branch names, they should be come first and in
   *     providing order when ordering branches
   * @return a new state result of merging toMergeState into intoState
   */
  public MultiBranchState merge(
      MultiBranchState toMergeState,
      MultiBranchState intoState,
      ImmutableSet<String> priorityBranchNames) {
    ImmutableSet<Branch> newBranches =
        mergeBranchesByPriorityThenCreatedDateThenName(
            intoState.getBranches(), toMergeState.getBranches(), priorityBranchNames);

    ImmutableMap<String, BranchStateTextUnit> intoStateUpdated =
        intoState.getBranchStateTextUnits().stream()
            .map(mergerMultiBranchStateIntoBranchStateTextUnit(toMergeState, newBranches))
            .collect(ImmutableMap.toImmutableMap(BranchStateTextUnit::getMd5, Function.identity()));

    ImmutableList<BranchStateTextUnit> fromToMergeStateNotInIntoState =
        toMergeState.getBranchStateTextUnits().stream()
            .filter(bstu -> !intoStateUpdated.containsKey(bstu.getMd5()))
            .collect(ImmutableList.toImmutableList());

    ImmutableList<BranchStateTextUnit> all =
        Stream.concat(intoStateUpdated.values().stream(), fromToMergeStateNotInIntoState.stream())
            .collect(ImmutableList.toImmutableList());

    return MultiBranchState.of().withBranches(newBranches).withBranchStateTextUnits(all);
  }

  public MultiBranchState removeBranch(MultiBranchState state, String branchName) {

    ImmutableSet<Branch> newBranches =
        state.getBranches().stream()
            .filter(b -> !branchName.equals(b.getName()))
            .collect(ImmutableSet.toImmutableSet());

    ImmutableList<BranchStateTextUnit> intoStateUpdated =
        state.getBranchStateTextUnits().stream()
            .map(
                intoStateTextUnit ->
                    intoStateTextUnit.withBranchNameToBranchDatas(
                        removeBranchByName(intoStateTextUnit, branchName)))
            .collect(ImmutableList.toImmutableList());

    return MultiBranchState.of()
        .withBranches(newBranches)
        .withBranchStateTextUnits(intoStateUpdated);
  }

  /**
   * Search inside the toMergeState for a text unit that match by md5 - If it exists, add the
   * branches (and their data) of that text unit to merge (could be in multiple branch but maybe not
   * in all branch of the merge state) into the "intoBranchStateTextUnit". --> to keep reccord of
   * all the branch in which that text unit is used - If it doesn't exist, remove all the branches
   * of the merge state from the "intoBranchStateTextUnit" --> this covers updates in a branch that
   * removes a text unit and potentially could lead it to be unused (no branch remaining)
   */
  Function<BranchStateTextUnit, BranchStateTextUnit> mergerMultiBranchStateIntoBranchStateTextUnit(
      MultiBranchState toMergeState, ImmutableSet<Branch> sortedBranches) {

    ImmutableMap<String, BranchStateTextUnit> toMergeStateBranchTextUnitsByMd5 =
        toMergeState.getBranchStateTextUnits().stream()
            .collect(ImmutableMap.toImmutableMap(BranchStateTextUnit::getMd5, Function.identity()));

    ImmutableMap<String, Branch> toMergeStateBranchNames =
        toMergeState.getBranches().stream()
            .collect(ImmutableMap.toImmutableMap(Branch::getName, Function.identity()));

    return intoBranchStateTextUnit -> {
      ImmutableMap<String, BranchData> branchNamesToBranchDatas;
      BranchStateTextUnit toMergeBranchStateTextUnit =
          toMergeStateBranchTextUnitsByMd5.get(intoBranchStateTextUnit.getMd5());

      if (toMergeBranchStateTextUnit == null) {
        branchNamesToBranchDatas =
            intoBranchStateTextUnit.getBranchNameToBranchDatas().entrySet().stream()
                .filter(
                    branchNameToBranchData ->
                        !toMergeStateBranchNames.containsKey(branchNameToBranchData.getKey()))
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
      } else {
        branchNamesToBranchDatas =
            Stream.concat(
                    intoBranchStateTextUnit.getBranchNameToBranchDatas().entrySet().stream(),
                    toMergeBranchStateTextUnit.getBranchNameToBranchDatas().entrySet().stream())
                .collect(
                    ImmutableMap.toImmutableMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (branchData1, branchData2) ->
                            // update the branchData by taking the newer entry from
                            // toMergeBranchStateTextUnit (stream order is key)
                            branchData2));
      }

      ImmutableMap<String, BranchData> branchNameToBranchDataUpdated =
          sortBranchDataByBranch(branchNamesToBranchDatas, sortedBranches);
      return intoBranchStateTextUnit.withBranchNameToBranchDatas(branchNameToBranchDataUpdated);
    };
  }

  ImmutableMap<String, BranchData> sortBranchDataByBranch(
      ImmutableMap<String, BranchData> branchNamesToBranchDatas,
      ImmutableSet<Branch> sortedBranches) {
    ImmutableList<String> sortedBranchNames =
        sortedBranches.stream().map(Branch::getName).collect(ImmutableList.toImmutableList());
    return branchNamesToBranchDatas.entrySet().stream()
        .sorted(
            Comparator.comparing(
                stringBranchDataEntry -> {
                  int idx = sortedBranchNames.indexOf(stringBranchDataEntry.getKey());
                  return idx == -1 ? Integer.MAX_VALUE : idx;
                }))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  ImmutableMap<String, BranchData> removeBranchByName(
      BranchStateTextUnit intoStateTextUnit, String branchName) {
    return intoStateTextUnit.getBranchNameToBranchDatas().entrySet().stream()
        .filter(stringBranchDataEntry -> !branchName.equals(stringBranchDataEntry.getKey()))
        .collect(ImmutableMapCollectors.mapEntriesToImmutableMap());
  }

  /**
   * Merge 2 set of branches and order them accroding to {@lin
   * sortBranchesByPriorityThenCreatedDateThenName}.
   *
   * <p>Also dedup branch with same name and different timestamps by keeping the first encountered
   * entry
   *
   * @param branches1
   * @param branches2
   * @param priorityBranchNames
   * @return
   */
  ImmutableSet<Branch> mergeBranchesByPriorityThenCreatedDateThenName(
      ImmutableSet<Branch> branches1,
      ImmutableSet<Branch> branches2,
      ImmutableSet<String> priorityBranchNames) {

    Preconditions.checkNotNull(branches1);
    Preconditions.checkNotNull(branches2);
    Preconditions.checkNotNull(priorityBranchNames);

    ImmutableSet<Branch> collect =
        ImmutableSet.copyOf(
            Stream.concat(branches1.stream(), branches2.stream())
                .collect(
                    ImmutableMap.toImmutableMap(
                        Branch::getName, Function.identity(), (branch1, branch2) -> branch1))
                .values());

    return sortBranchesByPriorityThenCreatedDateThenName(collect, priorityBranchNames);
  }

  /**
   * Sort branches putting first priority branches (following the names order provided), then bey
   * createdAt and finally by name
   *
   * @param branches1
   * @param branches2
   * @param priorityBranchNames those branch will come first, in provided order
   * @return
   */
  ImmutableSet<Branch> sortBranchesByPriorityThenCreatedDateThenName(
      ImmutableSet<Branch> branches, ImmutableSet<String> priorityBranchNames) {

    Preconditions.checkNotNull(branches);
    Preconditions.checkNotNull(priorityBranchNames);

    ImmutableList<String> priorityBranchNamesAsList = priorityBranchNames.asList();
    ImmutableSet<Branch> sorted =
        branches.stream()
            .sorted(
                Comparator.comparing(
                        (Branch branch) -> {
                          int idx = priorityBranchNamesAsList.indexOf(branch.getName());
                          return idx == -1 ? Integer.MAX_VALUE : idx;
                        })
                    .thenComparing(
                        // Comparing with instant. if applying default comparator the logic fails
                        // when TZ are different, even if pointing to the same instant
                        branch ->
                            branch.getCreatedAt() == null
                                ? null
                                : branch.getCreatedAt().toInstant(),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(
                        Branch::getName, Comparator.nullsLast(Comparator.naturalOrder())))
            .collect(ImmutableSet.toImmutableSet());

    return sorted;
  }
}
