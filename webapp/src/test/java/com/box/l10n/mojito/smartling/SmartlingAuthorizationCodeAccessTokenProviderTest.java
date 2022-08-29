package com.box.l10n.mojito.smartling;

import static org.mockito.ArgumentMatchers.*;

import com.box.l10n.mojito.smartling.response.AuthenticationData;
import com.box.l10n.mojito.smartling.response.AuthenticationResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.web.client.RestTemplate;

public class SmartlingAuthorizationCodeAccessTokenProviderTest {

  @Spy SmartlingAuthorizationCodeAccessTokenProvider smartlingAuthorizationCodeAccessTokenProvider;

  @Mock RestTemplate mockRestTemplate;

  @Mock SmartlingOAuth2ProtectedResourceDetails mockResourceDetails;

  @Mock AccessTokenRequest mockAccessTokenRequest;

  @Mock AuthenticationResponse mockResp;

  AuthenticationData mockAuthData = new AuthenticationData();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    Mockito.when(smartlingAuthorizationCodeAccessTokenProvider.getRestTemplate())
        .thenReturn(mockRestTemplate);
    Mockito.when(mockResourceDetails.getClientId()).thenReturn("testClient");
    Mockito.when(mockResourceDetails.getClientSecret()).thenReturn("testSecret");
    Mockito.when(mockResourceDetails.getAccessTokenUri()).thenReturn("http://test.com/accessToken");
    Mockito.when(mockResourceDetails.getRefreshUri())
        .thenReturn("http://test.com/accessToken/refresh");
    Mockito.when(mockRestTemplate.postForObject(anyString(), anyMap(), any())).thenReturn(mockResp);

    mockAuthData.setAccessToken("testAccessToken");
    mockAuthData.setExpiresIn(480);
    mockAuthData.setRefreshToken("testRefreshToken");
    mockAuthData.setRefreshExpiresIn(21700);

    Mockito.when(mockAccessTokenRequest.getExistingToken()).thenReturn(null);
    Mockito.when(mockResp.getData()).thenReturn(mockAuthData);
  }

  @Test
  public void testRefreshTokenMethodNotCalledIfNoExistingToken() {

    OAuth2AccessToken accessToken =
        smartlingAuthorizationCodeAccessTokenProvider.obtainAccessToken(
            mockResourceDetails, mockAccessTokenRequest);
    Mockito.verify(smartlingAuthorizationCodeAccessTokenProvider, Mockito.times(0))
        .refreshAccessToken(
            isA(OAuth2ProtectedResourceDetails.class),
            isA(OAuth2RefreshToken.class),
            isA(AccessTokenRequest.class));
    Assert.assertEquals(mockAuthData.getAccessToken(), accessToken.getValue());
    Assert.assertEquals(mockAuthData.getRefreshToken(), accessToken.getRefreshToken().getValue());
  }

  @Test
  public void testRefreshTokenMethodNotCalledIfNoRefreshToken() {

    DefaultOAuth2AccessToken accessToken = new DefaultOAuth2AccessToken("testAccessToken");
    accessToken.setRefreshToken(null);
    Mockito.when(mockAccessTokenRequest.getExistingToken()).thenReturn(accessToken);
    smartlingAuthorizationCodeAccessTokenProvider.obtainAccessToken(
        mockResourceDetails, mockAccessTokenRequest);
    Mockito.verify(smartlingAuthorizationCodeAccessTokenProvider, Mockito.times(0))
        .refreshAccessToken(
            isA(OAuth2ProtectedResourceDetails.class),
            isA(OAuth2RefreshToken.class),
            isA(AccessTokenRequest.class));
  }

  @Test
  public void testTokenIsRefreshedIfRefreshTokenIsPresent() {

    OAuth2AccessToken accessToken =
        smartlingAuthorizationCodeAccessTokenProvider.obtainAccessToken(
            mockResourceDetails, mockAccessTokenRequest);
    Mockito.verify(smartlingAuthorizationCodeAccessTokenProvider, Mockito.times(0))
        .refreshAccessToken(
            isA(OAuth2ProtectedResourceDetails.class),
            isA(OAuth2RefreshToken.class),
            isA(AccessTokenRequest.class));
    Mockito.when(mockAccessTokenRequest.getExistingToken()).thenReturn(accessToken);
    smartlingAuthorizationCodeAccessTokenProvider.obtainAccessToken(
        mockResourceDetails, mockAccessTokenRequest);
    Mockito.verify(smartlingAuthorizationCodeAccessTokenProvider, Mockito.times(1))
        .refreshAccessToken(
            isA(OAuth2ProtectedResourceDetails.class),
            isA(OAuth2RefreshToken.class),
            isA(AccessTokenRequest.class));
  }
}
