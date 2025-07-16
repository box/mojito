package com.box.l10n.mojito.service.delta;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.push-pull-run.cleanup-job.cleanup-per-asset")
public class CleanPushPullPerAssetConfigurationProperties {
  private List<DayRange> dayRanges = new ArrayList<>();

  public List<DayRange> getDayRanges() {
    return dayRanges;
  }

  public void setDayRanges(List<DayRange> dayRanges) {
    this.dayRanges = dayRanges;
  }

  public record DayRange(int startDay, int endDay) {}
}
