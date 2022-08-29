package com.box.l10n.mojito.service.sla;

import java.util.Arrays;
import java.util.List;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** @author jeanaurambault */
@Configuration
@ConfigurationProperties(prefix = "l10n.drop-schedule")
public class DropScheduleConfig {

  DateTimeZone timezone = DateTimeZone.forID("PST8PDT");

  List<Integer> createdDays = Arrays.asList(1, 2, 3, 4, 5);

  List<Integer> dueDays = Arrays.asList(3, 4, 5, 1, 2);

  LocalTime createdLocalTime = new LocalTime("20:00");

  LocalTime dueLocalTime = new LocalTime("14:00");

  public DateTimeZone getTimezone() {
    return timezone;
  }

  public void setTimezone(DateTimeZone timezone) {
    this.timezone = timezone;
  }

  public List<Integer> getCreatedDays() {
    return createdDays;
  }

  public void setCreatedDays(List<Integer> createdDays) {
    this.createdDays = createdDays;
  }

  public List<Integer> getDueDays() {
    return dueDays;
  }

  public void setDueDays(List<Integer> dueDays) {
    this.dueDays = dueDays;
  }

  public LocalTime getCreatedLocalTime() {
    return createdLocalTime;
  }

  public void setCreatedLocalTime(LocalTime createdLocalTime) {
    this.createdLocalTime = createdLocalTime;
  }

  public LocalTime getDueLocalTime() {
    return dueLocalTime;
  }

  public void setDueLocalTime(LocalTime dueLocalTime) {
    this.dueLocalTime = dueLocalTime;
  }
}
