package com.box.l10n.mojito.smartling;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.AccessTokenProvider;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@ConfigurationProperties("l10n.smartling")
@EnableOAuth2Client
public class SmartlingClientConfiguration {

    String userIdentifier;
    String userSecret;

    @ConditionalOnProperty(prefix = "l10n.smartling", value = {"userIdentifier", "userSecret"})
    @Bean
    public SmartlingClient getSmartlingClient() {
        return new SmartlingClient();
    }

    @ConditionalOnProperty(prefix = "l10n.smartling", value = {"userIdentifier", "userSecret"})
    @Bean
    public ResourceOwnerPasswordResourceDetails smartling() {
        ResourceOwnerPasswordResourceDetails details = new ResourceOwnerPasswordResourceDetails();
        details.setId("smartling");
        details.setUsername(userIdentifier);
        details.setPassword(userSecret);
        details.setAccessTokenUri("https://api.smartling.com/auth-api/v2/authenticate");
        details.setAuthenticationScheme(AuthenticationScheme.query);
        return details;
    }

    @ConditionalOnProperty(prefix = "l10n.smartling", value = {"userIdentifier", "userSecret"})
    @Bean
    public OAuth2ClientContext singletonClientContext() {
        return new DefaultOAuth2ClientContext(new DefaultAccessTokenRequest());
    }


    @ConditionalOnProperty(prefix = "l10n.smartling", value = {"userIdentifier", "userSecret"})
    @Bean
    public OAuth2RestTemplate smartlingRestTemplate() {
        OAuth2RestTemplate template = new OAuth2RestTemplate(smartling(), singletonClientContext());

        AccessTokenProvider accessTokenProvider = new ResourceOwnerPasswordAccessTokenProvider();
        ((ResourceOwnerPasswordAccessTokenProvider) accessTokenProvider).setRequestFactory(new BufferingClientHttpRequestFactory(
                new SimpleClientHttpRequestFactory()
        ));
        ((ResourceOwnerPasswordAccessTokenProvider) accessTokenProvider).setInterceptors(Collections.singletonList(new SmartlingRequestInterceptor()));
        template.setAccessTokenProvider(accessTokenProvider);

        return template;
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
