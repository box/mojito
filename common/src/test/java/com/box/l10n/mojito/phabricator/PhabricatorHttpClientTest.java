package com.box.l10n.mojito.phabricator;

import com.box.l10n.mojito.phabricator.payload.ResultWithError;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import static com.box.l10n.mojito.phabricator.PhabricatorHttpClient.API_TOKEN;
import static com.box.l10n.mojito.phabricator.PhabricatorHttpClient.CONSTRAINTS_PHIDS_0;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

@RunWith(MockitoJUnitRunner.class)
public class PhabricatorHttpClientTest {

    PhabricatorHttpClient phabricatorHttpClient;

    @Mock
    RestTemplate mockRestTemplate;

    @Before
    public void before() {
        phabricatorHttpClient = new PhabricatorHttpClient("https://secure.phabricator.com", "testtoken");
        phabricatorHttpClient.restTemplate = mockRestTemplate;
    }

    @Test
    public void postEntityAndCheckResponse() {

        ResultWithError objectResultWithError = new ResultWithError();
        Mockito.doReturn(objectResultWithError).when(mockRestTemplate).postForObject(
                eq("https://secure.phabricator.com/api/differential.revision.search"),
                any(),
                eq(ResultWithError.class));

        ResultWithError resultWithError = phabricatorHttpClient.postEntityAndCheckResponse(
                Method.DIFFERENTIAL_REVISION_SEARCH,
                phabricatorHttpClient.getHttpEntityFormUrlEncoded(),
                ResultWithError.class);

        assertEquals(objectResultWithError, resultWithError);
    }

    @Test(expected = PhabricatorException.class)
    public void postEntityAndCheckResponseError() {

        ResultWithError objectResultWithError = new ResultWithError();
        objectResultWithError.setErrorCode("ERROR");
        Mockito.doReturn(objectResultWithError).when(mockRestTemplate).postForObject(
                eq("https://secure.phabricator.com/api/differential.revision.search"),
                any(),
                eq(ResultWithError.class));

        ResultWithError resultWithError = phabricatorHttpClient.postEntityAndCheckResponse(
                Method.DIFFERENTIAL_REVISION_SEARCH,
                phabricatorHttpClient.getHttpEntityFormUrlEncoded(),
                ResultWithError.class);

        assertEquals(objectResultWithError, resultWithError);
    }

    @Test
    public void getHttpEntityFormUrlEncoded() {
        HttpEntity<MultiValueMap<String, Object>> constraintsForPHID = phabricatorHttpClient.getHttpEntityFormUrlEncoded();
        assertEquals("testtoken", constraintsForPHID.getBody().getFirst(API_TOKEN));
        assertEquals(1, constraintsForPHID.getBody().size());
    }

    @Test
    public void getConstraintsForPHID() {
        HttpEntity<MultiValueMap<String, Object>> constraintsForPHID = phabricatorHttpClient.getConstraintsForPHID("PHID-HMBT-sometest");
        assertEquals("PHID-HMBT-sometest", constraintsForPHID.getBody().getFirst(CONSTRAINTS_PHIDS_0));
        assertEquals("testtoken", constraintsForPHID.getBody().getFirst(API_TOKEN));
        assertEquals(2, constraintsForPHID.getBody().size());
    }

    @Test
    public void getUrl() {
        assertEquals("https://secure.phabricator.com/api/somemethod", phabricatorHttpClient.getUrl("somemethod"));
    }

    @Test(expected = PhabricatorException.class)
    public void checkNoError() {
        ResultWithError objectResultWithError = new ResultWithError();
        objectResultWithError.setErrorCode("ERR_CODE");
        phabricatorHttpClient.checkNoError(objectResultWithError);
    }
}
