package com.box.l10n.mojito.rest.textunit;

import static org.junit.Assert.assertEquals;

import org.joda.time.DateTime;
import org.junit.Test;

/** @author jeanaurambault */
public class StringToDateTimeConverterTest {

  @Test
  public void testConvertNull() {
    String source = null;
    StringToDateTimeConverter instance = new StringToDateTimeConverter();
    DateTime expResult = null;
    DateTime result = instance.convert(source);
    assertEquals(expResult, result);
  }

  @Test
  public void testConvertMillisecond() {
    String source = "1525887295000";
    StringToDateTimeConverter instance = new StringToDateTimeConverter();
    DateTime expResult = new DateTime("2018-05-09T17:34:55.000Z");
    DateTime result = instance.convert(source);
    assertEquals(expResult, result);
  }

  @Test
  public void testConvertISO() {
    String source = "2018-05-09T17:34:55.000Z";
    StringToDateTimeConverter instance = new StringToDateTimeConverter();
    DateTime expResult = new DateTime("2018-05-09T17:34:55.000Z");
    DateTime result = instance.convert(source);
    assertEquals(expResult, result);
  }

  @Test
  public void testConvertISONoTZ() {
    String source = "2018-05-09T17:34:55.000";
    StringToDateTimeConverter instance = new StringToDateTimeConverter();
    DateTime expResult = new DateTime("2018-05-09T17:34:55.000Z");
    DateTime result = instance.convert(source);
    assertEquals(expResult, result);
  }

  @Test
  public void testConvertISOUTC() {
    String source = "2018-05-09T17:34:55.000";
    StringToDateTimeConverter instance = new StringToDateTimeConverter();
    DateTime expResult = new DateTime("2018-05-09T17:34:55.000Z");
    DateTime result = instance.convert(source);
    assertEquals(expResult, result);
  }

  @Test
  public void testConvertISOTZ() {
    String source = "2018-05-09T17:34:55.000-07:00";
    StringToDateTimeConverter instance = new StringToDateTimeConverter();
    DateTime expResult = new DateTime("2018-05-10T00:34:55.000Z");
    DateTime result = instance.convert(source);
    assertEquals(expResult, result);
  }

  @Test
  public void testConvertISOTZNoMillisecond() {
    String source = "2018-05-09T17:34:55-07:00";
    StringToDateTimeConverter instance = new StringToDateTimeConverter();
    DateTime expResult = new DateTime("2018-05-10T00:34:55.000Z");
    DateTime result = instance.convert(source);
    assertEquals(expResult, result);
  }
}
