package com.box.l10n.mojito;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;
import org.threeten.extra.AmountFormats;
import org.threeten.extra.PeriodDuration;

public class JSR310Migration {

  public static ZonedDateTime newDateTimeCtor(
      int year, int monthOfYear, int dayOfMonth, int hourOfDay, int minuteOfHour) {
    return ZonedDateTime.of(
        year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, 0, 0, ZoneId.systemDefault());
  }

  public static ZonedDateTime newDateTimeEmptyCtor() {
    return ZonedDateTime.now();
  }

  /** this must have a zone, but that's the case for this usage ... */
  public static ZonedDateTime newDateTimeCtorWithISO8601Str(String str) {
    return ZonedDateTime.parse(str);
  }

  public static ZonedDateTime newDateTimeCtorWithLongAndString(Object instant) {
    ZonedDateTime zonedDateTime;
    if (instant instanceof Long) {
      zonedDateTime = newDateTimeCtorWithEpochMilli((long) instant);
    } else if (instant instanceof String) {
      String instantAsStr = (String) instant;
      try {
        zonedDateTime = ZonedDateTime.parse(instantAsStr);
      } catch (DateTimeParseException dateTimeParseException) {
        try {
          zonedDateTime = LocalDateTime.parse(instantAsStr).atZone(ZoneId.systemDefault());
        } catch (DateTimeParseException dateTimeParseException2) {
          zonedDateTime = LocalDate.parse(instantAsStr).atStartOfDay(ZoneId.systemDefault());
        }
      }
    } else {
      throw new IllegalStateException();
    }

    return zonedDateTime;
  }

  public static ZonedDateTime newDateTimeCtorWithEpochMilli(long value) {
    return Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault());
  }

  public static ZonedDateTime newDateTimeCtorAtEpoch() {
    return newDateTimeCtorWithEpochMilli(Instant.EPOCH.toEpochMilli());
  }

  public static ZonedDateTime newDateTimeCtorWithDate(Date date) {
    return date == null
        ? ZonedDateTime.now()
        : ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
  }

  public static ZonedDateTime newDateTimeCtorWithStringAndDateTimeZone(String str, ZoneId zoneId) {
    return newDateTimeCtorWithISO8601Str(str).withZoneSameInstant(zoneId);
  }

  public static ZonedDateTime newDateTimeCtorWithDateTimeZone(ZoneId zoneId) {
    return ZonedDateTime.now(zoneId);
  }

  public static ZonedDateTime dateTimeNow() {
    return ZonedDateTime.now();
  }

  public static long getMillis(ZonedDateTime zonedDateTime) {
    return zonedDateTime.toInstant().toEpochMilli();
  }

  public static long dateTimeWith0MillisAsMillis(ZonedDateTime dateTime) {
    return dateTime.withNano(0).toInstant().toEpochMilli();
  }

  public static String toWordBasedDuration(ZonedDateTime start, ZonedDateTime end) {
    PeriodDuration between = PeriodDuration.between(start, end);
    return AmountFormats.wordBased(between.getPeriod(), between.getDuration(), Locale.getDefault());
  }

  public static String toWordBasedDuration(long start, long end) {
    return toWordBasedDuration(
        Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()),
        Instant.ofEpochMilli(end).atZone(ZoneId.systemDefault()));
  }

  public static Date dateTimeToDate(ZonedDateTime zonedDateTime) {
    return Date.from(zonedDateTime.toInstant());
  }

  public static boolean dateTimeIsAfterEpochMillis(ZonedDateTime dateTime, long after) {
    return dateTime.toInstant().isAfter(Instant.ofEpochMilli(after));
  }

  public static int dateTimeGetMillisOfSecond(ZonedDateTime dateTime) {
    return dateTime.toInstant().get(ChronoField.MILLI_OF_SECOND);
  }

  public static ZonedDateTime dateTimeWithMillisOfSeconds(ZonedDateTime dateTime, int millis) {
    return dateTime.withNano(Duration.ofMillis(millis).getNano());
  }

  public static ZonedDateTime dateTimeWithLocalTime(ZonedDateTime dateTime, LocalTime localTime) {
    return dateTime.with(localTime);
  }

  public static LocalTime newLocalTimeWithString(String source) {
    return source == null ? LocalTime.now() : LocalTime.parse(source);
  }

  public static LocalTime newLocalTimeWithHMS(int hourOfDay, int minuteOfHour, int secondOfMinute) {
    return LocalTime.of(hourOfDay, minuteOfHour, secondOfMinute);
  }

  public static Integer dateTimeGetDayOfWeek(ZonedDateTime dateTime) {
    return dateTime.getDayOfWeek().getValue();
  }

  public static ZonedDateTime dateTimeWithDayOfWeek(ZonedDateTime dateTime, int dayOfWeek) {
    return dateTime.with(DayOfWeek.of(dayOfWeek));
  }

  /**
   * This is used by Spring converter, which should be only used by the drop schedule check ... not
   * critical
   */
  public static PeriodDuration newPeriodCtorWithLong(long value) {
    return PeriodDuration.of(Duration.ofMillis(value));
  }

  public static PeriodDuration newPeriodCtorWithHMSM(
      int hours, int minutes, int seconds, int millis) {
    return PeriodDuration.of(
        Period.ZERO,
        Duration.ofHours(hours).plusMinutes(minutes).plusSeconds(seconds).plusMillis(millis));
  }

  public static ZoneId dateTimeZoneForId(String id) {
    return ZoneId.of(id);
  }

  public static ZonedDateTime dateTimeNowInUTC() {
    return ZonedDateTime.now(ZoneOffset.UTC);
  }

  public static ZonedDateTime dateTimeOfEpochSecond(int epochSecond) {
    return Instant.ofEpochSecond(epochSecond).atZone(ZoneId.systemDefault());
  }

  public static Date dateTimePlusAsDate(ZonedDateTime dateTime, long millis) {
    return Date.from(dateTime.plus(millis, ChronoUnit.MILLIS).toInstant());
  }

  /** Millisecond precision. Mysql tables are currently with second precision and HSQL */
  static DateTimeFormatter SQL_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

  public static String toRawSQL(ZonedDateTime zonedDateTime) {
    // will depend on hsql vs mysql
    return zonedDateTime.format(SQL_FORMATTER);
  }
}
