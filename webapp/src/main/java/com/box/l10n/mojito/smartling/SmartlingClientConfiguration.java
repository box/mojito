package com.box.l10n.mojito.smartling;

import com.box.l10n.mojito.utils.RestTemplateUtils;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenProviderChain;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.web.util.DefaultUriTemplateHandler;

import java.util.Arrays;

/**
 * @author jaurambault
 */
@Configuration
@EnableOAuth2Client
@ConfigurationProperties("l10n.smartling")
public class SmartlingClientConfiguration {

    String baseUri = "https://api.smartling.com/";
    String accessTokenUri = "https://api.smartling.com/auth-api/v2/authenticate";
    String refreshTokenUri = "https://api.smartling.com/auth-api/v2/authenticate/refresh";

    String clientID;
    String clientSecret;

    @ConditionalOnProperty("l10n.smartling.clientID")
    @Bean
    public SmartlingClient getSmartlingClient() {
        return new SmartlingClient(smartlingRestTemplate());
    }

    @Bean
    public OAuth2ProtectedResourceDetails smartling() {
        SmartlingOAuth2ProtectedResourceDetails details = new SmartlingOAuth2ProtectedResourceDetails();
        details.setId("Smartling");
        details.setGrantType("smartling");
        details.setClientId(clientID);
        details.setClientSecret(clientSecret);
        details.setAccessTokenUri(accessTokenUri);
        details.setRefreshUri(refreshTokenUri);
        return details;
    }

    public OAuth2RestTemplate smartlingRestTemplate() {
        OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(smartling(), new DefaultOAuth2ClientContext());

        RestTemplateUtils restTemplateUtils = new RestTemplateUtils();
        restTemplateUtils.enableFeature(oAuth2RestTemplate, DeserializationFeature.UNWRAP_ROOT_VALUE);

        AccessTokenProviderChain accessTokenProviderChain = new AccessTokenProviderChain(Arrays.asList(
                new SmartlingAuthorizationCodeAccessTokenProvider())
        );
        oAuth2RestTemplate.setAccessTokenProvider(accessTokenProviderChain);

        DefaultUriTemplateHandler defaultUriTemplateHandler = new DefaultUriTemplateHandler();
        defaultUriTemplateHandler.setBaseUrl(baseUri);

        oAuth2RestTemplate.setUriTemplateHandler(defaultUriTemplateHandler);
        return oAuth2RestTemplate;
    }

    public String getAccessTokenUri() {
        return accessTokenUri;
    }

    public void setAccessTokenUri(String accessTokenUri) {
        this.accessTokenUri = accessTokenUri;
    }

    public String getRefreshTokenUri() {
        return refreshTokenUri;
    }

    public void setRefreshTokenUri(String refreshTokenUri) {
        this.refreshTokenUri = refreshTokenUri;
    }

    public String getClientID() {
        return clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}
