package com.box.l10n.mojito.react;

import com.box.l10n.mojito.security.SecurityConfig;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReactAppConfig {

    @JsonUnwrapped
    @Autowired
    LinkConfig link;

    @Autowired
    RepositoryStatisticsConfig repositoryStatistics;

    @Autowired
    GoogleAnalyticsConfig googleAnalytics;

    //TODO(spring2) review if we want to isolate frontend only configuration - it is a subset of the backend, see how
    //we can map them simply
    @Autowired
    SecurityConfig security;

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

    public SecurityConfig getSecurity() {
        return security;
    }

    public void setSecurity(SecurityConfig security) {
        this.security = security;
    }
}
