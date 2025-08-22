package com.box.l10n.mojito.react;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReactStaticAppConfig {

  @JsonUnwrapped @Autowired LinkConfig link;

  @Autowired RepositoryStatisticsConfig repositoryStatistics;

  @Autowired GoogleAnalyticsConfig googleAnalytics;

  @Autowired ReactSecurityConfig security;

  @Autowired ReactStatelessSecurityConfig stateless;

  @Autowired ScreenshotsUiConfig screenshots;

  public LinkConfig getLink() {
    return link;
  }

  public void setLink(LinkConfig link) {
    this.link = link;
  }

  public RepositoryStatisticsConfig getRepositoryStatistics() {
    return repositoryStatistics;
  }

  public void setRepositoryStatistics(RepositoryStatisticsConfig repositoryStatistics) {
    this.repositoryStatistics = repositoryStatistics;
  }

  public GoogleAnalyticsConfig getGoogleAnalytics() {
    return googleAnalytics;
  }

  public void setGoogleAnalytics(GoogleAnalyticsConfig googleAnalytics) {
    this.googleAnalytics = googleAnalytics;
  }

  public ReactSecurityConfig getSecurity() {
    return security;
  }

  public void setSecurity(ReactSecurityConfig security) {
    this.security = security;
  }

  public ReactStatelessSecurityConfig getStateless() {
    return stateless;
  }

  public void setStateless(ReactStatelessSecurityConfig stateless) {
    this.stateless = stateless;
  }

  public ScreenshotsUiConfig getScreenshots() {
    return screenshots;
  }

  public void setScreenshots(ScreenshotsUiConfig screenshots) {
    this.screenshots = screenshots;
  }
}
