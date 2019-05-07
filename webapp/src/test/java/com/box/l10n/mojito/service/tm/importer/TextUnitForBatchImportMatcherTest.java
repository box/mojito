package com.box.l10n.mojito.service.tm.importer;

import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import org.junit.Test;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.function.Predicate;

import static org.junit.Assert.*;

public class TextUnitForBatchImportMatcherTest {

    @Test
    public void testNotAlreadyMatched() {
        TextUnitForBatchImportMatcher textUnitForBatchImportMatcher = new TextUnitForBatchImportMatcher();
        Predicate<TextUnitDTO> notAlreadyMatched = textUnitForBatchImportMatcher.notAlreadyMatched("test");

        TextUnitDTO textUnitDTO = new TextUnitDTO();
        textUnitDTO.setTmTextUnitId(1000L);

        assertTrue(notAlreadyMatched.test(textUnitDTO));
        assertFalse(notAlreadyMatched.test(textUnitDTO));
    }

}