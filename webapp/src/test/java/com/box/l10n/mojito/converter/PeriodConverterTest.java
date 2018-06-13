package com.box.l10n.mojito.converter;

import org.joda.time.Period;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author jeanaurambault
 */
public class PeriodConverterTest {

    @Test
    public void testConvert() {
        PeriodConverter periodConverter = new PeriodConverter();
        Period expResult = new Period(0, 1, 0, 0);
        Period result = periodConverter.convert("60000");
        assertEquals(expResult, result);
    }

    @Test(expected = NumberFormatException.class)
    public void testConvertNull() {
        PeriodConverter periodConverter = new PeriodConverter();
        periodConverter.convert(null);
    }

    @Test(expected = NumberFormatException.class)
    public void testConvertInvalid() {
        PeriodConverter periodConverter = new PeriodConverter();
        periodConverter.convert("invalid");
    }

}
