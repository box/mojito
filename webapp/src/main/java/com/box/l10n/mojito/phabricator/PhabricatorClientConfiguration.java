package com.box.l10n.mojito.phabricator;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("l10n.phabricator")
public class PhabricatorClientConfiguration {

    String token;
    String url;

    @ConditionalOnProperty("l10n.phabricator.token")
    @Bean
    PhabricatorClient getPhabricatorClient() {
        return new PhabricatorClient(url, token);
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
