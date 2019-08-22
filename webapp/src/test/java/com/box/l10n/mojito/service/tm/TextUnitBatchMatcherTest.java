package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.service.tm.TextUnitBatchMatcher;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.Assert.*;

public class TextUnitBatchMatcherTest {

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

        Function<TextUnitForBatchMatcher, List<TextUnitDTO>> matchByPluralPrefix = textUnitBatchMatcher.createMatchByPluralPrefix(existingTextUnitDTOs);

        List<TextUnitDTO> result = matchByPluralPrefix.apply(createTextUnitForBatchMatcher("name-0"));
        assertEquals(0, result.size());
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

        Function<TextUnitForBatchMatcher, List<TextUnitDTO>> matchByPluralPrefix = textUnitBatchMatcher.createMatchByPluralPrefix(existingTextUnitDTOs);

        List<TextUnitDTO> name2 = matchByPluralPrefix.apply(createTextUnitForBatchMatcher("name-2"));
        assertEquals(2, name2.size());
        assertEquals("name-2_other", name2.get(0).getName());
        assertEquals("name-2_one", name2.get(1).getName());

        List<TextUnitDTO> name3 = matchByPluralPrefix.apply(createTextUnitForBatchMatcher("name-3"));
        assertEquals(6, name3.size());
        assertEquals("name-3_zero", name3.get(0).getName());
        assertEquals("name-3_one", name3.get(1).getName());
        assertEquals("name-3_two", name3.get(2).getName());
        assertEquals("name-3_few", name3.get(3).getName());
        assertEquals("name-3_many", name3.get(4).getName());
        assertEquals("name-3_other", name3.get(5).getName());

        List<TextUnitDTO> name3SecondTime = matchByPluralPrefix.apply(createTextUnitForBatchMatcher("name-3"));
        assertEquals(0, name3SecondTime.size());
    }


    TextUnitForBatchMatcher createTextUnitForBatchMatcher(String name) {
        return createTextUnitForBatchMatcher(name, UUID.randomUUID().getMostSignificantBits());
    }

    TextUnitForBatchMatcher createTextUnitForBatchMatcher(String name, Long id) {
        TextUnitForBatchMatcher textUnitForBatchMatcher = new TextUnitForBatchMatcher() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public Long getTmTextUnitId() {
                return id;
            }
        };

        return textUnitForBatchMatcher;
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
        TextUnitDTO textUnitDTO = createTextUnitDTO(prefix + "_" + form);
        textUnitDTO.setPluralForm(form);
        textUnitDTO.setPluralFormOther(prefix + "_other");
        return textUnitDTO;
    }
}