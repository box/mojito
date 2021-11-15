package com.box.l10n.mojito.phabricator;

import com.box.l10n.mojito.phabricator.payload.ResultWithError;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

class PhabricatorHttpClient {

    static final String API_TOKEN = "api.token";
    static final String CONSTRAINTS_PHIDS_0 = "constraints[phids][0]";
    static final String IDS_0 = "ids[0]";
    static final String REVISION_ID = "revision_id";

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(PhabricatorHttpClient.class);

    RestTemplate restTemplate = new RestTemplate();

    String authToken;

    String baseUrl;

    public PhabricatorHttpClient(String baseUrl, String authToken) {
        this.authToken = authToken;
        this.baseUrl = baseUrl;
    }

    public <T extends ResultWithError> T postEntityAndCheckResponse(
            Method method,
            HttpEntity<MultiValueMap<String, Object>> httpEntity,
            Class<T> clazz) throws PhabricatorException {

        String url = getUrl(method.getMethod());

        T responseWithError = restTemplate.postForObject(
                url,
                httpEntity,
                clazz);

        checkNoError(responseWithError);
        return responseWithError;
    }

    public HttpEntity<MultiValueMap<String, Object>> getHttpEntityFormUrlEncoded() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add(API_TOKEN, authToken);
        return new HttpEntity<>(body, headers);
    }

    public HttpEntity<MultiValueMap<String, Object>> getConstraintsForPHID(String phid) {
        HttpEntity<MultiValueMap<String, Object>> httpEntity = getHttpEntityFormUrlEncoded();
        httpEntity.getBody().add(CONSTRAINTS_PHIDS_0, phid);
        return httpEntity;
    }

    public HttpEntity<MultiValueMap<String, Object>> withId(String id) {
        HttpEntity<MultiValueMap<String, Object>> httpEntity = getHttpEntityFormUrlEncoded();
        httpEntity.getBody().add(IDS_0, id);
        return httpEntity;
    }

    public HttpEntity<MultiValueMap<String, Object>> withRevisionId(String revisionId) {
        HttpEntity<MultiValueMap<String, Object>> httpEntity = getHttpEntityFormUrlEncoded();
        httpEntity.getBody().add(REVISION_ID, revisionId);
        return httpEntity;
    }

    String getUrl(String method) {
        return this.baseUrl + "/api/" + method;
    }

    void checkNoError(ResultWithError resultWithError) throws PhabricatorException {
        Preconditions.checkNotNull(resultWithError);
        if (resultWithError.getErrorCode() != null) {
            throw new PhabricatorException(resultWithError.getErrorCode() + "\n" + resultWithError.getErrorInfo());
        }
    }

}
