package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.ParameterException;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class TmTextUnitMappingConverterTest {

    @Test
    public void testConvertNull() {
        TmTextUnitMappingConverter tmTextUnitMappingConverter = new TmTextUnitMappingConverter();
        Map<Long, Long> convert = tmTextUnitMappingConverter.convert(null);
        assertNull(convert);
    }

    @Test
    public void testConvertValid() {
        TmTextUnitMappingConverter tmTextUnitMappingConverter = new TmTextUnitMappingConverter();
        Map<Long, Long> convert = tmTextUnitMappingConverter.convert("1001:2001;1002:2002");
        assertEquals(2001L, convert.get(1001L).longValue());
        assertEquals(2002L, convert.get(1002L).longValue());
        assertEquals(2, convert.size());
    }

    @Test(expected = ParameterException.class)
    public void testConvertInvalid() {
        TmTextUnitMappingConverter tmTextUnitMappingConverter = new TmTextUnitMappingConverter();
        Map<Long, Long> convert = tmTextUnitMappingConverter.convert("dsafdsf");
        assertEquals(0, convert.size());
    }
}
