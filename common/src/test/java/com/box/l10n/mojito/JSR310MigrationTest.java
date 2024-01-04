package com.box.l10n.mojito;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.assertj.core.api.Assertions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;
import org.junit.Assert;
import org.junit.Test;

public class JSR310MigrationTest {

  static final DateTime DATE_TIME_WITH_MILLIS = new DateTime(2020, 7, 10, 0, 0, 12, 345);
  static final ZonedDateTime ZONED_DATE_TIME_WITH_MILLIS =
      ZonedDateTime.of(2020, 7, 10, 0, 0, 12, 345000000, ZoneId.systemDefault());

  static final DateTime dateTime = newDateTimeCtorOld(2020, 7, 10, 0, 0);
  static final ZonedDateTime zonedDateTime = JSR310Migration.newDateTimeCtor(2020, 7, 10, 0, 0);

  @Test
  public void toStringEquivalence() {

    if (ZoneId.systemDefault().getId().equals("UTC")) {
      // run in maven:
      // mvn clean install -Dtest=JSR310MigrationTest -P'!frontend' -Duser.timezone=UTC

      // the old to string shows milliseconds and just the offset
      Assertions.assertThat(dateTime.toString()).isEqualTo("2020-07-10T00:00:00.000Z");
      // the new to string does not show milliseconds and adds the zone id
      Assertions.assertThat(zonedDateTime.toString()).isEqualTo("2020-07-10T00:00Z[UTC]");

      // As previous assert shows, the output of the 2 toString() method are not the same.
      // In places where number formatting matters, toString() must be change and an equivalent call
      //
      // A first try is to look at the DateTimeFormatter, this gives a pretty similar result, but it
      // is still missing the milliseconds
      Assertions.assertThat(zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
          .isEqualTo("2020-07-10T00:00:00Z");

    } else if (ZoneId.systemDefault().getId().equals("America/Los_Angeles")) {
      Assertions.assertThat(dateTime.toString()).isEqualTo("2020-07-10T00:00:00.000-07:00");
      Assertions.assertThat(zonedDateTime.toString())
          .isEqualTo("2020-07-10T00:00-07:00[America/Los_Angeles]");
      Assertions.assertThat(zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
          .isEqualTo("2020-07-10T00:00:00-07:00");

    } else if (ZoneId.systemDefault().getId().equals("America/New_York")) {
      Assertions.assertThat(dateTime.toString()).isEqualTo("2020-07-10T00:00:00.000-04:00");
      Assertions.assertThat(zonedDateTime.toString())
          .isEqualTo("2020-07-10T00:00-04:00[America/New_York]");
      Assertions.assertThat(zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
          .isEqualTo("2020-07-10T00:00:00-04:00");
    }
  }

  @Test
  public void dateTimeNow() {
    Assertions.assertThat(dateTimeNowOld())
        .isBetween(
            Instant.now().toDateTime().minusMillis(100),
            Instant.now().toDateTime().plusMillis(100));

    Assertions.assertThat(JSR310Migration.dateTimeNow())
        .isBetween(
            ZonedDateTime.now().minus(100, ChronoUnit.MILLIS),
            ZonedDateTime.now().plus(100, ChronoUnit.MILLIS));
  }

  @Test
  public void getMillis() {
    Assertions.assertThat(getMillisOld(dateTime))
        .isEqualTo(JSR310Migration.getMillis(zonedDateTime));
  }

  @Test
  public void toWordBasedDuration() {
    DateTime end = new DateTime(2020, 8, 10, 0, 0, 13, 450);
    String joda = toWordBaseDurationOld(DATE_TIME_WITH_MILLIS, end);

    ZonedDateTime zend = ZonedDateTime.of(2020, 8, 10, 0, 0, 13, 450000000, ZoneId.systemDefault());
    String jsr = JSR310Migration.toWordBasedDuration(ZONED_DATE_TIME_WITH_MILLIS, zend);

    Assertions.assertThat(joda).isEqualTo(jsr);
  }

  @Test
  public void toWordBasedDurationDifference() {
    DateTime end = new DateTime(2020, 10, 25, 0, 0, 13, 450);
    String joda = toWordBaseDurationOld(DATE_TIME_WITH_MILLIS, end);

    ZonedDateTime zend =
        ZonedDateTime.of(2020, 10, 25, 0, 0, 13, 450000000, ZoneId.systemDefault());
    String jsr = JSR310Migration.toWordBasedDuration(ZONED_DATE_TIME_WITH_MILLIS, zend);

    Assertions.assertThat(joda)
        .isEqualTo("3 months, 2 weeks, 1 day, 1 second and 105 milliseconds");
    Assertions.assertThat(jsr).isEqualTo("3 months, 15 days, 1 second and 105 milliseconds");
  }

  @Test
  public void toWordBasedDurationLong() {
    long start = 1594339212345L;
    long end = 1597017613450L;
    String joda = toWordBaseDurationOld(start, end);
    String jsr = JSR310Migration.toWordBasedDuration(start, end);
    Assertions.assertThat(joda).isEqualTo(jsr);
  }

  @Test
  public void dateTimeToDate() {
    Date joda = dateTimeToDateOld(dateTime);
    Date jsr = JSR310Migration.dateTimeToDate(zonedDateTime);
    Assertions.assertThat(joda).isEqualTo(jsr);
  }

  @Test
  public void dateTimeIsAfter() {
    long before = zonedDateTime.minusMinutes(1).toInstant().toEpochMilli();
    long after = zonedDateTime.plusMinutes(1).toInstant().toEpochMilli();

    Assertions.assertThat(dateTimeIsAfterEpochMillisOld(dateTime, before)).isTrue();
    Assertions.assertThat(JSR310Migration.dateTimeIsAfterEpochMillis(zonedDateTime, before))
        .isTrue();

    Assertions.assertThat(dateTimeIsAfterEpochMillisOld(dateTime, after)).isFalse();
    Assertions.assertThat(JSR310Migration.dateTimeIsAfterEpochMillis(zonedDateTime, after))
        .isFalse();
  }

  @Test
  public void newDateTimeCtorWithISO8601Str() {
    DateTime dateTime = newDateTimeCtorWithISO8601StrOld("2018-05-09T17:34:55.000Z");
    ZonedDateTime zonedDateTime =
        JSR310Migration.newDateTimeCtorWithISO8601Str("2018-05-09T17:34:55.000Z");
    Assertions.assertThat(dateTime.toInstant().getMillis())
        .isEqualTo(zonedDateTime.toInstant().toEpochMilli());

    DateTime dateTimeWithOffset = newDateTimeCtorWithISO8601StrOld("2018-06-08T14:00:00.000-07:00");
    ZonedDateTime zonedDateTimeWithOffset =
        JSR310Migration.newDateTimeCtorWithISO8601Str("2018-06-08T14:00:00.000-07:00");
    Assertions.assertThat(dateTimeWithOffset.toInstant().getMillis())
        .isEqualTo(zonedDateTimeWithOffset.toInstant().toEpochMilli());
  }

  @Test
  public void newDateTimeCtorWithLongAndString() {
    Assertions.assertThat(
            newDateTimeCtorWithLongAndStringOld("2018-05-09T17:34:55.000Z").toInstant().getMillis())
        .isEqualTo(
            JSR310Migration.newDateTimeCtorWithLongAndString("2018-05-09T17:34:55.000Z")
                .toInstant()
                .toEpochMilli());

    Assertions.assertThat(
            newDateTimeCtorWithLongAndStringOld("2018-05-09T17:34:55.000").toInstant().getMillis())
        .isEqualTo(
            JSR310Migration.newDateTimeCtorWithLongAndString("2018-05-09T17:34:55.000")
                .toInstant()
                .toEpochMilli());

    Assertions.assertThat(
            newDateTimeCtorWithLongAndStringOld(1594339100000L).toInstant().getMillis())
        .isEqualTo(
            JSR310Migration.newDateTimeCtorWithLongAndString(1594339100000L)
                .toInstant()
                .toEpochMilli());

    Assertions.assertThat(newDateTimeCtorWithLongAndStringOld("2018-05-09").toInstant().getMillis())
        .isEqualTo(
            JSR310Migration.newDateTimeCtorWithLongAndString("2018-05-09")
                .toInstant()
                .toEpochMilli());

    Assert.assertThrows(
        IllegalStateException.class,
        () -> JSR310Migration.newDateTimeCtorWithLongAndString(new Date()));
  }

  @Test
  public void dateTimeGetMillisOfSecond() {
    DateTime start = DATE_TIME_WITH_MILLIS;
    ZonedDateTime zstart = ZONED_DATE_TIME_WITH_MILLIS;
    Assertions.assertThat(dateTimeGetMillisOfSecondOld(start))
        .isEqualTo(JSR310Migration.dateTimeGetMillisOfSecond(zstart));
  }

  @Test
  public void dateTimeWithMillisOfSeconds() {
    Assertions.assertThat(dateTimeWithMillisOfSecondsOld(dateTime, 963).toInstant().getMillis())
        .isEqualTo(
            JSR310Migration.dateTimeWithMillisOfSeconds(zonedDateTime, 963)
                .toInstant()
                .toEpochMilli());
  }

  /**
   * This test might be brittle but cannot reproduce.
   *
   * <p>Saw a failure with: "expected: 1702591002127L but was: 1702591002126L"
   */
  @Test
  public void newDateTimeCtorWithDate() {
    Date date = new Date();
    Assertions.assertThat(newDateTimeCtorWithDateOld(date).toInstant().getMillis())
        .isEqualTo(JSR310Migration.newDateTimeCtorWithDate(date).toInstant().toEpochMilli());

    Assertions.assertThat(newDateTimeCtorWithDateOld(null).toInstant().getMillis())
        .isEqualTo(JSR310Migration.newDateTimeCtorWithDate(null).toInstant().toEpochMilli());
  }

  @Test
  public void dateTimeWithLocalTime() {
    Assertions.assertThat(
            dateTimeWithLocalTimeOld(dateTime, new LocalTime(10, 15)).toInstant().getMillis())
        .isEqualTo(
            JSR310Migration.dateTimeWithLocalTime(zonedDateTime, java.time.LocalTime.of(10, 15))
                .toInstant()
                .toEpochMilli());
  }

  @Test
  public void newLocalTimeWithString() {
    String source = "12:34";
    LocalTime joda = newLocalTimeWithStringOld(source);
    java.time.LocalTime jsr = JSR310Migration.newLocalTimeWithString(source);
    Assertions.assertThat(joda.getMillisOfSecond() * 1000).isEqualTo(jsr.getNano());
  }

  @Test
  public void newLocalTimeWithHMS() {
    LocalTime joda = newLocalTimeWithHMSOld(10, 20, 30);
    java.time.LocalTime jsr = JSR310Migration.newLocalTimeWithHMS(10, 20, 30);
    Assertions.assertThat(joda.getMillisOfSecond() * 1000).isEqualTo(jsr.getNano());
  }

  @Test
  public void dateTimeGetDayOfWeek() {
    Assertions.assertThat(dateTimeGetDayOfWeekOld(dateTime))
        .isEqualTo(JSR310Migration.dateTimeGetDayOfWeek(zonedDateTime));
  }

  @Test
  public void dateTimeWithDayOfWeek() {
    Assertions.assertThat(dateTimeWithDayOfWeekOld(dateTime, 4).toInstant().getMillis())
        .isEqualTo(
            JSR310Migration.dateTimeWithDayOfWeek(zonedDateTime, 4).toInstant().toEpochMilli());
  }

  @Test
  public void newPeriodCtorWithLong() {
    Assertions.assertThat(newPeriodCtorWithLongOld(100_000L).toStandardDuration().getMillis())
        .isEqualTo(
            JSR310Migration.newPeriodCtorWithLong(100_000L).getDuration().toNanos() / 1000_000L);
  }

  @Test
  public void newPeriodCtorWithHMS() {
    Assertions.assertThat(
            newPeriodCtorWithHMSMOld(10, 50, 30, 987).toStandardDuration().getMillis())
        .isEqualTo(
            JSR310Migration.newPeriodCtorWithHMSM(10, 50, 30, 987).getDuration().toNanos()
                / 1000_000L);
  }

  @Test
  public void newDateTimeCtorWithStringAndDateTimeZone() {
    String zoneId = "America/Los_Angeles";
    String dateTimeStr = "2020-07-10T00:00:00-04:00";

    DateTime dateTime1 =
        newDateTimeCtorWithStringAndDateTimeZoneOld(dateTimeStr, DateTimeZone.forID(zoneId));

    ZonedDateTime dateTime2 =
        JSR310Migration.newDateTimeCtorWithStringAndDateTimeZone(dateTimeStr, ZoneId.of(zoneId));

    Assertions.assertThat(dateTime1.getMillis()).isEqualTo(dateTime2.toInstant().toEpochMilli());
  }

  @Test
  public void newDateTimeCtorWithDateTimeZone() {
    String zoneId = "America/Los_Angeles";
    // can't compare time, just look at the zone
    Assertions.assertThat(
            newDateTimeCtorWithDateTimeZoneOld(DateTimeZone.forID(zoneId)).getZone().getID())
        .isEqualTo(
            JSR310Migration.newDateTimeCtorWithDateTimeZone(ZoneId.of(zoneId)).getZone().getId());
  }

  @Test
  public void dateTimeZoneForId() {
    String id = "PST8PDT";
    DateTimeZone dateTimeZone = dateTimeZoneForIdOld(id);
    ZoneId zoneId = JSR310Migration.dateTimeZoneForId(id);
    Assertions.assertThat(dateTimeZone.getID()).isEqualTo(zoneId.getId());
  }

  @Test
  public void dateTimeWith0MillisAsMillis() {
    DateTime withMillis = DATE_TIME_WITH_MILLIS;
    ZonedDateTime zWithMillis = ZONED_DATE_TIME_WITH_MILLIS;
    Assertions.assertThat(dateTimeWith0MillisAsMillisOld(withMillis))
        .isEqualTo(JSR310Migration.dateTimeWith0MillisAsMillis(zWithMillis));
  }

  @Test
  public void dateTimeNowInUTC() {
    Assertions.assertThat(dateTimeNowInUTCOld())
        .isBetween(
            Instant.now().toDateTime().minusMillis(100),
            Instant.now().toDateTime().plusMillis(100));

    Assertions.assertThat(JSR310Migration.dateTimeNowInUTC())
        .isBetween(
            ZonedDateTime.now().minus(100, ChronoUnit.MILLIS),
            ZonedDateTime.now().plus(100, ChronoUnit.MILLIS));

    Assertions.assertThat(dateTimeNowInUTCOld().getZone()).isEqualTo(DateTimeZone.UTC);
    // not it is not equal to ZoneId.off("UTC")
    Assertions.assertThat(JSR310Migration.dateTimeNowInUTC().getZone()).isEqualTo(ZoneOffset.UTC);
  }

  @Test
  public void dateTimeOfEpochSecond() {
    int epochSecond = 1597017613;
    Assertions.assertThat(dateTimeOfEpochSecondOld(epochSecond).toInstant().getMillis())
        .isEqualTo(JSR310Migration.dateTimeOfEpochSecond(epochSecond).toInstant().toEpochMilli());
  }

  /**
   * Must be careful, basically anywhere we compare dates it must be either in the same timezone or
   * using instant comparison instead.
   *
   * <p>Some tests failed (when run locally) because of that but 1) it is not obvious to spot all
   * the usages. 2) when test are run in UTC, it hides the issue.
   */
  @Test
  public void zonedDateTimeEquals() {
    ZonedDateTime utc = ZonedDateTime.of(2020, 7, 10, 0, 0, 12, 345000000, ZoneOffset.UTC);
    ZonedDateTime pst = utc.withZoneSameInstant(ZoneId.of("America/Los_Angeles"));
    Assertions.assertThat(utc.isEqual(pst)).isTrue();

    // Needs to pay close attention to not use "equals()" but "isEqualsTo"
    Assertions.assertThat(utc.equals(pst)).isFalse();

    // Be careful AssertJ "isEqualTo" does not use ZonedDateTime.equals()
    Assertions.assertThat(utc).isEqualTo(pst);
  }

  public static DateTime newDateTimeCtorOld(
      int year, int monthOfYear, int dayOfMonth, int hourOfDay, int minuteOfHour) {
    return new DateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour);
  }

  public static DateTime dateTimeNowOld() {
    return DateTime.now();
  }

  public static long getMillisOld(DateTime dateTime) {
    return dateTime.getMillis();
  }

  public static String toWordBaseDurationOld(DateTime start, DateTime end) {
    Period period = new Period(start, end);
    return PeriodFormat.getDefault().print(period.toPeriod());
  }

  public static String toWordBaseDurationOld(long start, long end) {
    Period period = new Period(start, end);
    return PeriodFormat.getDefault().print(period.toPeriod());
  }

  public static Date dateTimeToDateOld(DateTime dateTime) {
    return dateTime.toDate();
  }

  public static boolean dateTimeIsAfterEpochMillisOld(DateTime dateTime, long afterMillis) {
    return dateTime.isAfter(afterMillis);
  }

  public static DateTime newDateTimeCtorWithISO8601StrOld(String str) {
    // from doc: the String formats are described by ISODateTimeFormat.dateTimeParser().
    return new DateTime(str);
  }

  public static DateTime newDateTimeCtorWithLongAndStringOld(Object instant) {
    return new DateTime(instant);
  }

  public static int dateTimeGetMillisOfSecondOld(DateTime dateTime) {
    return dateTime.getMillisOfSecond();
  }

  public static DateTime dateTimeWithMillisOfSecondsOld(DateTime dateTime, int millis) {
    return dateTime.withMillisOfSecond(millis);
  }

  public static DateTime newDateTimeCtorWithDateOld(Date date) {
    return new DateTime(date);
  }

  public static DateTime newDateTimeCtorWithStringAndDateTimeZoneOld(
      String str, DateTimeZone dateTimeZone) {
    return new DateTime(str, dateTimeZone);
  }

  public static DateTime newDateTimeCtorWithDateTimeZoneOld(DateTimeZone dateTimeZone) {
    return new DateTime(dateTimeZone);
  }

  public static DateTime dateTimeWithLocalTimeOld(DateTime dateTime, LocalTime localTime) {
    return dateTime.withTime(localTime);
  }

  public static LocalTime newLocalTimeWithStringOld(String source) {
    return new LocalTime(source);
  }

  public static LocalTime newLocalTimeWithHMSOld(
      int hourOfDay, int minuteOfHour, int secondOfMinute) {
    return new LocalTime(hourOfDay, minuteOfHour, secondOfMinute);
  }

  public static Integer dateTimeGetDayOfWeekOld(DateTime dateTime) {
    return dateTime.getDayOfWeek();
  }

  public static DateTime dateTimeWithDayOfWeekOld(DateTime dateTime, int dayOfWeek) {
    return dateTime.withDayOfWeek(dayOfWeek);
  }

  public static Period newPeriodCtorWithLongOld(long value) {
    return new Period(value);
  }

  public static Period newPeriodCtorWithHMSMOld(int hours, int minutes, int seconds, int millis) {
    return new Period(hours, minutes, seconds, millis);
  }

  public static DateTimeZone dateTimeZoneForIdOld(String id) {
    return DateTimeZone.forID(id);
  }

  public static void junitAssertEqualsOld(DateTime dateTime1, DateTime dateTime2) {
    Assert.assertEquals(dateTime1, dateTime2);
  }

  public static long dateTimeWith0MillisAsMillisOld(DateTime dateTime) {
    return dateTime.withMillisOfSecond(0).getMillis();
  }

  public static DateTime dateTimeNowInUTCOld() {
    return DateTime.now(DateTimeZone.UTC);
  }

  public static DateTime dateTimeOfEpochSecondOld(int epochSecond) {
    return Instant.ofEpochSecond(epochSecond).toDateTime();
  }

  public static Date dateTimePlusAsDateOld(DateTime dateTime, long millis) {
    return dateTime.plus(millis).toDate();
  }
}
