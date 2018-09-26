package com.box.l10n.mojito.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.client.resource.UserRedirectRequiredException;
import org.springframework.security.oauth2.config.annotation.web.configuration.OAuth2ClientConfiguration;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Use that configuration to percent encode the redirect_uri when fetching the authorization token.
 *
 * For standard usage this should not be used.
 *
 * This is needed when integrating with an OAuth server that is not 100% compliant with the standard and requires
 * the redirect URI to be percent encoded.
 */
@ConditionalOnProperty(value = "l10n.security.oauth2.encodeRedirectUri", havingValue = "true")
@Configuration
public class OAuth2ClientWithEncdedRedirectUriConfiguration extends OAuth2ClientConfiguration {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(OAuth2ClientWithEncdedRedirectUriConfiguration.class);

    @Override
    public OAuth2ClientContextFilter oauth2ClientContextFilter() {
        return new OAuth2ClientContextFilter() {

            private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

            /**
             * Same implementation as parent class but skip "redirect_uri" in the builder and at it at the end with
             * percent encoding
             */
            @Override
            protected void redirectUser(UserRedirectRequiredException e, HttpServletRequest request, HttpServletResponse response) throws IOException {

                logger.debug("redirectUser with percent encoded redirect_uri");

                String redirectUri = e.getRedirectUri();
                UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(redirectUri);
                Map<String, String> requestParams = e.getRequestParams();

                String redirectUriParam = null;

                for (Map.Entry<String, String> param : requestParams.entrySet()) {
                    if ("redirect_uri".equals(param.getKey())) {
                        redirectUriParam = param.getValue();
                    } else {
                        builder.queryParam(param.getKey(), param.getValue());
                    }
                }

                if (e.getStateKey() != null) {
                    builder.queryParam("state", e.getStateKey());
                }

                String uri = builder.build().encode().toUriString();

                if (redirectUriParam != null) {
                    uri += "&redirect_uri=" + URLEncoder.encode(redirectUriParam, StandardCharsets.UTF_8.toString());
                }

                this.redirectStrategy.sendRedirect(request, response, uri);
            }

            @Override
            public void setRedirectStrategy(RedirectStrategy redirectStrategy) {
                super.setRedirectStrategy(redirectStrategy);
                this.redirectStrategy = redirectStrategy;
            }
        };
    }
}
