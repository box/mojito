package com.box.l10n.mojito.service.sla;

import com.box.l10n.mojito.utils.DateTimeUtils;
import java.util.Arrays;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doReturn;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

/**
 *
 * @author jeanaurambault
 */
@RunWith(MockitoJUnitRunner.class)
public class DropScheduleServiceTest {

    @InjectMocks
    DropScheduleService dropSchedule;

    @Mock
    DateTimeUtils dateTimeUtils;

    @Spy
    DropScheduleConfig dropScheduleConfig;

    DateTimeZone dateTimeZone = DateTimeZone.forID("PST8PDT");

    @Test
    public void testGetLastDropCreatedDate() {
        DateTime now = new DateTime("2018-06-08T14:00:00.000-07:00", dateTimeZone);
        doReturn(now).when(dateTimeUtils).now(dateTimeZone);
        DateTime expResult = new DateTime("2018-06-06T20:00:00.000-07:00", dateTimeZone);
        DateTime result = dropSchedule.getLastDropCreatedDate();
        assertEquals(expResult, result);
    }

    @Test
    public void testGetLastDropCreatedDatePreviousWeek() {
        DateTime now = new DateTime("2018-06-05T14:00:00.000-07:00", dateTimeZone);
        doReturn(now).when(dateTimeUtils).now(dateTimeZone);
        DateTime expResult = new DateTime("2018-06-01T20:00:00.000-07:00", dateTimeZone);
        DateTime result = dropSchedule.getLastDropCreatedDate();
        assertEquals(expResult, result);
    }

    @Test
    public void testGetLastDropDueDateDuringWeekend() {
        DateTime before = new DateTime("2018-06-09T21:00:00.000-07:00", dateTimeZone);
        assertEquals("2018-06-08T14:00:00.000-07:00", dropSchedule.getLastDropDueDate(before).toString());
    }

    @Test
    public void testGetLastDropDueDateExactSameTime() {
        DateTime before = new DateTime("2018-06-08T14:00:00.000-07:00", dateTimeZone);
        assertEquals("2018-06-08T14:00:00.000-07:00", dropSchedule.getLastDropDueDate(before).toString());
    }

    @Test
    public void testGetLastDropDueDateSameDayBeforeDropTime() {
        DateTime before = new DateTime("2018-06-08T11:00:00.000-07:00", dateTimeZone);
        assertEquals("2018-06-07T14:00:00.000-07:00", dropSchedule.getLastDropDueDate(before).toString());
    }

    @Test
    public void testGetLastDropDueDateSameDayAfterDropTime() {
        DateTime before = new DateTime("2018-06-08T16:00:00.000-07:00", dateTimeZone);
        assertEquals("2018-06-08T14:00:00.000-07:00", dropSchedule.getLastDropDueDate(before).toString());
    }

    @Test
    public void testGetLastDropDueDateEmptyWorkingDays() {
        doReturn(Arrays.asList()).when(dropScheduleConfig).getDueDays();
        DateTime before = new DateTime("2018-06-08T16:00:00.000-07:00", dateTimeZone);
        assertNull(dropSchedule.getLastDropDueDate(before));
    }

    @Test
    public void testGetLastDropDueDatePreviousWeek() {
        doReturn(Arrays.asList(5)).when(dropScheduleConfig).getDueDays();
        DateTime before = new DateTime("2018-06-06T16:00:00.000-07:00", dateTimeZone);
        assertEquals("2018-06-01T14:00:00.000-07:00", dropSchedule.getLastDropDueDate(before).toString());
    }

    @Test
    public void testGetLastDropDueDateOneWeekAgo() {
        doReturn(Arrays.asList(5)).when(dropScheduleConfig).getDueDays();
        DateTime before = new DateTime("2018-06-08T11:00:00.000-07:00", dateTimeZone);
        assertEquals("2018-06-01T14:00:00.000-07:00", dropSchedule.getLastDropDueDate(before).toString());
    }

    @Test
    public void testGetDropCreatedDate() {
        DateTime before = new DateTime("2018-06-08T14:00:00.000-07:00", dateTimeZone);
        assertEquals("2018-06-06T20:00:00.000-07:00", dropSchedule.getDropCreatedDate(before).toString());
    }

    @Test
    public void testGetDropCreatedDatePreviousWeek() {
        DateTime before = new DateTime("2018-06-05T14:00:00.000-07:00", dateTimeZone);
        assertEquals("2018-06-01T20:00:00.000-07:00", dropSchedule.getDropCreatedDate(before).toString());
    }
   
}
