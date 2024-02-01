package com.box.l10n.mojito.converter;

import static org.junit.Assert.assertEquals;

import com.box.l10n.mojito.JSR310Migration;
import org.junit.Test;
import org.threeten.extra.PeriodDuration;

/**
 * @author jeanaurambault
 */
public class PeriodDurationConverterTest {

  @Test
  public void testConvert() {
    PeriodDurationConverter periodDurationConverter = new PeriodDurationConverter();
    PeriodDuration expResult = JSR310Migration.newPeriodCtorWithHMSM(0, 1, 0, 0);
    PeriodDuration result = periodDurationConverter.convert("60000");
    assertEquals(expResult, result);
  }

  @Test(expected = NumberFormatException.class)
  public void testConvertNull() {
    PeriodDurationConverter periodDurationConverter = new PeriodDurationConverter();
    periodDurationConverter.convert(null);
  }

  @Test(expected = NumberFormatException.class)
  public void testConvertInvalid() {
    PeriodDurationConverter periodDurationConverter = new PeriodDurationConverter();
    periodDurationConverter.convert("invalid");
  }
}
