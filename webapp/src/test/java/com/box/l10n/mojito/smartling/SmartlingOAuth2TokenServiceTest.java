package com.box.l10n.mojito.smartling;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SmartlingOAuth2TokenServiceTest {

  @Mock private HttpClient httpClient;

  @Mock private HttpResponse<String> mockResponse;

  private SmartlingOAuth2TokenService smartlingOAuth2TokenService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    smartlingOAuth2TokenService =
        new SmartlingOAuth2TokenService(
            "clientId",
            "clientSecret",
            "https://test.local/auth",
            "https://test.local/auth/refresh");
    smartlingOAuth2TokenService.client = httpClient;
  }

  @Test
  public void getAccessTokenRequestsNewTokenWhenRefreshTokenExpired() throws Exception {
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);
    when(mockResponse.body())
        .thenReturn(
            "{\"response\": {\"data\": {\"accessToken\": \"newToken\", \"expiresIn\": 3600, \"refreshToken\": \"refreshToken\", \"refreshExpiresIn\": 1}}}");

    smartlingOAuth2TokenService.getAccessToken(); // First call to get the token
    String accessToken =
        smartlingOAuth2TokenService.getAccessToken(); // Second call should request a new token

    assertEquals("newToken", accessToken);
    verify(httpClient, times(2)).send(any(), any());
  }

  @Test
  public void getAccessTokenRequestsNewTokenWhenNoTokenExists() throws Exception {
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);
    when(mockResponse.body())
        .thenReturn(
            "{\"response\": {\"data\": {\"accessToken\": \"newToken\", \"expiresIn\": 3600, \"refreshToken\": \"refreshToken\", \"refreshExpiresIn\": 7200}}}");

    String accessToken = smartlingOAuth2TokenService.getAccessToken();

    assertEquals("newToken", accessToken);
    verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
  }

  @Test
  public void getRefreshedAccessTokenTokenWhenAccessTokenExpired() throws Exception {
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);
    when(mockResponse.body())
        .thenReturn(
            "{\"response\": {\"data\": {\"accessToken\": \"newToken\", \"expiresIn\": 1, \"refreshToken\": \"refreshToken\", \"refreshExpiresIn\": 7200}}}");

    String accessToken = smartlingOAuth2TokenService.getAccessToken();
    assertEquals("newToken", accessToken);

    when(mockResponse.body())
        .thenReturn(
            "{\"response\": {\"data\": {\"accessToken\": \"refreshedToken\", \"expiresIn\": 3600, \"refreshToken\": \"refreshToken\", \"refreshExpiresIn\": 7200}}}");
    Thread.sleep(2000);
    accessToken = smartlingOAuth2TokenService.getAccessToken();

    assertEquals("refreshedToken", accessToken);
    verify(httpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
  }
}
