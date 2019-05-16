package com.box.l10n.mojito.smartling;

import com.box.l10n.mojito.smartling.response.AuthenticationResponse;
import com.box.l10n.mojito.smartling.response.SourceStringsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Smartling client based on the Web API: https://github.com/Smartling/api-sdk-java
 */
public class SmartlingClient {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(SmartlingClient.class);

    private static final String DEFAULT_BASE_HOST = "https://api.smartling.com/";
    private static final String AUTH_API_V2_AUTHENTICATE = DEFAULT_BASE_HOST + "auth-api/v2/authenticate";
    private static final String AUTH_API_V2_REFRESH = DEFAULT_BASE_HOST + "auth-api/v2/authenticate/refresh";
    private static final String API_PULL_SOURCE_STRINGS = DEFAULT_BASE_HOST + "strings-api/v2/projects/{projectId}/source-strings?fileUri={fileUri}&offset={offset}";
    private static final String API_PUSH_NEW_CONTEXT = DEFAULT_BASE_HOST + "context-api/v2/projects/{projectId}/contexts";
    private static final String API_BIND_CONTEXT_TO_HASHCODE = DEFAULT_BASE_HOST + "context-api/v2/projects/{projecId}/bindings";

    RestTemplate restTemplate = new RestTemplate();

    private String userIdentifier;
    private String userSecret;
    private String authToken = null;
    private String refreshToken = null;
    private Instant nextAuthentication = Instant.now();
    private Instant nextRefresh = Instant.now();
    private Instant lastRefresh = Instant.now();
    // Refresh tokens are valid for up to 1 hour
    private static final Integer refreshDurationMinutes = 50;
    // Authentication tokens are valid for up to 5 minutes
    private static final Integer authenticationDurationMinutes = 4;
    // Authentication tokens can be refreshed for up to 24 hours
    private static final Integer authenticationDurationHours = 24;

    public SmartlingClient(String userIdentifier, String userSecret) {
        this.userIdentifier = userIdentifier;
        this.userSecret = userSecret;
    }

    private Map<String, Object> getPayloadMapAuthentication() {
        Map<String, Object> map = new HashMap<>();
        map.put("userIdentifier", userIdentifier);
        map.put("userSecret", userSecret);
        return map;
    }

    private Map<String, Object> getPayloadMapRefresh() {
        Map<String, Object> map = new HashMap<>();
        map.put("refreshToken", refreshToken);
        return map;
    }

    private Map<String, Object> getPayloadMapPullSourceString() {
        return new HashMap<>();
    }

    private HttpHeaders getHttpHeadersForJson() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders getHttpHeadersForJsonWithAuthorization() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + authToken);
        return headers;
    }

    private <T> HttpEntity<T> getHttpEntityForPayload(T payload) {
        return new HttpEntity<>(payload, getHttpHeadersForJson());
    }

    private <T> HttpEntity<T> getHttpEntityForPayloadWithAuthorization(T payload) {
        return new HttpEntity<>(payload, getHttpHeadersForJsonWithAuthorization());
    }

    public void authenticate() throws SmartlingClientException {
        Map<String, Object> payload = getPayloadMapAuthentication();
        AuthenticationResponse authenticationResponse = postForAuthentication(payload, AUTH_API_V2_AUTHENTICATE);

        if (authenticationResponse.isAuthenticationErrorResponse()) {
            throw new SmartlingClientException("Error authenticating with Smartling API, please check your credentials.");
        } else if (!authenticationResponse.isSuccessResponse()) {
            throw new SmartlingClientException(authenticationResponse.getErrorMessage());
        } else {
            logger.debug("Successful authentication with Smartling API.");
            Instant now = Instant.now();
            nextAuthentication = now.plus(Duration.ofHours(authenticationDurationHours));
            nextRefresh = now.plus(Duration.ofMinutes(authenticationDurationMinutes));
            lastRefresh = now;
            authToken = authenticationResponse.getResponse().getData().getAccessToken();
            refreshToken = authenticationResponse.getResponse().getData().getRefreshToken();
        }
    }

    AuthenticationResponse postForAuthentication(Map<String, Object> payload, String authApiV2) throws SmartlingClientException {
        HttpEntity<Map<String, Object>> httpEntity = getHttpEntityForPayload(payload);
        AuthenticationResponse postForObject = restTemplate.postForObject(
                authApiV2, httpEntity, AuthenticationResponse.class);
        return postForObject;
    }

    public void refresh() throws SmartlingClientException {
        Instant now = Instant.now();
        // Check if time for next authentication or last refresh was more than ~ an hour ago,
        // which means our current refresh token has expired. If neither of those conditions satisfied then check if
        // time for next refresh has passed
        if (now.isAfter(nextAuthentication) || lastRefresh.plus(Duration.ofMinutes(refreshDurationMinutes)).isBefore(now)) {
            authenticate();
        } else if (!(nextRefresh.isAfter(now))) {
            Map<String, Object> payload = getPayloadMapRefresh();
            AuthenticationResponse authenticationResponse = postForAuthentication(payload, AUTH_API_V2_REFRESH);

            if (authenticationResponse.isAuthenticationErrorResponse()) {
                authenticate();
            } else if (!authenticationResponse.isSuccessResponse()) {
                nextRefresh = now;
                throw new SmartlingClientException(authenticationResponse.getErrorMessage());
            } else {
                logger.debug("Successful refresh of authentication with Smartling API.");
                nextRefresh = now.plus(Duration.ofMinutes(authenticationDurationMinutes));
                lastRefresh = now;
                refreshToken = authenticationResponse.getResponse().getData().getRefreshToken();
                authToken = authenticationResponse.getResponse().getData().getAccessToken();
            }
        }
    }

    public SourceStringsResponse getSourceStrings(String projectId, String file, Integer offset) throws SmartlingClientException {
        refresh();
        Map<String, Object> payload = getPayloadMapPullSourceString();

        HttpEntity<Map<String, Object>> httpEntity = getHttpEntityForPayloadWithAuthorization(payload);

        Map<String, String> pathVariables = new HashMap<>();
        pathVariables.put("projectId", projectId);
        pathVariables.put("fileUri", file);
        pathVariables.put("offset", offset.toString());
        UriTemplate template = new UriTemplate(API_PULL_SOURCE_STRINGS);
        SourceStringsResponse sourceStringsResponse = new SourceStringsResponse();
        ResponseEntity<SourceStringsResponse> exchange = null;
        try {
            exchange = restTemplate.exchange(
                    template.toString(), HttpMethod.GET, httpEntity, SourceStringsResponse.class, pathVariables);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
                logger.debug("Unauthorized error occurred when attempting to pull the source strings via Smartling API.");
                authenticate();
            } else {
                throw e;
            }
        }
        if (exchange != null) {
            sourceStringsResponse = exchange.getBody();
            if (!sourceStringsResponse.isSuccessResponse()) {
                throw new SmartlingClientException(sourceStringsResponse.getErrorMessage());
            }
        }
        return sourceStringsResponse;
    }

}
