package com.box.l10n.mojito.smartling;

import com.box.l10n.mojito.smartling.response.SourceStringsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.web.util.UriTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Smartling client based on the Web API: https://github.com/Smartling/api-sdk-java
 */
public class SmartlingClient {

    private static final String DEFAULT_BASE_HOST = "https://api.smartling.com/";
    private static final String API_PULL_SOURCE_STRINGS = DEFAULT_BASE_HOST + "strings-api/v2/projects/{projectId}/source-strings?fileUri={fileUri}&offset={offset}";
    private static final String API_PUSH_NEW_CONTEXT = DEFAULT_BASE_HOST + "context-api/v2/projects/{projectId}/contexts";
    private static final String API_BIND_CONTEXT_TO_HASHCODE = DEFAULT_BASE_HOST + "context-api/v2/projects/{projecId}/bindings";
    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(SmartlingClient.class);
    @Autowired
    @Qualifier("smartlingRestTemplate")
    private OAuth2RestTemplate smartlingRestTemplate;

    private Map<String, Object> getPayloadMapPullSourceString() {
        return new HashMap<>();
    }

    private HttpHeaders getHttpHeadersForJsonWithAuthorization() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + smartlingRestTemplate.getAccessToken());
        return headers;
    }

    private <T> HttpEntity<T> getHttpEntityForPayloadWithAuthorization(T payload) {
        return new HttpEntity<>(payload, getHttpHeadersForJsonWithAuthorization());
    }

    public SourceStringsResponse getSourceStrings(String projectId, String file, Integer offset) throws SmartlingClientException {
        Map<String, Object> payload = getPayloadMapPullSourceString();

        HttpEntity<Map<String, Object>> httpEntity = getHttpEntityForPayloadWithAuthorization(payload);

        Map<String, String> pathVariables = new HashMap<>();
        pathVariables.put("projectId", projectId);
        pathVariables.put("fileUri", file);
        pathVariables.put("offset", offset.toString());
        UriTemplate template = new UriTemplate(API_PULL_SOURCE_STRINGS);
        SourceStringsResponse sourceStringsResponse = new SourceStringsResponse();
        ResponseEntity<SourceStringsResponse> exchange = smartlingRestTemplate.exchange(template.toString(), HttpMethod.GET, httpEntity, SourceStringsResponse.class, pathVariables);
        if (exchange != null) {
            sourceStringsResponse = exchange.getBody();
            if (!sourceStringsResponse.isSuccessResponse()) {
                throw new SmartlingClientException(sourceStringsResponse.getErrorMessage());
            }
        }
        return sourceStringsResponse;
    }

}
