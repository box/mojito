package com.box.l10n.mojito.ltm.merger;

import com.box.l10n.mojito.json.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;

@Component
public class MultiBranchStateDiffBuilder {

    // get content for branch to be processed
    // build current MultiBranchState: CS-1 with branch content
    // fetch the base MultiBranchState BS-1
    // merge CS-1 into BS-1, this becomes the new current state: CS-2
    // create text units based on CS-2
    // CS-2 updated with ids of newly create text units gives new current state: CS-3
    // in retry logic with optimistic locking:
    // - check if BS-1 has changed, if so fetch BS-2 and compute new current state: CS-4 = BS2 + CS-3
    // -
    // create ATM from new state

    public MultiBranchState mergeCurrentIntoBaseAndCreateTextUnits(MultiBranchState base, MultiBranchState current, ImmutableList<String> priorityBranchName) {

        // so we merge and just work from one state?

        MultiBranchStateMerger multiBranchStateMerger = new MultiBranchStateMerger();

        MultiBranchState merged = multiBranchStateMerger.merge(base, current, priorityBranchName);

        ImmutableMap<String, BranchStateTextUnit> textUnitsToCreate = getTextUnitsToCreate(merged);

        ImmutableSet<String> namesToCreate = textUnitsToCreate.values().stream()
                .map(BranchStateTextUnit::getName)
//                .peek(n -> System.out.println("nametocreate: " + n))
                .collect(ImmutableSet.toImmutableSet());

        ImmutableListMultimap<String, BranchStateTextUnit> candidateByNames = base.getMd5ToBranchStateTextUnits().values().stream()
                .filter(tu -> namesToCreate.contains(tu.getName()))
                .collect(ImmutableListMultimap.toImmutableListMultimap(BranchStateTextUnit::getName, Function.identity()));


        ImmutableSet<String> sourcesToCreate = textUnitsToCreate.values().stream()
                .map(BranchStateTextUnit::getSource)
                .collect(ImmutableSet.toImmutableSet());

        ImmutableListMultimap<String, BranchStateTextUnit> candidateBySources = base.getMd5ToBranchStateTextUnits().values().stream()
                .filter(tu -> sourcesToCreate.contains(tu.getSource()))
                .collect(ImmutableListMultimap.toImmutableListMultimap(BranchStateTextUnit::getSource, Function.identity()));

        ImmutableList<Match> matchesForSourceLeveraging = textUnitsToCreate.values().stream()
                .map(tu -> {
                    // This mimimics the current source leveraging - the logic could be improved quite a bit:
                    // real detection of refactoring in diff command (if diff command not used probably drop the match based name anyway)
                    // coupled with sensible matching here
                    BranchStateTextUnit match = null;
                    boolean uniqueMatch = false;
                    boolean translationNeededIfUniqueMatch = true;

                    if (match == null) {
                        ImmutableList<BranchStateTextUnit> byNameAndContentAndUsed = candidateByNames.get(tu.getMd5()).stream()
                                .filter(m -> m.getSource().equals(tu.getSource()))
                                .filter(usedBranchStateTextUnit())
                                .collect(ImmutableList.toImmutableList());

                        match = byNameAndContentAndUsed.stream().findFirst().orElse(null);
                        uniqueMatch = byNameAndContentAndUsed.size() == 1;
                        translationNeededIfUniqueMatch = false;
                    }

                    if (match == null) {
                        ImmutableList<BranchStateTextUnit> byNameAndUsed = candidateByNames.get(tu.getMd5()).stream()
                                .filter(usedBranchStateTextUnit())
                                .collect(ImmutableList.toImmutableList());

                        match = byNameAndUsed.stream().findFirst().orElse(null);
                        uniqueMatch = byNameAndUsed.size() == 1;
                        translationNeededIfUniqueMatch = true;
                    }

                    if (match == null) {
                        ImmutableList<BranchStateTextUnit> byContent = candidateBySources.get(tu.getSource()).stream()
                                .filter(usedBranchStateTextUnit())
                                .collect(ImmutableList.toImmutableList());

                        match = byContent.stream().findFirst().orElse(null);
                        uniqueMatch = byContent.size() == 1;
                        translationNeededIfUniqueMatch = true;
                    }

                    if (match == null) {
                        ImmutableList<BranchStateTextUnit> byNameAndContentAndUnused = candidateByNames.get(tu.getMd5()).stream()
                                .filter(m -> m.getSource().equals(tu.getSource()))
                                .filter(unusedBranchStateTextUnit())
                                .collect(ImmutableList.toImmutableList());

                        match = byNameAndContentAndUnused.stream().findFirst().orElse(null);
                        uniqueMatch = byNameAndContentAndUnused.size() == 1;
                        translationNeededIfUniqueMatch = false;
                    }

                    if (match != null) {
                        System.out.println("Match: " + ObjectMapper.withIndentedOutput().writeValueAsStringUnchecked(match));
                    }

                    return new Match(tu, match, uniqueMatch, translationNeededIfUniqueMatch);
                })
                .collect(ImmutableList.toImmutableList());


        // this list will at worse contain extra text units due to concurrent modification, we just need to handle that
        // properly when updating the DB
        ImmutableMap<String, Long> createdTextUnits = createTextUnits(textUnitsToCreate);

        MultiBranchState mergedUpdated = new MultiBranchState();
        mergedUpdated.setBranches(merged.getBranches());
        mergedUpdated.setMd5ToBranchStateTextUnits(merged.getMd5ToBranchStateTextUnits().values().stream()
                .map(tu -> {
                    tu.setId(createdTextUnits.get(tu.getMd5()));
                    return tu;
                }).collect(ImmutableMap.toImmutableMap(BranchStateTextUnit::getMd5, Function.identity()))
        );

        return mergedUpdated;
    }

    Predicate<BranchStateTextUnit> usedBranchStateTextUnit() {
        return m -> !m.getBranchToBranchDatas().isEmpty();
    }

    Predicate<BranchStateTextUnit> unusedBranchStateTextUnit() {
        return m -> m.getBranchToBranchDatas().isEmpty();
    }

    private Comparator<String> equalFirstComparator() {
        return (o1, o2) -> o1.equals(o2) ? -1 : 0;
    }


    /**
     * Text units in current that don't have a matching entry by MD5 in the base state.
     */
    ImmutableMap<String, BranchStateTextUnit> getTextUnitsToCreate(MultiBranchState multiBranchState) {
        return multiBranchState.getMd5ToBranchStateTextUnits().values().stream()
                .filter(tu -> tu.getId() == null)
                .collect(ImmutableMap.toImmutableMap(BranchStateTextUnit::getMd5, Function.identity()));
    }

    /**
     * this is a stub, it actually needs to use the DB ect.
     *
     * @param toCreateTmTextUnits
     * @return
     */
    ImmutableMap<String, Long> createTextUnits(ImmutableMap<String, BranchStateTextUnit> toCreateTmTextUnits) {
        Random random = new Random();
        return toCreateTmTextUnits.values().stream().collect(ImmutableMap.toImmutableMap(BranchStateTextUnit::getMd5, (e) -> random.nextLong()));
    }
}
