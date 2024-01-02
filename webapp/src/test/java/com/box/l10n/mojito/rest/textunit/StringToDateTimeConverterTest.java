package com.box.l10n.mojito.rest.textunit;

import static org.junit.Assert.assertEquals;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.test.JSR310MigrationForTesting;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.junit.Assume;
import org.junit.Test;

/** @author jeanaurambault */
public class StringToDateTimeConverterTest {

  @Test
  public void testConvertNull() {
    String source = null;
    StringToDateTimeConverter instance = new StringToDateTimeConverter();
    ZonedDateTime expResult = null;
    ZonedDateTime result = instance.convert(source);
    assertEquals(expResult, result);
  }

  @Test
  public void testConvertMillisecond() {
    String source = "1525887295000";
    StringToDateTimeConverter instance = new StringToDateTimeConverter();
    ZonedDateTime expResult =
        JSR310Migration.newDateTimeCtorWithISO8601Str("2018-05-09T17:34:55.000Z");
    ZonedDateTime result = instance.convert(source);
    JSR310MigrationForTesting.junitAssertEquals(expResult, result);
  }

  @Test
  public void testConvertISO() {
    String source = "2018-05-09T17:34:55.000Z";
    StringToDateTimeConverter instance = new StringToDateTimeConverter();
    ZonedDateTime expResult =
        JSR310Migration.newDateTimeCtorWithISO8601Str("2018-05-09T17:34:55.000Z");
    ZonedDateTime result = instance.convert(source);
    assertEquals(expResult, result);
  }

  /**
   * If there is no timezone specified in the source string, then the system default is used.
   *
   * <p>In practice, on the server side, the default TZ is UTC, see test below.
   */
  @Test
  public void testConvertISONoTZ() {
    String source = "2018-05-09T17:34:55.000";
    StringToDateTimeConverter instance = new StringToDateTimeConverter();
    ZonedDateTime expResult =
        JSR310Migration.newDateTimeCtorWithISO8601Str("2018-05-09T17:34:55.000Z")
            .withZoneSameLocal(ZoneId.systemDefault());
    ZonedDateTime result = instance.convert(source);
    JSR310MigrationForTesting.junitAssertEquals(expResult, result);
  }

  /**
   * This test will typically run on CI, and the behavior is mimicking a production deployment which
   * usually is in UTC.
   */
  @Test
  public void testConvertISONoTZAssumeUTC() {
    Assume.assumeTrue(ZoneId.systemDefault().equals(ZoneOffset.UTC));
    String source = "2018-05-09T17:34:55.000";
    StringToDateTimeConverter instance = new StringToDateTimeConverter();
    ZonedDateTime expResult =
        JSR310Migration.newDateTimeCtorWithISO8601Str("2018-05-09T17:34:55.000Z");
    ZonedDateTime result = instance.convert(source);
    JSR310MigrationForTesting.junitAssertEquals(expResult, result);
  }

  @Test
  public void testConvertISOTZ() {
    String source = "2018-05-09T17:34:55.000-07:00";
    StringToDateTimeConverter instance = new StringToDateTimeConverter();
    ZonedDateTime expResult =
        JSR310Migration.newDateTimeCtorWithISO8601Str("2018-05-10T00:34:55.000Z");
    ZonedDateTime result = instance.convert(source);
    JSR310MigrationForTesting.junitAssertEquals(expResult, result);
  }

  @Test
  public void testConvertISOTZNoMillisecond() {
    String source = "2018-05-09T17:34:55-07:00";
    StringToDateTimeConverter instance = new StringToDateTimeConverter();
    ZonedDateTime expResult =
        JSR310Migration.newDateTimeCtorWithISO8601Str("2018-05-10T00:34:55.000Z");
    ZonedDateTime result = instance.convert(source);
    JSR310MigrationForTesting.junitAssertEquals(expResult, result);
  }
}
