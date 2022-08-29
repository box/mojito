package com.box.l10n.mojito.rest.resttemplate;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestClientException;

@RunWith(SpringRunner.class)
@Configuration
@ComponentScan(basePackageClasses = {AuthenticatedRestTemplate.class})
@SpringBootTest(classes = {AuthenticatedRestTemplateTest.class})
@EnableConfigurationProperties
public class AuthenticatedRestTemplateTest {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(AuthenticatedRestTemplateTest.class);

  @Autowired AuthenticatedRestTemplate authenticatedRestTemplate;

  @Autowired ResttemplateConfig resttemplateConfig;

  @Autowired FormLoginConfig formLoginConfig;

  protected static final String LOGIN_PAGE_HTML =
      "<html>\n"
          + "<head>\n"
          + "</head>\n"
          + "<script type=\"text/javascript\">\n"
          + "   CSRF_TOKEN = '87023b1d-0e6f-48dd-bb06-d80802954572';\n"
          + "</script>\n"
          + "<body>\n"
          + "</body>\n"
          + "</html>";

  protected WireMockServer wireMockServer;

  @Before
  public void before() {
    wireMockServer = new WireMockServer(0);
    wireMockServer.start();

    // resets the mock server that was set inside the rest template
    authenticatedRestTemplate.restTemplate = new CookieStoreRestTemplate();
    authenticatedRestTemplate.init();

    // if port was 0, then the server will randomize it on start up, and now we
    // want to get that value back
    int port = wireMockServer.port();
    logger.debug("Wiremock server is running on port = {}", port);
    resttemplateConfig.setPort(port);
    WireMock.configureFor("localhost", port);
  }

  @After
  public void after() {
    wireMockServer.stop();
  }

  @Test
  public void testDoubleEncodedUrlForGetForObject() {
    initialAuthenticationMock();
    mockCsrfTokenEndpoint();

    WireMock.stubFor(
        WireMock.get(WireMock.urlEqualTo("/api/assets?path=abc%5Cdef"))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HttpStatus.FOUND.value())
                    .withHeader("Location", "/api/assets?path=abc%5Cdefs")
                    .withBody("")));

    Map<String, String> uriVariables = new HashMap<>();
    uriVariables.put("path", "abc\\def");
    authenticatedRestTemplate.getForObjectWithQueryStringParams(
        "/api/assets", String.class, uriVariables);

    WireMock.verify(WireMock.getRequestedFor(WireMock.urlEqualTo("/api/assets?path=abc%5Cdef")));
  }

  @Test
  public void testUnPreparedAuthRestTemplateAndSessionTimeoutForGetForObject() {
    initialAuthenticationMock();
    mockCsrfTokenEndpoint();

    WireMock.stubFor(
        WireMock.get(WireMock.urlEqualTo("/session-timeout-endpoint"))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HttpStatus.FOUND.value())
                    .withHeader("Location", "/login")
                    .withBody("")));

    String response = null;
    try {
      response = authenticatedRestTemplate.getForObject("session-timeout-endpoint", String.class);
    } catch (RestClientException e) {
      logger.debug(
          "Expecting this to fail because the response for /session-timeout-endpoint has been stubbed to be always returning a 302");
      Assert.assertEquals(
          "Tried to re-authenticate but the response remains to be unauthenticated",
          e.getMessage());
    }

    Assert.assertNull("There should have been no response", response);

    // Expecting 2 requests for each because initially, the AuthenticatedRestTemplate has not been
    // prepared,
    // there hasn't been any session set or csrf token, so it'll authenticate first.
    // Then it'll resume the original request (ie. session-timeout-endpoint) but the response
    // session timeout, so it
    // retries the auth flow again, and lastly, tries the session-timeout-endpoint again.
    // So in summary, this is the following expected flow:
    // 1. GET /login - because it has not been prepped, it starts auth flow
    // 2. POST /login
    // 3. GET /session-timeout-endpoint - response 302/login
    // 4. GET /login
    // 5. POST /login
    // 6. GET /session-timeout-endpoint
    WireMock.verify(
        2,
        WireMock.getRequestedFor(WireMock.urlMatching("/login"))
            .withHeader("Accept", WireMock.matching("text/plain.*")));
    WireMock.verify(
        2,
        WireMock.postRequestedFor(WireMock.urlMatching("/login"))
            .withHeader("Accept", WireMock.matching("text/plain.*")));
    WireMock.verify(
        2,
        WireMock.getRequestedFor(WireMock.urlMatching("/session-timeout-endpoint"))
            .withHeader("Accept", WireMock.matching("text/plain.*"))
            .withHeader("X-CSRF-TOKEN", WireMock.matching("madeup-csrf-value")));
  }

  @Test
  public void testAuthRestTemplateForSessionTimeoutWithPostForObject() {
    initialAuthenticationMock();
    mockCsrfTokenEndpoint();

    logger.debug("Mock returns 403 for POST request when session timeout");
    WireMock.stubFor(
        WireMock.post(WireMock.urlEqualTo("/random-403-endpoint"))
            .willReturn(WireMock.aResponse().withStatus(HttpStatus.FORBIDDEN.value())));

    String response = null;
    try {
      response =
          authenticatedRestTemplate.postForObject(
              "random-403-endpoint", new HashMap<String, String>(), String.class);
    } catch (RestClientException e) {
      logger.debug(
          "Expecting this to fail because the response for /random-403-endpoint has been stubbed to be always returning a 403");
      Assert.assertEquals(
          "Tried to re-authenticate but the response remains to be unauthenticated",
          e.getMessage());
    }

    Assert.assertNull(response);

    WireMock.verify(
        2,
        WireMock.getRequestedFor(WireMock.urlMatching("/login"))
            .withHeader("Accept", WireMock.matching("text/plain.*")));
    WireMock.verify(
        2,
        WireMock.postRequestedFor(WireMock.urlMatching("/login"))
            .withHeader("Accept", WireMock.matching("text/plain.*")));
    WireMock.verify(
        2,
        WireMock.postRequestedFor(WireMock.urlMatching("/random-403-endpoint"))
            .withHeader("Accept", WireMock.matching("text/plain.*"))
            .withHeader("X-CSRF-TOKEN", WireMock.matching("madeup-csrf-value")));
  }

  @Test
  public void testAuthRestTemplateFor401() {
    initialAuthenticationMock();
    mockCsrfTokenEndpoint();

    logger.debug("Mock returns 401 for POST request when session timeout");
    WireMock.stubFor(
        WireMock.post(WireMock.urlEqualTo("/random-401-endpoint"))
            .willReturn(WireMock.aResponse().withStatus(HttpStatus.UNAUTHORIZED.value())));

    String response = null;
    try {
      response =
          authenticatedRestTemplate.postForObject(
              "random-401-endpoint", new HashMap<String, String>(), String.class);
    } catch (RestClientException e) {
      logger.debug(
          "Expecting this to fail because the response for /random-401-endpoint has been stubbed to be always returning a 401");
      Assert.assertEquals(
          "Tried to re-authenticate but the response remains to be unauthenticated",
          e.getMessage());
    }

    Assert.assertNull(response);

    WireMock.verify(
        2,
        WireMock.getRequestedFor(WireMock.urlMatching("/login"))
            .withHeader("Accept", WireMock.matching("text/plain.*")));
    WireMock.verify(
        2,
        WireMock.postRequestedFor(WireMock.urlMatching("/login"))
            .withHeader("Accept", WireMock.matching("text/plain.*")));
    WireMock.verify(
        2,
        WireMock.postRequestedFor(WireMock.urlMatching("/random-401-endpoint"))
            .withHeader("Accept", WireMock.matching("text/plain.*"))
            .withHeader("X-CSRF-TOKEN", WireMock.matching("madeup-csrf-value")));
  }

  protected void initialAuthenticationMock() {
    String expectedResponse = "expected content";

    WireMock.stubFor(
        WireMock.get(WireMock.urlEqualTo("/" + formLoginConfig.getLoginFormPath()))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withHeader("Content-Type", "text/html")
                    .withBody(LOGIN_PAGE_HTML)));

    WireMock.stubFor(
        WireMock.post(WireMock.urlEqualTo("/" + formLoginConfig.getLoginPostPath()))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HttpStatus.FOUND.value())
                    .withHeader("Content-Type", "text/html")
                    .withHeader(
                        "Location",
                        authenticatedRestTemplate.getURIForResource(
                            formLoginConfig.getLoginRedirectPath()))
                    .withBody(expectedResponse)));
  }

  protected void mockCsrfTokenEndpoint() {
    WireMock.stubFor(
        WireMock.get(WireMock.urlEqualTo("/" + formLoginConfig.getCsrfTokenPath()))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withHeader("Content-Type", "text/html")
                    .withBody("madeup-csrf-value")));
  }
}
