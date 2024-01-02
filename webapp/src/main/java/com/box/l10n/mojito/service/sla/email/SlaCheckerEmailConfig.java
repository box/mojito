package com.box.l10n.mojito.service.sla.email;

import com.box.l10n.mojito.JSR310Migration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.threeten.extra.PeriodDuration;

/** @author jeanaurambault */
@Configuration
@ConfigurationProperties(prefix = "l10n.sla-checker.email")
public class SlaCheckerEmailConfig {

  String from;

  String[] to;

  /**
   * Period to wait before re-sending an email during an incident.
   *
   * <p>The config property is a value in milliseconds.
   */
  PeriodDuration periodBetweenEmail = JSR310Migration.newPeriodCtorWithHMSM(1, 0, 0, 0);

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String[] getTo() {
    return to;
  }

  public void setTo(String[] to) {
    this.to = to;
  }

  public PeriodDuration getPeriodBetweenEmail() {
    return periodBetweenEmail;
  }

  public void setPeriodBetweenEmail(PeriodDuration periodBetweenEmail) {
    this.periodBetweenEmail = periodBetweenEmail;
  }
}
