package com.box.l10n.mojito.service.sla.email;

import org.joda.time.Period;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author jeanaurambault
 */
@Configuration
@ConfigurationProperties(prefix = "l10n.slaChecker.email")
public class SlaCheckerEmailConfig {

    String from;

    String[] to;

    /**
     * Period to wait before re-sending an email during an incident.
     * 
     * The config property is a value in milliseconds. 
     */
    Period periodBetweenEmail = new Period(1, 0, 0, 0);

    String mojitoUrl = "http://localhost:8080/";

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

    public Period getPeriodBetweenEmail() {
        return periodBetweenEmail;
    }

    public void setPeriodBetweenEmail(Period periodBetweenEmail) {
        this.periodBetweenEmail = periodBetweenEmail;
    }

    public String getMojitoUrl() {
        return mojitoUrl;
    }

    public void setMojitoUrl(String mojitoUrl) {
        this.mojitoUrl = mojitoUrl;
    }

}
