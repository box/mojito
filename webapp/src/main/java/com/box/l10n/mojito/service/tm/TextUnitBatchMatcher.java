package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.utils.Optionals;
import com.google.common.collect.ImmutableList;
import org.apache.mina.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.box.l10n.mojito.utils.Predicates.not;
import static java.util.stream.Collectors.groupingBy;

@Component
public class TextUnitBatchMatcher {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(TextUnitBatchMatcher.class);

    @Autowired
    PluralNameParser pluralNameParser;

    /**
     * Creates a function that matches a text unit to one of the provided text units.
     * <p>
     * Provided text units can be mapped only once.
     * <p>
     * Machting is first performed by looking at the {@link TextUnitDTO#tmTextUnitId}. Then it looks at the
     * {@link TextUnitDTO#name} first amongst used text units and then unused.
     *
     * @param existingTextUnits
     * @return
     */
    public Function<TextUnitForBatchMatcher, Optional<TextUnitDTO>> match(List<TextUnitDTO> existingTextUnits) {

        Function<TextUnitForBatchMatcher, Optional<TextUnitDTO>> matchByTmTextUnitId = createMatchByTmTextUnitId(existingTextUnits);
        Function<TextUnitForBatchMatcher, Optional<TextUnitDTO>> matchByNameAndUsed = createMatchByNameAndUsed(existingTextUnits);
        Function<TextUnitForBatchMatcher, Optional<TextUnitDTO>> matchByNameAndUnused = createMatchByNameAndUnused(existingTextUnits);
        Predicate<TextUnitDTO> notAlreadyMatched = notAlreadyMatched("global");

        return textUnitForBatchMatcher -> {
            return Optionals.or(textUnitForBatchMatcher, notAlreadyMatched, matchByTmTextUnitId, matchByNameAndUsed, matchByNameAndUnused);
        };
    }

    public Function<TextUnitForBatchMatcher, List<TextUnitDTO>> matchByNameAndPluralPrefix(List<TextUnitDTO> existingTextUnits, String pluralSeparator) {
        Function<TextUnitForBatchMatcher, Optional<List<TextUnitDTO>>> matchByPluralPrefixAndUsed = createMatchByPluralPrefixAndUsed(existingTextUnits, pluralSeparator);
        Function<TextUnitForBatchMatcher, Optional<List<TextUnitDTO>>> matchByNameAndUsed = createMatchByNameAndUsed(existingTextUnits).andThen(Optionals::optionalToOptionalList);
        Function<TextUnitForBatchMatcher, Optional<List<TextUnitDTO>>> matchByPluralPrefixAndUnused = createMatchByPluralPrefixAndUnused(existingTextUnits, pluralSeparator);
        Function<TextUnitForBatchMatcher, Optional<List<TextUnitDTO>>> matchByNameAndUnused = createMatchByNameAndUnused(existingTextUnits).andThen(Optionals::optionalToOptionalList);
        Predicate<List<TextUnitDTO>> notAlreadyMatchedInList = notAlreadyMatchedInList("global");

        return textUnitForBatchMatcher -> {
            ImmutableList<TextUnitDTO> textUnitDTOs = Stream.of(matchByPluralPrefixAndUsed, matchByNameAndUsed, matchByPluralPrefixAndUnused, matchByNameAndUnused)
                    .map(f -> f.apply(textUnitForBatchMatcher))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(notAlreadyMatchedInList)
                    .flatMap(textUnitDTOS -> textUnitDTOS.stream())
                    .collect(ImmutableList.toImmutableList());
            return textUnitDTOs;
        };
    }

    Function<TextUnitForBatchMatcher, Optional<TextUnitDTO>> createMatchByTmTextUnitId(List<TextUnitDTO> existingTextUnits) {

        logger.debug("Create the map to match by id");
        Map<Long, TextUnitDTO> tmTextUnitIdToTextUnitDTO = existingTextUnits.stream().collect(
                Collectors.toMap(TextUnitDTO::getTmTextUnitId, Function.identity()));

        logger.debug("createMatchByTmTextUnitId function");
        return (textUnitForBatchMatcher) -> {
            Optional<TextUnitDTO> textUnitDTO = Optional.ofNullable(tmTextUnitIdToTextUnitDTO.get(textUnitForBatchMatcher.getTmTextUnitId()));
            textUnitDTO.ifPresent(t -> logger.debug("Got match by tmTextUnitId: {}", t.getTmTextUnitId()));
            return textUnitDTO;
        };
    }

    /**
     * Match used text units by name.
     * <p>
     * If multiple text unit matches, return them in order with the hope that the order was preserved and that it will
     * match to the right text units. This is flakey and should be avoided by providing "id"s for matching.
     *
     * @param existingTextUnits
     * @return
     */
    Function<TextUnitForBatchMatcher, Optional<TextUnitDTO>> createMatchByNameAndUsed(List<TextUnitDTO> existingTextUnits) {

        logger.debug("Create the map to match by name and used text units");
        Map<String, List<TextUnitDTO>> nameToUsedTextUnitDTO = existingTextUnits.stream().filter(TextUnitDTO::isUsed).collect(groupingBy(TextUnitDTO::getName));
        Predicate<TextUnitDTO> byNameAndUsedNotAlreadyMatched = notAlreadyMatched("byNameAndUsed");

        logger.debug("createMatchByNameAndUsed");
        return (textUnitForBatchImport) -> {

            List<TextUnitDTO> textUnitDTOS = Optional.ofNullable(nameToUsedTextUnitDTO.get(textUnitForBatchImport.getName())).orElseGet(ArrayList::new);

            if (textUnitDTOS.size() == 1) {
                logger.debug("Unique match by name: {} and used", textUnitForBatchImport.getName());
            } else if (textUnitDTOS.size() > 1) {
                logger.debug("Multiple matches by name: {} and used\nFlakey, this will randomly (hoping order will " +
                                "put the translations for the right text units) select where the translation is " +
                                "added and must be avoided by providing tmTextUnitId in the import. Do this to not fail the" +
                                "import. Duplicated names can easily happen when working with branches (while without it should not)",
                        textUnitForBatchImport.getName());
            }

            return textUnitDTOS.stream().filter(byNameAndUsedNotAlreadyMatched).findFirst();
        };
    }

    /**
     * Match used text units by plural prefix.
     * <p>
     * Plural text units are grouped by prefix. The plural separator is assumed to be: "_" at the very end of the string
     *
     * @param existingTextUnits
     * @param pluralSeparator
     * @return
     */
    Function<TextUnitForBatchMatcher, Optional<List<TextUnitDTO>>> createMatchByPluralPrefixAndUsed(List<TextUnitDTO> existingTextUnits, String pluralSeparator) {

        logger.debug("Create the map to match by prefix and used text units");
        Map<String, List<TextUnitDTO>> pluralPrefixToUsedTextUnitDTOsMap = existingTextUnits.stream()
                .filter(TextUnitDTO::isUsed)
                .filter(t -> t.getPluralForm() != null)
                .collect(groupingBy(o -> pluralNameParser.getPrefix(o.getPluralFormOther(), pluralSeparator)));

        Predicate<TextUnitDTO> byPluralPreifxNotAlreadyMatched = notAlreadyMatched("byPluralPrefixAndUsed");

        logger.debug("createMatchByPluralPrefixAndUsed");
        return (textUnitForBatchMatcher) -> {
            Optional<List<TextUnitDTO>> optionalTextUnitDTOS = Optional.empty();
            if (textUnitForBatchMatcher.isNamePluralPrefix()) {
                List<TextUnitDTO> textUnitDTOS = Optional.ofNullable(pluralPrefixToUsedTextUnitDTOsMap.get(textUnitForBatchMatcher.getName())).orElseGet(ArrayList::new);
                List<TextUnitDTO> filtered = textUnitDTOS.stream()
                        .filter(byPluralPreifxNotAlreadyMatched)
                        .collect(Collectors.toList());

                if (!filtered.isEmpty()) {
                    optionalTextUnitDTOS = Optional.of(filtered);
                }
            }
            return optionalTextUnitDTOS;
        };
    }

    Function<TextUnitForBatchMatcher, Optional<List<TextUnitDTO>>> createMatchByPluralPrefixAndUnused(List<TextUnitDTO> existingTextUnits, String pluralSeparator) {

        logger.debug("Create the map to match by prefix and not used text units");
        Map<String, List<TextUnitDTO>> pluralPrefixToUsedTextUnitDTOsMap = existingTextUnits.stream()
                .filter(not(TextUnitDTO::isUsed))
                .filter(t -> t.getPluralForm() != null)
                .collect(groupingBy(o -> pluralNameParser.getPrefix(o.getPluralFormOther(), pluralSeparator)));

        Predicate<TextUnitDTO> byPluralPreifxNotAlreadyMatched = notAlreadyMatched("byPluralPrefixAndUnused");

        logger.debug("createMatchByPluralPrefixAndUnused");
        return (textUnitForBatchMatcher) -> {
            Optional<List<TextUnitDTO>> optionalTextUnitDTOS = Optional.empty();
            if (textUnitForBatchMatcher.isNamePluralPrefix()) {
                List<TextUnitDTO> textUnitDTOS = Optional.ofNullable(pluralPrefixToUsedTextUnitDTOsMap.get(textUnitForBatchMatcher.getName())).orElseGet(ArrayList::new);
                List<TextUnitDTO> filtered = textUnitDTOS.stream()
                        .filter(byPluralPreifxNotAlreadyMatched)
                        .collect(Collectors.toList());

                if (filtered.size() == 0 || filtered.size() > 6) {
                    logger.debug("No unique match in unused, skip");
                } else {
                    logger.debug("Unique match by name: {} and unused", textUnitForBatchMatcher.getName());
                    optionalTextUnitDTOS = Optional.of(filtered);
                }
            }

            return optionalTextUnitDTOS;
        };
    }

    /**
     * Match unused text units by name.
     * <p>
     * Only return a match by name from unused text units if it is unique.
     *
     * @param existingTextUnits
     * @return
     */
    Function<TextUnitForBatchMatcher, Optional<TextUnitDTO>> createMatchByNameAndUnused(List<TextUnitDTO> existingTextUnits) {

        logger.debug("Create the map to match by name and unused text units");
        Map<String, List<TextUnitDTO>> nameToUnusedTextUnitDTO = existingTextUnits.stream().filter(not(TextUnitDTO::isUsed)).collect(groupingBy(TextUnitDTO::getName));

        logger.debug("createMatchByNameAndUnused");
        return (textUnitForBatchMatcher) -> {
            List<TextUnitDTO> candidates = Optional.ofNullable(nameToUnusedTextUnitDTO.get(textUnitForBatchMatcher.getName())).orElseGet(ArrayList::new);

            Optional<TextUnitDTO> textUnitDTO = Optional.empty();

            if (candidates.size() == 1) {
                logger.debug("Unique match by name: {} and unused", textUnitForBatchMatcher.getName());
                textUnitDTO = Optional.of(candidates.get(0));
            } else if (candidates.size() > 1) {
                logger.debug("No unique match in unused, skip");
            }

            return textUnitDTO;
        };
    }

    /**
     * Predicate that make sure that text units are matched only once.
     * <p>
     * Matching multiple times the same text unit would break the batch import with constraint violation.
     *
     * @param context to distinguish in which context the predicate is used when logging
     * @return
     */
    Predicate<TextUnitDTO> notAlreadyMatched(String context) {
        ConcurrentHashSet<Long> alreadyMappedTmTextUnitIds = new ConcurrentHashSet<>();
        return (textUnitDTO) -> {
            boolean notAlreadyMatched = !alreadyMappedTmTextUnitIds.contains(textUnitDTO.getTmTextUnitId());
            if (notAlreadyMatched) {
                logger.debug("Text unit: {} ({}) not matched yet in context: {}", textUnitDTO.getTmTextUnitId(), textUnitDTO.getName(), context);
                alreadyMappedTmTextUnitIds.add(textUnitDTO.getTmTextUnitId());
            } else {
                logger.debug("Text unit: {}, ({}) is already matched in context: {}, can't used it", textUnitDTO.getTmTextUnitId(), textUnitDTO.getName(), context);
            }
            return notAlreadyMatched;
        };
    }

    /**
     * Predicate to make sure that text units are matched only once. Discard the whole list if a single entry matches.
     *
     * @param context
     * @return
     */
    Predicate<List<TextUnitDTO>> notAlreadyMatchedInList(String context) {
        ConcurrentHashSet<Long> alreadyMappedTmTextUnitIds = new ConcurrentHashSet<>();
        return (textUnitDTOs) -> {
            boolean notAlreadyMatched = textUnitDTOs.stream().noneMatch(t -> alreadyMappedTmTextUnitIds.contains(t.getTmTextUnitId()));
            if (notAlreadyMatched) {
                logger.debug("Text units: not matched yet in context: {}", context);
                alreadyMappedTmTextUnitIds.addAll(textUnitDTOs.stream().map(TextUnitDTO::getTmTextUnitId).collect(Collectors.toList()));
            } else {
                logger.debug("List contains a text unit that is already matched in context: {}, can't used it", context);
            }
            return notAlreadyMatched;
        };
    }
}
