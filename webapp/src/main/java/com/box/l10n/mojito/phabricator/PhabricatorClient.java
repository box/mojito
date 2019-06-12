package com.box.l10n.mojito.phabricator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

public class PhabricatorClient {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(PhabricatorClient.class);

    static final String API_TOKEN = "api.token";
    static final String METHOD_DIFFERENTIAL_REVISION_EDIT = "differential.revision.edit";

    static final String TX_TYPE_REVIEWERS_REMOVE = "reviewers.remove";
    static final String TX_TYPE_REVIEWERS_ADD = "reviewers.add";
    static final String TX_TYPE_COMMENT = "comment";

    static final String TRANSACTIONS_0_TYPE = "transactions[0][type]";
    static final String TRANSACTIONS_0_VALUE = "transactions[0][value]";
    static final String TRANSACTIONS_0_VALUE_0 = "transactions[0][value][0]";
    static final String OBJECT_IDENTIFIER = "objectIdentifier";

    RestTemplate restTemplate = new RestTemplate();

    String authToken;

    String baseUrl;

    public PhabricatorClient(String baseUrl, String authToken) {
        this.authToken = authToken;
        this.baseUrl = baseUrl;
    }

    @SuppressWarnings("Duplicates")
    public void addComment(String objectIdentifier, String value) throws PhabricatorClientException {
        logger.debug("Add reviewer: {} from: {}", value, objectIdentifier);
        HttpEntity<MultiValueMap<String, Object>> httpEntity = getHttpEntityFormUrlEncoded();
        httpEntity.getBody().add(TRANSACTIONS_0_TYPE, TX_TYPE_COMMENT);
        httpEntity.getBody().add(TRANSACTIONS_0_VALUE, value);
        httpEntity.getBody().add(OBJECT_IDENTIFIER, objectIdentifier);
        postEntityAndCheckResponse(METHOD_DIFFERENTIAL_REVISION_EDIT, httpEntity);
    }

    public void removeReviewer(String objectIdentifier, String value, boolean blocking) throws PhabricatorClientException {
        logger.debug("Remove reviewer: {} from: {}", value, objectIdentifier);
        changeReviewer(objectIdentifier, value, blocking, TX_TYPE_REVIEWERS_REMOVE);
    }

    public void addReviewer(String objectIdentifier, String value, boolean blocking) throws PhabricatorClientException {
        logger.debug("Add reviewer: {} from: {}", value, objectIdentifier);
        changeReviewer(objectIdentifier, value, blocking, TX_TYPE_REVIEWERS_ADD);
    }

    @SuppressWarnings("Duplicates")
    void changeReviewer(String objectIdentifier, String value, boolean blocking, String txType) throws PhabricatorClientException {
        String optionalBlockingValue = optionalWrapWithBlocking(value, blocking);
        HttpEntity<MultiValueMap<String, Object>> httpEntity = getHttpEntityFormUrlEncoded();
        httpEntity.getBody().add(TRANSACTIONS_0_TYPE, txType);
        httpEntity.getBody().add(TRANSACTIONS_0_VALUE_0, optionalBlockingValue);
        httpEntity.getBody().add(OBJECT_IDENTIFIER, objectIdentifier);
        postEntityAndCheckResponse(METHOD_DIFFERENTIAL_REVISION_EDIT, httpEntity);
    }


    String optionalWrapWithBlocking(String value, boolean blocking) {
        if (blocking) {
            value = "blocking(" + value + ")";
        }
        return value;
    }

    void postEntityAndCheckResponse(String method, HttpEntity<MultiValueMap<String, Object>> httpEntity) throws PhabricatorClientException {
        Response response = restTemplate.postForObject(
                getUrl(method),
                httpEntity,
                Response.class);

        checkResponseForError(response);
    }

    String getUrl(String method) {
        return this.baseUrl + "/api/" + method;
    }

    HttpEntity<MultiValueMap<String, Object>> getHttpEntityFormUrlEncoded() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add(API_TOKEN, authToken);
        return new HttpEntity<>(body, headers);
    }

    void checkResponseForError(Response response) throws PhabricatorClientException {
        if (response.getErrorCode() != null) {
            throw new PhabricatorClientException(response.getErrorCode() + "\n" + response.getErrorInfo());
        }
    }
}
