package com.box.l10n.mojito.service.delta;

import static com.box.l10n.mojito.service.delta.CleanPushPullPerAssetConfigurationProperties.DayRange;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.box.l10n.mojito.service.pullrun.PullRunService;
import com.box.l10n.mojito.service.pushrun.PushRunService;
import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PushPullRunCleanupServiceMockTest {
  @Mock PushRunService pushRunServiceMock;

  @Mock PullRunService pullRunServiceMock;

  PushPullRunCleanupServiceTestImpl pushPullRunCleanupService;

  @Captor ArgumentCaptor<ZonedDateTime> startDateTimeCaptor;

  @Captor ArgumentCaptor<ZonedDateTime> endDateTimeCaptor;

  CleanPushPullPerAssetConfigurationProperties configurationProperties;

  AutoCloseable mocks;

  @BeforeEach
  public void setUp() {
    mocks = MockitoAnnotations.openMocks(this);

    this.configurationProperties = new CleanPushPullPerAssetConfigurationProperties();
    DayRange firstDayRange = new DayRange(15, 21);
    DayRange secondDayRange = new DayRange(22, 31);
    this.configurationProperties.setDayRanges(ImmutableList.of(firstDayRange, secondDayRange));
  }

  @Test
  public void testCleanOldPushPullData_Uses2025_7_1AsCurrentDate() {
    LocalDate date = LocalDate.of(2025, 7, 1);
    ZonedDateTime currentDateTime = date.atStartOfDay(ZoneId.systemDefault());
    this.pushPullRunCleanupService =
        new PushPullRunCleanupServiceTestImpl(
            this.pushRunServiceMock,
            this.pullRunServiceMock,
            this.configurationProperties,
            currentDateTime);
    Duration duration = Duration.ofDays(30);

    this.pushPullRunCleanupService.cleanOldPushPullData(duration);

    verify(this.pushRunServiceMock).deleteAllPushEntitiesOlderThan(eq(duration));
    verify(this.pullRunServiceMock).deleteAllPullEntitiesOlderThan(eq(duration));
    verify(this.pushRunServiceMock, times(2))
        .deletePushRunsByAsset(
            this.startDateTimeCaptor.capture(), this.endDateTimeCaptor.capture());

    List<ZonedDateTime> expectedStartDates =
        List.of(
            LocalDate.of(2025, 6, 15).atStartOfDay(ZoneId.systemDefault()),
            LocalDate.of(2025, 6, 22).atStartOfDay(ZoneId.systemDefault()));
    assertEquals(expectedStartDates, startDateTimeCaptor.getAllValues());
    List<ZonedDateTime> expectedEndDates =
        List.of(
            LocalDate.of(2025, 6, 21).atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()),
            LocalDate.of(2025, 6, 30).atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()));
    assertEquals(expectedEndDates, endDateTimeCaptor.getAllValues());
  }

  @Test
  public void testCleanOldPushPullData_Uses2025_7_31AsCurrentDate() {
    LocalDate date = LocalDate.of(2025, 7, 31);
    ZonedDateTime currentDateTime = date.atStartOfDay(ZoneId.systemDefault());
    this.pushPullRunCleanupService =
        new PushPullRunCleanupServiceTestImpl(
            this.pushRunServiceMock,
            this.pullRunServiceMock,
            this.configurationProperties,
            currentDateTime);
    Duration duration = Duration.ofDays(30);

    this.pushPullRunCleanupService.cleanOldPushPullData(duration);

    verify(this.pushRunServiceMock).deleteAllPushEntitiesOlderThan(eq(duration));
    verify(this.pullRunServiceMock).deleteAllPullEntitiesOlderThan(eq(duration));
    verify(this.pushRunServiceMock, times(2))
        .deletePushRunsByAsset(
            this.startDateTimeCaptor.capture(), this.endDateTimeCaptor.capture());

    List<ZonedDateTime> expectedStartDates =
        List.of(
            LocalDate.of(2025, 7, 15).atStartOfDay(ZoneId.systemDefault()),
            LocalDate.of(2025, 7, 22).atStartOfDay(ZoneId.systemDefault()));
    assertEquals(expectedStartDates, startDateTimeCaptor.getAllValues());
    List<ZonedDateTime> expectedEndDates =
        List.of(
            LocalDate.of(2025, 7, 21).atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()),
            currentDateTime);
    assertEquals(expectedEndDates, endDateTimeCaptor.getAllValues());
  }

  @Test
  public void testCleanOldPushPullData_Uses2025_7_15AsCurrentDate() {
    LocalDate date = LocalDate.of(2025, 7, 15);
    ZonedDateTime currentDateTime = date.atStartOfDay(ZoneId.systemDefault());
    this.pushPullRunCleanupService =
        new PushPullRunCleanupServiceTestImpl(
            this.pushRunServiceMock,
            this.pullRunServiceMock,
            this.configurationProperties,
            currentDateTime);
    Duration duration = Duration.ofDays(30);

    this.pushPullRunCleanupService.cleanOldPushPullData(duration);

    verify(this.pushRunServiceMock).deleteAllPushEntitiesOlderThan(eq(duration));
    verify(this.pullRunServiceMock).deleteAllPullEntitiesOlderThan(eq(duration));
    verify(this.pushRunServiceMock, times(2))
        .deletePushRunsByAsset(
            this.startDateTimeCaptor.capture(), this.endDateTimeCaptor.capture());

    List<ZonedDateTime> expectedStartDates =
        List.of(
            LocalDate.of(2025, 6, 15).atStartOfDay(ZoneId.systemDefault()),
            LocalDate.of(2025, 6, 22).atStartOfDay(ZoneId.systemDefault()));
    assertEquals(expectedStartDates, startDateTimeCaptor.getAllValues());
    List<ZonedDateTime> expectedEndDates =
        List.of(
            LocalDate.of(2025, 6, 21).atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()),
            LocalDate.of(2025, 6, 30).atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()));
    assertEquals(expectedEndDates, endDateTimeCaptor.getAllValues());
  }

  @Test
  public void testCleanOldPushPullData_Uses2025_7_22AsCurrentDate() {
    LocalDate date = LocalDate.of(2025, 7, 22);
    ZonedDateTime currentDateTime = date.atStartOfDay(ZoneId.systemDefault());
    this.pushPullRunCleanupService =
        new PushPullRunCleanupServiceTestImpl(
            this.pushRunServiceMock,
            this.pullRunServiceMock,
            this.configurationProperties,
            currentDateTime);
    Duration duration = Duration.ofDays(30);

    this.pushPullRunCleanupService.cleanOldPushPullData(duration);

    verify(this.pushRunServiceMock).deleteAllPushEntitiesOlderThan(eq(duration));
    verify(this.pullRunServiceMock).deleteAllPullEntitiesOlderThan(eq(duration));
    verify(this.pushRunServiceMock, times(2))
        .deletePushRunsByAsset(
            this.startDateTimeCaptor.capture(), this.endDateTimeCaptor.capture());

    List<ZonedDateTime> expectedStartDates =
        List.of(
            LocalDate.of(2025, 6, 22).atStartOfDay(ZoneId.systemDefault()),
            LocalDate.of(2025, 7, 15).atStartOfDay(ZoneId.systemDefault()));
    assertEquals(expectedStartDates, startDateTimeCaptor.getAllValues());
    List<ZonedDateTime> expectedEndDates =
        List.of(
            LocalDate.of(2025, 6, 30).atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()),
            LocalDate.of(2025, 7, 21).atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()));
    assertEquals(expectedEndDates, endDateTimeCaptor.getAllValues());
  }

  @Test
  public void testCleanOldPushPullData_Uses2025_7_18AsCurrentDate() {
    LocalDate date = LocalDate.of(2025, 7, 18);
    ZonedDateTime currentDateTime = date.atStartOfDay(ZoneId.systemDefault());
    this.pushPullRunCleanupService =
        new PushPullRunCleanupServiceTestImpl(
            this.pushRunServiceMock,
            this.pullRunServiceMock,
            this.configurationProperties,
            currentDateTime);
    Duration duration = Duration.ofDays(30);

    this.pushPullRunCleanupService.cleanOldPushPullData(duration);

    verify(this.pushRunServiceMock).deleteAllPushEntitiesOlderThan(eq(duration));
    verify(this.pullRunServiceMock).deleteAllPullEntitiesOlderThan(eq(duration));
    verify(this.pushRunServiceMock, times(3))
        .deletePushRunsByAsset(
            this.startDateTimeCaptor.capture(), this.endDateTimeCaptor.capture());

    List<ZonedDateTime> expectedStartDates =
        List.of(
            LocalDate.of(2025, 6, 18).atStartOfDay(ZoneId.systemDefault()),
            LocalDate.of(2025, 6, 22).atStartOfDay(ZoneId.systemDefault()),
            LocalDate.of(2025, 7, 15).atStartOfDay(ZoneId.systemDefault()));
    assertEquals(expectedStartDates, startDateTimeCaptor.getAllValues());
    List<ZonedDateTime> expectedEndDates =
        List.of(
            LocalDate.of(2025, 6, 21).atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()),
            LocalDate.of(2025, 6, 30).atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()),
            currentDateTime);
    assertEquals(expectedEndDates, endDateTimeCaptor.getAllValues());
  }

  @Test
  public void testCleanOldPushPullData_Uses2025_7_25AsCurrentDate() {
    LocalDate date = LocalDate.of(2025, 7, 25);
    ZonedDateTime currentDateTime = date.atStartOfDay(ZoneId.systemDefault());
    this.pushPullRunCleanupService =
        new PushPullRunCleanupServiceTestImpl(
            this.pushRunServiceMock,
            this.pullRunServiceMock,
            this.configurationProperties,
            currentDateTime);
    Duration duration = Duration.ofDays(30);

    this.pushPullRunCleanupService.cleanOldPushPullData(duration);

    verify(this.pushRunServiceMock).deleteAllPushEntitiesOlderThan(eq(duration));
    verify(this.pullRunServiceMock).deleteAllPullEntitiesOlderThan(eq(duration));
    verify(this.pushRunServiceMock, times(3))
        .deletePushRunsByAsset(
            this.startDateTimeCaptor.capture(), this.endDateTimeCaptor.capture());

    List<ZonedDateTime> expectedStartDates =
        List.of(
            LocalDate.of(2025, 6, 25).atStartOfDay(ZoneId.systemDefault()),
            LocalDate.of(2025, 7, 15).atStartOfDay(ZoneId.systemDefault()),
            LocalDate.of(2025, 7, 22).atStartOfDay(ZoneId.systemDefault()));
    assertEquals(expectedStartDates, startDateTimeCaptor.getAllValues());
    List<ZonedDateTime> expectedEndDates =
        List.of(
            LocalDate.of(2025, 6, 30).atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()),
            LocalDate.of(2025, 7, 21).atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()),
            currentDateTime);
    assertEquals(expectedEndDates, endDateTimeCaptor.getAllValues());
  }

  @Test
  public void testCleanOldPushPullData_Uses60DaysRetentionDuration() {
    LocalDate date = LocalDate.of(2025, 7, 18);
    ZonedDateTime currentDateTime = date.atStartOfDay(ZoneId.systemDefault());
    this.pushPullRunCleanupService =
        new PushPullRunCleanupServiceTestImpl(
            this.pushRunServiceMock,
            this.pullRunServiceMock,
            this.configurationProperties,
            currentDateTime);
    Duration duration = Duration.ofDays(60);

    this.pushPullRunCleanupService.cleanOldPushPullData(duration);

    verify(this.pushRunServiceMock).deleteAllPushEntitiesOlderThan(eq(duration));
    verify(this.pullRunServiceMock).deleteAllPullEntitiesOlderThan(eq(duration));
    verify(this.pushRunServiceMock, times(5))
        .deletePushRunsByAsset(
            this.startDateTimeCaptor.capture(), this.endDateTimeCaptor.capture());

    List<ZonedDateTime> expectedStartDates =
        List.of(
            LocalDate.of(2025, 5, 19).atStartOfDay(ZoneId.systemDefault()),
            LocalDate.of(2025, 5, 22).atStartOfDay(ZoneId.systemDefault()),
            LocalDate.of(2025, 6, 15).atStartOfDay(ZoneId.systemDefault()),
            LocalDate.of(2025, 6, 22).atStartOfDay(ZoneId.systemDefault()),
            LocalDate.of(2025, 7, 15).atStartOfDay(ZoneId.systemDefault()));
    assertEquals(expectedStartDates, startDateTimeCaptor.getAllValues());
    List<ZonedDateTime> expectedEndDates =
        List.of(
            LocalDate.of(2025, 5, 21).atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()),
            LocalDate.of(2025, 5, 31).atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()),
            LocalDate.of(2025, 6, 21).atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()),
            LocalDate.of(2025, 6, 30).atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()),
            currentDateTime);
    assertEquals(expectedEndDates, endDateTimeCaptor.getAllValues());
  }

  @Test
  public void testCleanOldPushPullData_DoesNotRunDeletePushRunsByAsset() {
    LocalDate date = LocalDate.of(2025, 7, 1);
    ZonedDateTime currentDateTime = date.atStartOfDay(ZoneId.systemDefault());
    CleanPushPullPerAssetConfigurationProperties configurationProperties =
        new CleanPushPullPerAssetConfigurationProperties();
    this.pushPullRunCleanupService =
        new PushPullRunCleanupServiceTestImpl(
            this.pushRunServiceMock,
            this.pullRunServiceMock,
            configurationProperties,
            currentDateTime);
    Duration duration = Duration.ofDays(30);

    this.pushPullRunCleanupService.cleanOldPushPullData(duration);

    verify(this.pushRunServiceMock, times(0))
        .deletePushRunsByAsset(any(ZonedDateTime.class), any(ZonedDateTime.class));
    verify(this.pullRunServiceMock, times(0))
        .deletePullRunsByAsset(any(ZonedDateTime.class), any(ZonedDateTime.class));
  }

  @AfterEach
  void tearDown() throws Exception {
    mocks.close();
  }

  private static class PushPullRunCleanupServiceTestImpl extends PushPullRunCleanupService {
    private final ZonedDateTime currentDateTime;

    public PushPullRunCleanupServiceTestImpl(
        PushRunService pushRunService,
        PullRunService pullRunService,
        CleanPushPullPerAssetConfigurationProperties configurationProperties,
        ZonedDateTime currentDateTime) {
      super(pushRunService, pullRunService, configurationProperties);
      this.currentDateTime = currentDateTime;
    }

    @Override
    public ZonedDateTime getCurrentDateTime() {
      return currentDateTime;
    }
  }
}
