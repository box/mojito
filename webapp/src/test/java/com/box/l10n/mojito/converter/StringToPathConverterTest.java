package com.box.l10n.mojito.converter;

import java.nio.file.Path;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jaurambault
 */
public class StringToPathConverterTest {

    @Test
    public void testConvert() {
        String source = "some/path";
        StringToPathConverter instance = new StringToPathConverter();
        Path result = instance.convert(source);
        assertEquals(source, result.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testConvertNull() {
        String source = null;
        StringToPathConverter instance = new StringToPathConverter();
        instance.convert(source);
    }
}
