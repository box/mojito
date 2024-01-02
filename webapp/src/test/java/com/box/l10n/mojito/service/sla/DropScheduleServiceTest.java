package com.box.l10n.mojito.service.sla;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.test.JSR310MigrationForTesting;
import com.box.l10n.mojito.utils.DateTimeUtils;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/** @author jeanaurambault */
@RunWith(MockitoJUnitRunner.class)
public class DropScheduleServiceTest {

  @InjectMocks DropScheduleService dropSchedule;

  @Mock DateTimeUtils dateTimeUtils;

  @Spy DropScheduleConfig dropScheduleConfig;

  ZoneId dateTimeZone = JSR310Migration.dateTimeZoneForId("PST8PDT");

  @Test
  public void testGetLastDropCreatedDate() {
    ZonedDateTime now =
        JSR310Migration.newDateTimeCtorWithStringAndDateTimeZone(
            "2018-06-08T14:00:00.000-07:00", dateTimeZone);
    doReturn(now).when(dateTimeUtils).now(dateTimeZone);
    ZonedDateTime expResult =
        JSR310Migration.newDateTimeCtorWithStringAndDateTimeZone(
            "2018-06-06T20:00:00.000-07:00", dateTimeZone);
    ZonedDateTime result = dropSchedule.getLastDropCreatedDate();
    JSR310MigrationForTesting.junitAssertEquals(expResult, result);
  }

  @Test
  public void testGetLastDropCreatedDatePreviousWeek() {
    ZonedDateTime now =
        JSR310Migration.newDateTimeCtorWithStringAndDateTimeZone(
            "2018-06-05T14:00:00.000-07:00", dateTimeZone);
    doReturn(now).when(dateTimeUtils).now(dateTimeZone);
    ZonedDateTime expResult =
        JSR310Migration.newDateTimeCtorWithStringAndDateTimeZone(
            "2018-06-01T20:00:00.000-07:00", dateTimeZone);
    ZonedDateTime result = dropSchedule.getLastDropCreatedDate();
    JSR310MigrationForTesting.junitAssertEquals(expResult, result);
  }

  @Test
  public void testGetLastDropDueDateDuringWeekend() {
    ZonedDateTime before =
        JSR310Migration.newDateTimeCtorWithStringAndDateTimeZone(
            "2018-06-09T21:00:00.000-07:00", dateTimeZone);
    JSR310MigrationForTesting.junitAssertEqualsDateTime(
        "2018-06-08T14:00:00.000-07:00", dropSchedule.getLastDropDueDate(before).toString());
  }

  @Test
  public void testGetLastDropDueDateExactSameTime() {
    ZonedDateTime before =
        JSR310Migration.newDateTimeCtorWithStringAndDateTimeZone(
            "2018-06-08T14:00:00.000-07:00", dateTimeZone);
    JSR310MigrationForTesting.junitAssertEqualsDateTime(
        "2018-06-08T14:00:00.000-07:00", dropSchedule.getLastDropDueDate(before).toString());
  }

  @Test
  public void testGetLastDropDueDateSameDayBeforeDropTime() {
    ZonedDateTime before =
        JSR310Migration.newDateTimeCtorWithStringAndDateTimeZone(
            "2018-06-08T11:00:00.000-07:00", dateTimeZone);
    JSR310MigrationForTesting.junitAssertEqualsDateTime(
        "2018-06-07T14:00:00.000-07:00", dropSchedule.getLastDropDueDate(before).toString());
  }

  @Test
  public void testGetLastDropDueDateSameDayAfterDropTime() {
    ZonedDateTime before =
        JSR310Migration.newDateTimeCtorWithStringAndDateTimeZone(
            "2018-06-08T16:00:00.000-07:00", dateTimeZone);
    JSR310MigrationForTesting.junitAssertEqualsDateTime(
        "2018-06-08T14:00:00.000-07:00", dropSchedule.getLastDropDueDate(before).toString());
  }

  @Test
  public void testGetLastDropDueDateEmptyWorkingDays() {
    doReturn(Arrays.asList()).when(dropScheduleConfig).getDueDays();
    ZonedDateTime before =
        JSR310Migration.newDateTimeCtorWithStringAndDateTimeZone(
            "2018-06-08T16:00:00.000-07:00", dateTimeZone);
    assertNull(dropSchedule.getLastDropDueDate(before));
  }

  @Test
  public void testGetLastDropDueDatePreviousWeek() {
    doReturn(Arrays.asList(5)).when(dropScheduleConfig).getDueDays();
    ZonedDateTime before =
        JSR310Migration.newDateTimeCtorWithStringAndDateTimeZone(
            "2018-06-06T16:00:00.000-07:00", dateTimeZone);
    JSR310MigrationForTesting.junitAssertEqualsDateTime(
        "2018-06-01T14:00:00.000-07:00", dropSchedule.getLastDropDueDate(before).toString());
  }

  @Test
  public void testGetLastDropDueDateOneWeekAgo() {
    doReturn(Arrays.asList(5)).when(dropScheduleConfig).getDueDays();
    ZonedDateTime before =
        JSR310Migration.newDateTimeCtorWithStringAndDateTimeZone(
            "2018-06-08T11:00:00.000-07:00", dateTimeZone);
    JSR310MigrationForTesting.junitAssertEqualsDateTime(
        "2018-06-01T14:00:00.000-07:00", dropSchedule.getLastDropDueDate(before).toString());
  }

  @Test
  public void testGetDropCreatedDate() {
    ZonedDateTime before =
        JSR310Migration.newDateTimeCtorWithStringAndDateTimeZone(
            "2018-06-08T14:00:00.000-07:00", dateTimeZone);
    JSR310MigrationForTesting.junitAssertEqualsDateTime(
        "2018-06-06T20:00:00.000-07:00", dropSchedule.getDropCreatedDate(before).toString());
  }

  @Test
  public void testGetDropCreatedDatePreviousWeek() {
    ZonedDateTime before =
        JSR310Migration.newDateTimeCtorWithStringAndDateTimeZone(
            "2018-06-05T14:00:00.000-07:00", dateTimeZone);
    JSR310MigrationForTesting.junitAssertEqualsDateTime(
        "2018-06-01T20:00:00.000-07:00", dropSchedule.getDropCreatedDate(before).toString());
  }
}
