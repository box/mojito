package com.box.l10n.mojito.smartling;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("l10n.smartling")
public class SmartlingClientConfiguration {

    String userIdentifier;
    String userSecret;

    @ConditionalOnProperty(prefix="l10n.smartling", value = { "userIdentifier", "userSecret" })
    @Bean
    public SmartlingClient getSmartlingClient() {
        return new SmartlingClient(userIdentifier, userSecret);
    }

    public String getUserIdentifier() {
        return userIdentifier;
    }

    public void setUserIdentifier(String userIdentifier) {
        this.userIdentifier = userIdentifier;
    }

    public String getUserSecret() {
        return userSecret;
    }

    public void setUserSecret(String userSecret) {
        this.userSecret = userSecret;
    }
}
