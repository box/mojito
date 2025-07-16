package com.box.l10n.mojito.service.delta;

import static com.box.l10n.mojito.service.delta.CleanPushPullPerAssetConfigurationProperties.DayRange;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import com.box.l10n.mojito.service.pullrun.PullRunService;
import com.box.l10n.mojito.service.pushrun.PushRunService;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author garion
 */
@Service
public class PushPullRunCleanupService {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(PushPullRunCleanupService.class);

  private final PushRunService pushRunService;

  private final PullRunService pullRunService;

  private final CleanPushPullPerAssetConfigurationProperties configurationProperties;

  public PushPullRunCleanupService(
      PushRunService pushRunService,
      PullRunService pullRunService,
      CleanPushPullPerAssetConfigurationProperties configurationProperties) {
    this.pushRunService = pushRunService;
    this.pullRunService = pullRunService;
    this.configurationProperties = configurationProperties;
  }

  /**
   * Method added for testing purposes
   *
   * @return Current date time
   */
  protected ZonedDateTime getCurrentDateTime() {
    return ZonedDateTime.now();
  }

  private boolean isEqualOrAfter(ZonedDateTime dateTime1, ZonedDateTime dateTime2) {
    return dateTime1.isEqual(dateTime2) || dateTime1.isAfter(dateTime2);
  }

  private Optional<DateRange> getRange(
      ZonedDateTime validStartDateOfRange,
      ZonedDateTime currentDateTime,
      int startDay,
      int endDay) {
    ZonedDateTime startDateOfRange =
        validStartDateOfRange
            .toLocalDate()
            .withDayOfMonth(Math.min(startDay, validStartDateOfRange.toLocalDate().lengthOfMonth()))
            .atStartOfDay(ZoneId.systemDefault());
    ZonedDateTime endDateOfRange =
        validStartDateOfRange
            .toLocalDate()
            .withDayOfMonth(Math.min(endDay, validStartDateOfRange.toLocalDate().lengthOfMonth()))
            .atTime(LocalTime.MAX)
            .atZone(ZoneId.systemDefault());
    if (this.isEqualOrAfter(startDateOfRange, validStartDateOfRange)
        && startDateOfRange.isBefore(currentDateTime)) {
      return of(
          new DateRange(
              startDateOfRange,
              endDateOfRange.isBefore(currentDateTime) ? endDateOfRange : currentDateTime));
    } else if (endDateOfRange.isAfter(validStartDateOfRange)
        && startDateOfRange.isBefore(currentDateTime)) {
      return of(
          new DateRange(
              validStartDateOfRange,
              endDateOfRange.isBefore(currentDateTime) ? endDateOfRange : currentDateTime));
    }
    return empty();
  }

  private void deletePushAndPullRunsByAsset(Duration retentionDuration) {
    List<DateRange> dateRanges = new ArrayList<>();
    ZonedDateTime currentDateTime = this.getCurrentDateTime();
    ZonedDateTime validStartDateOfRange =
        currentDateTime.minusSeconds((int) retentionDuration.getSeconds());
    while ((validStartDateOfRange.getYear() < currentDateTime.getYear())
        || (validStartDateOfRange.getYear() == currentDateTime.getYear()
            && validStartDateOfRange.getMonthValue() <= currentDateTime.getMonthValue())) {
      for (DayRange dayRange : configurationProperties.getDayRanges()) {
        Optional<DateRange> dateRange =
            this.getRange(
                validStartDateOfRange, currentDateTime, dayRange.startDay(), dayRange.endDay());
        dateRange.ifPresent(dateRanges::add);
      }
      validStartDateOfRange =
          validStartDateOfRange
              .toLocalDate()
              .withDayOfMonth(1)
              .plusMonths(1)
              .atStartOfDay(ZoneId.systemDefault());
    }
    dateRanges.forEach(
        dateRange -> {
          logger.debug(
              "Deleting push and pull runs from {} to {}", dateRange.startDate, dateRange.endDate);
          this.pushRunService.deletePushRunsByAsset(dateRange.startDate, dateRange.endDate);
          this.pullRunService.deletePullRunsByAsset(dateRange.startDate, dateRange.endDate);
        });
  }

  public void cleanOldPushPullData(Duration retentionDuration) {
    this.deletePushAndPullRunsByAsset(retentionDuration);

    pushRunService.deleteAllPushEntitiesOlderThan(retentionDuration);
    pullRunService.deleteAllPullEntitiesOlderThan(retentionDuration);
  }

  private record DateRange(ZonedDateTime startDate, ZonedDateTime endDate) {}
}
