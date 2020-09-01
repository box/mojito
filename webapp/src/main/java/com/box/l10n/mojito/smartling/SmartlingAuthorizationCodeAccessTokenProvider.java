package com.box.l10n.mojito.smartling;

import com.box.l10n.mojito.smartling.response.AuthenticationData;
import com.box.l10n.mojito.smartling.response.AuthenticationResponse;
import com.box.l10n.mojito.utils.RestTemplateUtils;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.resource.UserApprovalRequiredException;
import org.springframework.security.oauth2.client.resource.UserRedirectRequiredException;
import org.springframework.security.oauth2.client.token.AccessTokenProvider;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.common.DefaultExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jaurambault
 */
public class SmartlingAuthorizationCodeAccessTokenProvider implements AccessTokenProvider {

    static Logger logger = LoggerFactory.getLogger(SmartlingAuthorizationCodeAccessTokenProvider.class);

    RestTemplate restTemplate = new RestTemplate();

    RestTemplateUtils restTemplateUtils = new RestTemplateUtils();

    public boolean supportsResource(OAuth2ProtectedResourceDetails resource) {
        return resource instanceof SmartlingOAuth2ProtectedResourceDetails && "smartling".equals(resource.getGrantType());
    }

    public SmartlingAuthorizationCodeAccessTokenProvider() {
        restTemplateUtils.enableFeature(restTemplate, DeserializationFeature.UNWRAP_ROOT_VALUE);
    }

    @Override
    public OAuth2AccessToken obtainAccessToken(OAuth2ProtectedResourceDetails details, AccessTokenRequest accessTokenRequest) throws UserRedirectRequiredException, UserApprovalRequiredException, AccessDeniedException {

        logger.debug("Get access token");
        Map<String, String> request = new HashMap<>();
        request.put("userIdentifier", details.getClientId());
        request.put("userSecret", details.getClientSecret());

        DefaultOAuth2AccessToken defaultOAuth2AccessToken = null;
        try {
            DateTime now = getNowForToken();
            AuthenticationResponse authenticationResponse = restTemplate.postForObject(details.getAccessTokenUri(), request, AuthenticationResponse.class);
            defaultOAuth2AccessToken = getDefaultOAuth2AccessToken(now, authenticationResponse);
        } catch (Exception e) {
            String msg = "Can't get Smartling token";
            logger.debug(msg, e);
            throw new OAuth2AccessDeniedException(msg, details, e);
        }

        return defaultOAuth2AccessToken;
    }

    @Override
    public OAuth2AccessToken refreshAccessToken(OAuth2ProtectedResourceDetails resource, OAuth2RefreshToken refreshToken, AccessTokenRequest accessTokenRequest) throws UserRedirectRequiredException {

        logger.debug("Get refresh token");

        SmartlingOAuth2ProtectedResourceDetails smartlingOAuth2ProtectedResourceDetails = (SmartlingOAuth2ProtectedResourceDetails) resource;
        Map<String, String> request = new HashMap<>();
        request.put("refreshToken", refreshToken.getValue());

        DefaultOAuth2AccessToken defaultOAuth2AccessToken = null;
        try {
            DateTime now = getNowForToken();
            AuthenticationResponse authenticationResponse = restTemplate.postForObject(smartlingOAuth2ProtectedResourceDetails.getRefreshUri(), request, AuthenticationResponse.class);
            defaultOAuth2AccessToken = getDefaultOAuth2AccessToken(now, authenticationResponse);
        } catch (Exception e) {
            String msg = "Can't get Smartling refresh token";
            logger.debug(msg, e);
            throw new OAuth2AccessDeniedException(msg, resource, e);
        }

        return defaultOAuth2AccessToken;
    }

    @Override
    public boolean supportsRefresh(OAuth2ProtectedResourceDetails resource) {
        return true;
    }

    DefaultOAuth2AccessToken getDefaultOAuth2AccessToken(DateTime now, AuthenticationResponse authenticationResponse) {
        AuthenticationData data = authenticationResponse.getData();
        DefaultOAuth2AccessToken defaultOAuth2AccessToken = new DefaultOAuth2AccessToken(data.getAccessToken());
        defaultOAuth2AccessToken.setExpiration(now.plusSeconds(data.getExpiresIn()).toDate());
        defaultOAuth2AccessToken.setRefreshToken(new DefaultExpiringOAuth2RefreshToken(
                data.getRefreshToken(),
                now.plusSeconds(data.getRefreshExpiresIn()).toDate()));
        return defaultOAuth2AccessToken;
    }

    /**
     * Since Smartling gives the TTL of the token but not the emission time, play safe taking now() minus 15 seconds as
     * "now" time. If the token is already expired it's better to take it as expired, even if it has some leeway, and
     * just refresh it while keeping the token alive.
     *
     * @return
     */
    DateTime getNowForToken() {
        return DateTime.now().minusSeconds(15);
    }
}
