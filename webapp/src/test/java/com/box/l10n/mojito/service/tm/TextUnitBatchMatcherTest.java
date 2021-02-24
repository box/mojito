package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TextUnitBatchMatcherTest {

    static final String PLURAL_SEPARATOR = "_";

    TextUnitBatchMatcher textUnitBatchMatcher;

    @Before
    public void before() {
        textUnitBatchMatcher = new TextUnitBatchMatcher();
        textUnitBatchMatcher.pluralNameParser = new PluralNameParser();
    }

    @Test
    public void testNotAlreadyMatched() {

        Predicate<TextUnitDTO> notAlreadyMatched = textUnitBatchMatcher.notAlreadyMatched("test");

        TextUnitDTO textUnitDTO = new TextUnitDTO();
        textUnitDTO.setTmTextUnitId(1000L);

        assertTrue(notAlreadyMatched.test(textUnitDTO));
        assertFalse(notAlreadyMatched.test(textUnitDTO));
    }

    @Test
    public void testMatch() {

        List<TextUnitDTO> existingTextUnitDTOs = Arrays.asList(
                createTextUnitDTO("name-0"),
                createTextUnitDTO("name-1"),
                createTextUnitDTO("will map by tmTextUnit id", 3L),
                createUnusedTextUnitDTO("name-4"));

        Function<TextUnitForBatchMatcher, Optional<TextUnitDTO>> matchByPluralPrefix = textUnitBatchMatcher.match(existingTextUnitDTOs);

        TextUnitDTO name0 = matchByPluralPrefix.apply(createTextUnitForBatchMatcher("name-0")).get();
        assertEquals("name-0", name0.getName());

        Optional<TextUnitDTO> name0SecondTime = matchByPluralPrefix.apply(createTextUnitForBatchMatcher("name-0"));
        assertFalse(name0SecondTime.isPresent());

        TextUnitDTO name1 = matchByPluralPrefix.apply(createTextUnitForBatchMatcher("name-1")).get();
        assertEquals("name-1", name1.getName());

        TextUnitDTO name3 = matchByPluralPrefix.apply(createTextUnitForBatchMatcher("will map tmTextUnit id (diff)", 3L)).get();
        assertEquals("will map by tmTextUnit id", name3.getName());

        TextUnitDTO name4 = matchByPluralPrefix.apply(createTextUnitForBatchMatcher("name-4")).get();
        assertEquals("name-4", name4.getName());
    }

    @Test
    public void testMatchByPluralPrefixNoPlural() {
        List<TextUnitDTO> existingTextUnitDTOs = Arrays.asList(
                createTextUnitDTO("name-0"),
                createTextUnitDTO("name-1"));

        Function<TextUnitForBatchMatcher, Optional<List<TextUnitDTO>>> matchByPluralPrefix = textUnitBatchMatcher.createMatchByPluralPrefixAndUsed(existingTextUnitDTOs, PLURAL_SEPARATOR);

        Optional<List<TextUnitDTO>> result = matchByPluralPrefix.apply(createPluralTextUnitForBatchMatcher("name-0"));
        assertFalse(result.isPresent());
    }

    @Test
    public void testMatchByPluralPrefix() {
        List<TextUnitDTO> existingTextUnitDTOs = Arrays.asList(
                createTextUnitDTO("name-0"),
                createTextUnitDTO("name-1"),
                createPluralTextUnitDTO("name-2", "other"),
                createPluralTextUnitDTO("name-2", "one"),
                createPluralTextUnitDTO("name-3", "zero"),
                createPluralTextUnitDTO("name-3", "one"),
                createPluralTextUnitDTO("name-3", "two"),
                createPluralTextUnitDTO("name-3", "few"),
                createPluralTextUnitDTO("name-3", "many"),
                createPluralTextUnitDTO("name-3", "other")
        );

        Function<TextUnitForBatchMatcher, Optional<List<TextUnitDTO>>> matchByPluralPrefix = textUnitBatchMatcher.createMatchByPluralPrefixAndUsed(existingTextUnitDTOs, PLURAL_SEPARATOR);

        List<TextUnitDTO> name2 = matchByPluralPrefix.apply(createPluralTextUnitForBatchMatcher("name-2")).get();
        assertEquals(2, name2.size());
        assertEquals("name-2_other", name2.get(0).getName());
        assertEquals("name-2_one", name2.get(1).getName());

        List<TextUnitDTO> name3 = matchByPluralPrefix.apply(createPluralTextUnitForBatchMatcher("name-3")).get();
        assertEquals(6, name3.size());
        assertEquals("name-3_zero", name3.get(0).getName());
        assertEquals("name-3_one", name3.get(1).getName());
        assertEquals("name-3_two", name3.get(2).getName());
        assertEquals("name-3_few", name3.get(3).getName());
        assertEquals("name-3_many", name3.get(4).getName());
        assertEquals("name-3_other", name3.get(5).getName());

        Optional<List<TextUnitDTO>> name3SecondTime = matchByPluralPrefix.apply(createPluralTextUnitForBatchMatcher("name-3"));
        assertFalse(name3SecondTime.isPresent());
    }

    @Test
    public void testMatchByPluralPrefixUnused() {
        List<TextUnitDTO> existingTextUnitDTOs = Arrays.asList(
                createTextUnitDTO("name-0"),
                createTextUnitDTO("name-1"),
                createUnusedPluralTextUnitDTO("name-2", "other"),
                createUnusedPluralTextUnitDTO("name-2", "one"),
                createUnusedPluralTextUnitDTO("name-3", "zero"),
                createUnusedPluralTextUnitDTO("name-3", "one"),
                createUnusedPluralTextUnitDTO("name-3", "two"),
                createUnusedPluralTextUnitDTO("name-3", "few"),
                createUnusedPluralTextUnitDTO("name-3", "many"),
                createUnusedPluralTextUnitDTO("name-3", "other")
        );

        Function<TextUnitForBatchMatcher, Optional<List<TextUnitDTO>>> matchByPluralPrefix = textUnitBatchMatcher.createMatchByPluralPrefixAndUnused(existingTextUnitDTOs, PLURAL_SEPARATOR);

        List<TextUnitDTO> name2 = matchByPluralPrefix.apply(createPluralTextUnitForBatchMatcher("name-2")).get();
        assertEquals(2, name2.size());
        assertEquals("name-2_other", name2.get(0).getName());
        assertEquals("name-2_one", name2.get(1).getName());

        List<TextUnitDTO> name3 = matchByPluralPrefix.apply(createPluralTextUnitForBatchMatcher("name-3")).get();
        assertEquals(6, name3.size());
        assertEquals("name-3_zero", name3.get(0).getName());
        assertEquals("name-3_one", name3.get(1).getName());
        assertEquals("name-3_two", name3.get(2).getName());
        assertEquals("name-3_few", name3.get(3).getName());
        assertEquals("name-3_many", name3.get(4).getName());
        assertEquals("name-3_other", name3.get(5).getName());

        Optional<List<TextUnitDTO>> name3SecondTime = matchByPluralPrefix.apply(createPluralTextUnitForBatchMatcher("name-3"));
        assertFalse(name3SecondTime.isPresent());
    }

    @Test
    public void testMatchByNameAndPluralPrefix() {
        Function<TextUnitForBatchMatcher, List<TextUnitDTO>> textUnitForBatchMatcherListFunction = textUnitBatchMatcher.matchByNameAndPluralPrefix(Collections.emptyList(), PLURAL_SEPARATOR);
        textUnitForBatchMatcherListFunction.apply(createTextUnitForBatchMatcher("test"));
    }


    TextUnitForBatchMatcher createTextUnitForBatchMatcher(String name) {
        return createTextUnitForBatchMatcher(name, UUID.randomUUID().getMostSignificantBits());
    }

    TextUnitForBatchMatcher createTextUnitForBatchMatcher(String name, Long id) {
        return createTextUnitForBatchMatcher(name, id, false);
    }

    TextUnitForBatchMatcher createTextUnitForBatchMatcher(String name, Long id, boolean isNamePluralPrefix) {
        TextUnitForBatchMatcher textUnitForBatchMatcher = new TextUnitForBatchMatcher() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public Long getTmTextUnitId() {
                return id;
            }

            @Override
            public boolean isNamePluralPrefix() {
                return isNamePluralPrefix;
            }
        };

        return textUnitForBatchMatcher;
    }

    TextUnitForBatchMatcher createPluralTextUnitForBatchMatcher(String name) {
        return createTextUnitForBatchMatcher(name,  UUID.randomUUID().getMostSignificantBits(), true);
    }

    TextUnitDTO createTextUnitDTO(String name) {
        return createTextUnitDTO(name, UUID.randomUUID().getMostSignificantBits());
    }

    TextUnitDTO createTextUnitDTO(String name, Long tmTextUnitId) {
        TextUnitDTO textUnitDTO = new TextUnitDTO() {
            @Override
            public boolean isUsed() {
                return true;
            }
        };
        textUnitDTO.setName(name);
        textUnitDTO.setTmTextUnitId(tmTextUnitId);
        return textUnitDTO;
    }

    TextUnitDTO createUnusedTextUnitDTO(String name) {
        TextUnitDTO textUnitDTO = new TextUnitDTO() {
            @Override
            public boolean isUsed() {
                return false;
            }
        };
        textUnitDTO.setName(name);
        textUnitDTO.setTmTextUnitId(UUID.randomUUID().getMostSignificantBits());
        return textUnitDTO;
    }

    TextUnitDTO createPluralTextUnitDTO(String prefix, String form) {
        TextUnitDTO textUnitDTO = createTextUnitDTO(prefix + PLURAL_SEPARATOR + form);
        textUnitDTO.setPluralForm(form);
        textUnitDTO.setPluralFormOther(prefix + "_other");
        return textUnitDTO;
    }

    TextUnitDTO createUnusedPluralTextUnitDTO(String prefix, String form) {
        TextUnitDTO textUnitDTO = createUnusedTextUnitDTO(prefix + "_" + form);
        textUnitDTO.setPluralForm(form);
        textUnitDTO.setPluralFormOther(prefix + "_other");
        return textUnitDTO;
    }
}
