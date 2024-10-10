package com.box.l10n.mojito.pagerduty;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Map;

public class PagerDutyPayload {
  private String summary;
  private String source;
  private Severity severity;
  private Map<String, String> customDetails;

  public PagerDutyPayload(String summary, String source, Severity severity) {
    this.summary = summary;
    this.source = source;
    this.severity = severity;
  }

  public PagerDutyPayload(
      String summary, String source, Severity severity, Map<String, String> customDetails) {
    this.summary = summary;
    this.source = source;
    this.severity = severity;
    this.customDetails = customDetails;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public Severity getSeverity() {
    return severity;
  }

  public void setSeverity(Severity severity) {
    this.severity = severity;
  }

  public Map<String, String> getCustomDetails() {
    return customDetails;
  }

  public void setCustomDetails(Map<String, String> customDetails) {
    this.customDetails = customDetails;
  }

  public enum Severity {
    CRITICAL,
    ERROR,
    WARNING,
    INFO,
    UNKNOWN;

    // The severity must be lowercase, the object mapper will call this method because it's
    // annotated with @JsonValue and will use the return value
    @JsonValue
    public String toLowerCase() {
      return this.name().toLowerCase();
    }
  }
}
