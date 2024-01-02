package com.box.l10n.mojito.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.box.l10n.mojito.JSR310Migration;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import org.junit.Test;

/** @author jeanaurambault */
public class LocalTimeConverterTest {

  @Test
  public void testConvert() {
    String source = "14:00";
    LocalTimeConverter localTimeConverter = new LocalTimeConverter();
    LocalTime expResult = JSR310Migration.newLocalTimeWithHMS(14, 0, 0);
    LocalTime result = localTimeConverter.convert(source);
    assertEquals(expResult, result);
  }

  @Test
  public void testConvertNull() {
    LocalTimeConverter localTimeConverter = new LocalTimeConverter();
    LocalTime convert = localTimeConverter.convert(null);
    assertNotNull(convert);
  }

  @Test(expected = DateTimeParseException.class)
  public void testConvertInvaild() {
    LocalTimeConverter localTimeConverter = new LocalTimeConverter();
    localTimeConverter.convert("invalid");
  }
}
