package com.box.l10n.mojito.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;

import javax.servlet.Filter;

// This must in sync with {@link com.box.l10n.mojito.security.SecurityConfig.AuthenticationType#OAUTH2}
@ConditionalOnExpression("'${l10n.security.authenticationType:}'.toUpperCase().contains('OAUTH2')")
@Configuration
public class WebSecurityOAuth2Config {

    static Logger logger = LoggerFactory.getLogger(WebSecurityOAuth2Config.class);

    @Bean
    @ConfigurationProperties("l10n.security.oauth2.client")
    public AuthorizationCodeResourceDetails oauth2() {
        return new AuthorizationCodeResourceDetails();
    }

    @Bean
    @ConfigurationProperties("l10n.security.oauth2.resource")
    public ResourceServerProperties oauth2Resource() {
        return new ResourceServerProperties();
    }

    @Bean
    public FilterRegistrationBean oauth2ClientFilterRegistration(OAuth2ClientContextFilter filter) {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(filter);
        registration.setOrder(-100);
        return registration;
    }

    @Bean("oauth2Filter")
    public Filter oauth2Filter(OAuth2ClientContext oauth2ClientContext) {
        logger.error("Setup SSO filter for oauth2");
        OAuth2ClientAuthenticationProcessingFilter oauth2Filter = new OAuth2ClientAuthenticationProcessingFilter(
                "/login/oauth");
        OAuth2RestTemplate auth2RestTemplate = new OAuth2RestTemplate(oauth2(), oauth2ClientContext);

        oauth2Filter.setRestTemplate(auth2RestTemplate);
        UserInfoTokenServices tokenServices = new UserInfoTokenServices(
                oauth2Resource().getUserInfoUri(),
                oauth2().getClientId());
        tokenServices.setRestTemplate(auth2RestTemplate);

        oauth2Filter.setTokenServices(new MyUserInfoTokenServices(
                oauth2Resource().getUserInfoUri(),
                oauth2().getClientId()));

        return oauth2Filter;
    }
}
