package com.box.l10n.mojito.slack;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("l10n.slack")
public class SlackClientConfiguration {

    String token;

    @ConditionalOnProperty("l10n.slack.token")
    @Bean
    SlackClient getSlackClient() {
        return new SlackClient(token);
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
