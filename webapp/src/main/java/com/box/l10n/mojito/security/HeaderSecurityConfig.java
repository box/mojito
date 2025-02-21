package com.box.l10n.mojito.security;

import jakarta.annotation.PostConstruct;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HeaderSecurityConfig {

  @Value("${l10n.spring.security.header.user.identifyingHeader:}")
  protected String userIdentifyingHeader;

  @Value("${l10n.spring.security.header.service.identifyingHeader:}")
  protected String serviceIdentifyingHeader;

  @Value("${l10n.spring.security.header.service.identifyingPrefix:}")
  protected String servicePrefix;

  @Value("${l10n.spring.security.header.service.delimiter:/}")
  protected String serviceDelimiter;

  @Value("${l10n.spring.security.header.service.pagerduty.enableFailedAuthIncidents:false}")
  protected boolean isPagerDutyEnabled;

  @Value("${l10n.spring.security.header.service.pagerduty.integration.name:#{null}}")
  protected String pagerDutyIntegrationName;

  @Value("${l10n.spring.security.header.service.forbiddenRegex:#{null}}")
  protected String forbiddenServiceRegex;

  private Pattern forbiddenServicePattern;

  @PostConstruct
  public void init() {
    if (forbiddenServiceRegex != null && !forbiddenServiceRegex.isEmpty()) {
      forbiddenServicePattern = Pattern.compile(forbiddenServiceRegex);
    }
  }

  public boolean isPagerDutyEnabled() {
    return isPagerDutyEnabled;
  }

  public void setPagerDutyEnabled(boolean pagerDutyEnabled) {
    isPagerDutyEnabled = pagerDutyEnabled;
  }

  public String getPagerDutyIntegrationName() {
    return pagerDutyIntegrationName;
  }

  public void setPagerDutyIntegrationName(String pagerDutyIntegrationName) {
    this.pagerDutyIntegrationName = pagerDutyIntegrationName;
  }

  public String getUserIdentifyingHeader() {
    return userIdentifyingHeader;
  }

  public void setUserIdentifyingHeader(String userIdentifyingHeader) {
    this.userIdentifyingHeader = userIdentifyingHeader;
  }

  public String getServiceIdentifyingHeader() {
    return serviceIdentifyingHeader;
  }

  public void setServiceIdentifyingHeader(String serviceIdentifyingHeader) {
    this.serviceIdentifyingHeader = serviceIdentifyingHeader;
  }

  public String getServicePrefix() {
    return servicePrefix;
  }

  public void setServicePrefix(String servicePrefix) {
    this.servicePrefix = servicePrefix;
  }

  public String getServiceDelimiter() {
    return serviceDelimiter;
  }

  public void setServiceDelimiter(String serviceDelimiter) {
    this.serviceDelimiter = serviceDelimiter;
  }

  public Pattern getForbiddenServicePattern() {
    return forbiddenServicePattern;
  }
}
