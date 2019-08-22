package com.box.l10n.mojito.service.tm;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import static org.junit.Assert.*;

public class PluralNameParserTest {

    PluralNameParser pluralNameParser = new PluralNameParser();

    @Test
    public void testGetPrefix() {
        assertEquals("name", pluralNameParser.getPrefix("name_zero"));
        assertEquals("name", pluralNameParser.getPrefix("name_one"));
        assertEquals("name", pluralNameParser.getPrefix("name_two"));
        assertEquals("name", pluralNameParser.getPrefix("name_few"));
        assertEquals("name", pluralNameParser.getPrefix("name_many"));
        assertEquals("name", pluralNameParser.getPrefix("name_other"));
    }
}