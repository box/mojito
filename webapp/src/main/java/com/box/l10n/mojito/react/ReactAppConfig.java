package com.box.l10n.mojito.react;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReactAppConfig {

    @JsonUnwrapped
    @Autowired
    LinkConfig link;

    @Autowired
    LoginConfig login;

    @Autowired
    RepositoryStatisticsConfig repositoryStatisticsConfig;

    @Autowired
    GoogleAnalyticsConfig googleAnalytics;

    public LinkConfig getLink() {
        return link;
    }

    public void setLink(LinkConfig link) {
        this.link = link;
    }

    public LoginConfig getLogin() {
        return login;
    }

    public void setLogin(LoginConfig login) {
        this.login = login;
    }

    public RepositoryStatisticsConfig getRepositoryStatisticsConfig() {
        return repositoryStatisticsConfig;
    }

    public void setRepositoryStatisticsConfig(RepositoryStatisticsConfig repositoryStatisticsConfig) {
        this.repositoryStatisticsConfig = repositoryStatisticsConfig;
    }

    public GoogleAnalyticsConfig getGoogleAnalytics() {
        return googleAnalytics;
    }

    public void setGoogleAnalytics(GoogleAnalyticsConfig googleAnalytics) {
        this.googleAnalytics = googleAnalytics;
    }
}
