package com.box.l10n.mojito.service.tm.importer;

import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.utils.Optionals;
import org.apache.mina.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.box.l10n.mojito.utils.Predicates.not;
import static java.util.stream.Collectors.groupingBy;

@Component
public class TextUnitForBatchImportMatcher {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(TextUnitForBatchImportMatcher.class);

    /**
     * Creates a function that matches a text unit to one of the provided text units.
     *
     * Provided text units can be mapped only once.
     *
     * Machting is first performed by looking at the {@link TextUnitDTO#tmTextUnitId}. Then it looks at the
     * {@link TextUnitDTO#name} first amongst used text units and then unused.
     *
     * @param existingTextUnits
     * @return
     */
    Function<TextUnitForBatchImport, Optional<TextUnitDTO>> match(List<TextUnitDTO> existingTextUnits) {

        Function<TextUnitForBatchImport, Optional<TextUnitDTO>> matchByTmTextUnitId = createMatchByTmTextUnitId(existingTextUnits);
        Function<TextUnitForBatchImport, Optional<TextUnitDTO>> matchByNameAndUsed = createMatchByNameAndUsed(existingTextUnits);
        Function<TextUnitForBatchImport, Optional<TextUnitDTO>> matchByNameAndUnused = createMatchByNameAndUnused(existingTextUnits);
        Predicate<TextUnitDTO> notAlreadyMatched = notAlreadyMatched("global");

        return textUnitForBatchImport -> {
            return Optionals.or(textUnitForBatchImport, matchByTmTextUnitId, matchByNameAndUsed, matchByNameAndUnused).filter(notAlreadyMatched);
        };
    }

    Function<TextUnitForBatchImport, Optional<TextUnitDTO>> createMatchByTmTextUnitId(List<TextUnitDTO> existingTextUnits) {

        logger.debug("Create the map to match by id");
        Map<Long, TextUnitDTO> tmTextUnitIdToTextUnitDTO = existingTextUnits.stream().collect(
                Collectors.toMap(TextUnitDTO::getTmTextUnitId, Function.identity()));

        logger.debug("createMatchByTmTextUnitId function");
        return (textUnitForBatchImport) -> {
            Optional<TextUnitDTO> textUnitDTO = Optional.ofNullable(tmTextUnitIdToTextUnitDTO.get(textUnitForBatchImport.getTmTextUnitId()));
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
    Function<TextUnitForBatchImport, Optional<TextUnitDTO>> createMatchByNameAndUsed(List<TextUnitDTO> existingTextUnits) {

        logger.debug("Create the map to match by name and used text units");
        Map<String, List<TextUnitDTO>> nameToUsedTextUnitDTO = existingTextUnits.stream().filter(TextUnitDTO::isUsed).collect(groupingBy(TextUnitDTO::getName));
        Predicate<TextUnitDTO> byNameNotAlreadyMatched = notAlreadyMatched("byNameAndUsed");

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

            return textUnitDTOS.stream().filter(byNameNotAlreadyMatched).findFirst();
        };
    }

    /**
     * Match unused text units by name.
     *
     * Only return a match by name from unused text units if it is unique.
     *
     * @param existingTextUnits
     * @return
     */
    Function<TextUnitForBatchImport, Optional<TextUnitDTO>> createMatchByNameAndUnused(List<TextUnitDTO> existingTextUnits) {

        logger.debug("Create the map to match by name and unused text units");
        Map<String, List<TextUnitDTO>> nameToUnusedTextUnitDTO = existingTextUnits.stream().filter(not(TextUnitDTO::isUsed)).collect(groupingBy(TextUnitDTO::getName));

        logger.debug("createMatchByNameAndUnused");
        return (textUnitForBatchImport) -> {
            List<TextUnitDTO> candidates = Optional.ofNullable(nameToUnusedTextUnitDTO.get(textUnitForBatchImport.getName())).orElseGet(ArrayList::new);

            Optional<TextUnitDTO> textUnitDTO = Optional.empty();

            if (candidates.size() == 1) {
                logger.debug("Unique match by name: {} and unused", textUnitForBatchImport.getName());
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
        return (t) -> {
            boolean notAlreadyMatched = !alreadyMappedTmTextUnitIds.contains(t.getTmTextUnitId());
            if (notAlreadyMatched) {
                logger.debug("Text unit: {} ({}) not matched yet in context: {}", t.getTmTextUnitId(), t.getName(), context);
                alreadyMappedTmTextUnitIds.add(t.getTmTextUnitId());
            } else {
                logger.debug("Text unit: {}, ({}) is already matched in context: {}, can't used it", t.getTmTextUnitId(), t.getName(), context);
            }
            return notAlreadyMatched;
        };
    }
}
