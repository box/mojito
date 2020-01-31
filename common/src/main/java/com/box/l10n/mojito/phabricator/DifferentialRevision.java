package com.box.l10n.mojito.phabricator;

import com.box.l10n.mojito.phabricator.payload.Data;
import com.box.l10n.mojito.phabricator.payload.ResultWithError;
import com.box.l10n.mojito.phabricator.payload.RevisionSearchFields;
import com.box.l10n.mojito.phabricator.payload.RevisionSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.util.MultiValueMap;

public class DifferentialRevision {

    /**
     * logger
     */
    static final Logger logger = LoggerFactory.getLogger(DifferentialRevision.class);

    static final String TX_TYPE_REVIEWERS_REMOVE = "reviewers.remove";
    static final String TX_TYPE_REVIEWERS_ADD = "reviewers.add";
    static final String TX_TYPE_COMMENT = "comment";
    static final String TRANSACTIONS_0_TYPE = "transactions[0][type]";
    static final String TRANSACTIONS_0_VALUE = "transactions[0][value]";
    static final String TRANSACTIONS_0_VALUE_0 = "transactions[0][value][0]";

    static final String OBJECT_IDENTIFIER = "objectIdentifier";

    PhabricatorHttpClient phabricatorHttpClient;

    public DifferentialRevision(PhabricatorHttpClient phabricatorHttpClient) {
        this.phabricatorHttpClient = phabricatorHttpClient;
    }

    Data<RevisionSearchFields> getRevision(String revisionPHID) {
        try {
            RevisionSearchResult revisionSearchResponse = phabricatorHttpClient.postEntityAndCheckResponse(
                    Method.DIFFERENTIAL_REVISION_SEARCH,
                    phabricatorHttpClient.getConstraintsForPHID(revisionPHID),
                    RevisionSearchResult.class);

            return revisionSearchResponse.getResult().getData().get(0);
        } catch (Exception e) {
            throw new PhabricatorException("Can't find revision", e);
        }
    }

    @SuppressWarnings("Duplicates")
    public void addComment(String objectIdentifier, String value) throws PhabricatorException {
        logger.debug("Add reviewer: {} from: {}", value, objectIdentifier);
        HttpEntity<MultiValueMap<String, Object>> httpEntity = phabricatorHttpClient.getHttpEntityFormUrlEncoded();
        httpEntity.getBody().add(TRANSACTIONS_0_TYPE, TX_TYPE_COMMENT);
        httpEntity.getBody().add(TRANSACTIONS_0_VALUE, value);
        httpEntity.getBody().add(OBJECT_IDENTIFIER, objectIdentifier);
        phabricatorHttpClient.postEntityAndCheckResponse(Method.DIFFERENTIAL_REVISION_EDIT, httpEntity, ResultWithError.class);
    }

    public void removeReviewer(String objectIdentifier, String value, boolean blocking) throws PhabricatorException {
        logger.debug("Remove reviewer: {} from: {}", value, objectIdentifier);
        changeReviewer(objectIdentifier, value, blocking, TX_TYPE_REVIEWERS_REMOVE);
    }

    public void addReviewer(String objectIdentifier, String value, boolean blocking) throws PhabricatorException {
        logger.debug("Add reviewer: {} from: {}", value, objectIdentifier);
        changeReviewer(objectIdentifier, value, blocking, TX_TYPE_REVIEWERS_ADD);
    }

    @SuppressWarnings("Duplicates")
    void changeReviewer(String objectIdentifier, String value, boolean blocking, String txType) throws PhabricatorException {
        String optionalBlockingValue = optionalWrapWithBlocking(value, blocking);
        HttpEntity<MultiValueMap<String, Object>> httpEntity = phabricatorHttpClient.getHttpEntityFormUrlEncoded();
        httpEntity.getBody().add(TRANSACTIONS_0_TYPE, txType);
        httpEntity.getBody().add(TRANSACTIONS_0_VALUE_0, optionalBlockingValue);
        httpEntity.getBody().add(OBJECT_IDENTIFIER, objectIdentifier);
        phabricatorHttpClient.postEntityAndCheckResponse(Method.DIFFERENTIAL_REVISION_EDIT, httpEntity, ResultWithError.class);
    }

    String optionalWrapWithBlocking(String value, boolean blocking) {
        if (blocking) {
            value = "blocking(" + value + ")";
        }
        return value;
    }

}
