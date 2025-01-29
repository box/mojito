package com.box.l10n.mojito.rest.resttemplate;

import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * An interceptor that will check to see if there's a valid request csrf.
 *
 * @author wyau
 */
@Component
public class LoginAuthenticationCsrfTokenInterceptor implements ClientHttpRequestInterceptor {

  /** logger */
  Logger logger = LoggerFactory.getLogger(LoginAuthenticationCsrfTokenInterceptor.class);

  public static final String CSRF_PARAM_NAME = "_csrf";
  public static final String CSRF_HEADER_NAME = "X-CSRF-TOKEN";
  public static final String COOKIE_SESSION_NAME = "JSESSIONID";

  @Autowired FormLoginConfig formLoginConfig;

  @Autowired ResttemplateConfig resttemplateConfig;

  @Autowired RestTemplateUtil restTemplateUtil;

  /**
   * This is used for the authentication flow to keep things separate from the restTemplate that
   * this interceptor is intercepting
   */
  CookieStoreRestTemplate restTemplateForAuthenticationFlow;

  /**
   * This is so that we obtain access to the cookie store used inside HttpClient to check to see if
   * we have a session
   */
  CookieStore cookieStore;

  /** This is the latest session id that was used to obtain the {@link this#latestCsrfToken} */
  String latestSessionIdForLatestCsrfToken;

  /**
   * This is the lastest CSRF token that was obtained from the {@link
   * this#latestSessionIdForLatestCsrfToken}
   */
  CsrfToken latestCsrfToken = null;

  @Autowired CredentialProvider credentialProvider;

  /** Will delegate calls to the {@link RestTemplate} instance that was configured */
  @Autowired CookieStoreRestTemplate restTemplate;

  @Autowired(required = false)
  ProxyOutboundRequestInterceptor proxyOutboundRequestInterceptor;

  /** Init */
  @PostConstruct
  protected void init() {

    restTemplateForAuthenticationFlow = new CookieStoreRestTemplate();
    cookieStore = restTemplateForAuthenticationFlow.getCookieStore();

    logger.debug(
        "Inject cookie store used in the rest template for authentication flow into the restTemplate so that they will match");
    restTemplate.setCookieStoreAndUpdateRequestFactory(cookieStore);

    List<ClientHttpRequestInterceptor> interceptors =
        Stream.of(
                proxyOutboundRequestInterceptor,
                new ClientHttpRequestInterceptor() {
                  @Override
                  public ClientHttpResponse intercept(
                      HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
                      throws IOException {
                    if (latestCsrfToken != null) {
                      // At the beginning of auth flow, there's no token yet
                      HttpRequest newRequest = injectCsrfTokenIntoHeader(request, latestCsrfToken);
                      return execution.execute(newRequest, body);
                    }
                    return execution.execute(request, body);
                  }
                })
            .filter(Objects::nonNull)
            .toList();

    restTemplateForAuthenticationFlow.setRequestFactory(
        new InterceptingClientHttpRequestFactory(
            restTemplateForAuthenticationFlow.getRequestFactory(), interceptors));
  }

  /**
   * This intercepts a request then will either use the existing session, or will authenticate to
   * obtain a proper session, or will inject the request with the correct CSRF token.
   *
   * <p>Flow example: 1. GET /login - because it has not been prepped, it starts auth flow 2. POST
   * /login 3. GET /session-timeout-endpoint - response 302/login 4. GET /login 5. POST /login 6.
   * GET /session-timeout-endpoint
   *
   * @param request
   * @param body
   * @param execution
   * @return
   * @throws IOException
   */
  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    HttpRequest modifiedRequest;
    if (doesSessionIdInCookieStoreExistAndMatchLatestSessionId()) {
      modifiedRequest = injectCsrfTokenIntoHeader(request, latestCsrfToken);
    } else {
      modifiedRequest = startAuthenticationAndInjectCsrfToken(request);
    }

    ClientHttpResponse clientHttpResponse = execution.execute(modifiedRequest, body);

    clientHttpResponse = handleResponse(modifiedRequest, body, execution, clientHttpResponse);

    return clientHttpResponse;
  }

  /**
   * Handle http response from the intercept. It will check to see if the initial response was
   * successful (i.e. error status such as 301, 403). If so, it'll try the authentication flow
   * again. If it further encounters an unsuccessful response, then it'll throw a {@link
   * RestClientException}
   *
   * @param request
   * @param body
   * @param execution
   * @param clientHttpResponse
   * @return
   * @throws IOException
   */
  protected ClientHttpResponse handleResponse(
      HttpRequest request,
      byte[] body,
      ClientHttpRequestExecution execution,
      ClientHttpResponse clientHttpResponse)
      throws IOException {
    if (isForbiddened(clientHttpResponse) || isUnauthenticated(clientHttpResponse)) {
      reauthenticate(request);

      clientHttpResponse = execution.execute(request, body);

      if (isForbiddened(clientHttpResponse) || isUnauthenticated(clientHttpResponse)) {
        throw new RestClientException(
            "Tried to re-authenticate but the response remains to be unauthenticated");
      }
    }
    return clientHttpResponse;
  }

  /**
   * Checks to see if response was {@link HttpStatus#FORBIDDEN}
   *
   * @param clientHttpResponse
   * @return
   * @throws IOException
   */
  private Boolean isForbiddened(ClientHttpResponse clientHttpResponse) throws IOException {
    return clientHttpResponse.getStatusCode().equals(HttpStatus.FORBIDDEN);
  }

  /**
   * Reauthenticate the {@link AuthenticatedRestTemplate}
   *
   * @param request
   */
  private void reauthenticate(HttpRequest request) {
    logger.debug("Reseting authentication");
    resetAuthentication();

    logger.debug("Authenticate again");
    startAuthenticationAndInjectCsrfToken(request);
  }

  /**
   * Starts authentication flow and inject csrf token
   *
   * @param request
   */
  protected HttpRequest startAuthenticationAndInjectCsrfToken(HttpRequest request) {
    logger.debug(
        "Authenticate because no session is found in cookie store or it doesn't match with the one used to get the CSRF token we have.");
    // TODO: Remove logic once PreAuth logic is configured on all environments
    startAuthenticationFlow(resttemplateConfig.usesLoginAuthentication());

    logger.debug("Injecting CSRF token");
    return injectCsrfTokenIntoHeader(request, latestCsrfToken);
  }

  /**
   * Gets the authenticated session id. If it is not found, an authentication flow will be started
   * so that a proper session id is available
   *
   * @return
   */
  protected boolean doesSessionIdInCookieStoreExistAndMatchLatestSessionId() {
    logger.debug(
        "Check to see if session id in cookie store matches the session id used to get the latest CSRF token");
    String sessionId = getAuthenticationSessionIdFromCookieStore();

    return sessionId != null && sessionId.equals(latestSessionIdForLatestCsrfToken);
  }

  /**
   * @return null if no sesson id cookie is found
   */
  protected String getAuthenticationSessionIdFromCookieStore() {
    List<Cookie> cookies = cookieStore.getCookies();
    for (Cookie cookie : cookies) {
      if (cookie.getName().equals(COOKIE_SESSION_NAME)) {
        String cookieValue = cookie.getValue();
        logger.debug("Found session cookie: {}", cookieValue);
        return cookieValue;
      }
    }

    return null;
  }

  /**
   * @param request the request, containing method, URI, and headers
   * @param csrfToken the CSRF token to be injected into the request header
   */
  protected HttpRequest injectCsrfTokenIntoHeader(HttpRequest request, CsrfToken csrfToken) {
    if (csrfToken == null) {
      throw new SessionAuthenticationException("There is no CSRF token to inject");
    }

    logger.debug(
        "Injecting CSRF token into request {} token: {}", request.getURI(), csrfToken.getToken());
    HttpHeaders headers = new HttpHeaders();
    headers.add(csrfToken.getHeaderName(), csrfToken.getToken());
    return new CustomHttpRequestWrapper(request, headers);
  }

  /**
   * If preAuthentication is enabled, then we merely get the CSRF token If preAuthentication is not
   * enabled, start the traditional form login authentication flow handshake.
   *
   * <p>Consequentially, the cookie store (which contains the session id) and the CSRF token will be
   * updated.
   *
   * @throws AuthenticationException
   */
  protected synchronized void startAuthenticationFlow(boolean usesLoginAuthentication)
      throws AuthenticationException {
    logger.debug("Getting authenticated session");

    if (usesLoginAuthentication) {
      logger.debug(
          "Start by loading up the login form to get a valid unauthenticated session and CSRF token");
      ResponseEntity<String> loginResponseEntity =
          restTemplateForAuthenticationFlow.getForEntity(
              restTemplateUtil.getURIForResource(formLoginConfig.getLoginFormPath()), String.class);

      logger.debug("login Resonse status code {}", loginResponseEntity.getStatusCode());
      if (!loginResponseEntity.hasBody()) {
        throw new SessionAuthenticationException(
            "Authentication failed: no CSRF token could be found.  GET login status code = "
                + loginResponseEntity.getStatusCode());
      }

      latestCsrfToken = getCsrfTokenFromLoginHtml(loginResponseEntity.getBody());
      latestSessionIdForLatestCsrfToken = getAuthenticationSessionIdFromCookieStore();
      logger.debug(
          "Update CSRF token for interceptor ({}) from login form", latestCsrfToken.getToken());

      MultiValueMap<String, Object> loginPostParams = new LinkedMultiValueMap<>();
      loginPostParams.add("username", credentialProvider.getUsername());
      loginPostParams.add("password", credentialProvider.getPassword());
      loginPostParams.add("_csrf", latestCsrfToken.getToken());

      logger.debug(
          "Post to login url to startAuthenticationFlow with user={}, pwd={}",
          credentialProvider.getUsername(),
          credentialProvider.getPassword());
      ResponseEntity<String> postLoginResponseEntity =
          restTemplateForAuthenticationFlow.postForEntity(
              restTemplateUtil.getURIForResource(formLoginConfig.getLoginFormPath()),
              loginPostParams,
              String.class);

      // TODO(P1) This current way of checking if authentication is successful is somewhat
      // hacky. Basically it says that authentication is successful if a 302 is returned
      // and the redirect (from location header) maps to the login redirect path from the config.
      URI locationURI = URI.create(postLoginResponseEntity.getHeaders().get("Location").get(0));
      String expectedLocation =
          resttemplateConfig.getContextPath() + "/" + formLoginConfig.getLoginRedirectPath();

      boolean isAuthenticated =
          postLoginResponseEntity.getStatusCode().equals(HttpStatus.FOUND)
              && expectedLocation.equals(locationURI.getPath());

      if (!isAuthenticated) {
        throw new SessionAuthenticationException(
            "Authentication failed.  Post login status code = "
                + postLoginResponseEntity.getStatusCode()
                + ", location = ["
                + locationURI.getPath()
                + "], expected location = ["
                + expectedLocation
                + "]");
      }
    }

    latestCsrfToken =
        getCsrfTokenFromEndpoint(
            restTemplateUtil.getURIForResource(formLoginConfig.getCsrfTokenPath()));
    latestSessionIdForLatestCsrfToken = getAuthenticationSessionIdFromCookieStore();

    logger.debug(
        "Update CSRF token interceptor in AuthRestTemplate ({})", latestCsrfToken.getToken());
  }

  /**
   * Gets the CSRF token from login html because the CSRF token endpoint needs to be authenticated
   * first.
   *
   * @param loginHtml The login page HTML which contains the csrf token. It is assumed that the CSRF
   *     token is embedded on the page inside an input field with name matching {@link
   *     LoginAuthenticationCsrfTokenInterceptor#CSRF_PARAM_NAME}
   * @return
   * @throws AuthenticationException
   */
  protected CsrfToken getCsrfTokenFromLoginHtml(String loginHtml) throws AuthenticationException {
    Pattern pattern = Pattern.compile("CSRF_TOKEN = '(.*?)';");
    Matcher matcher = pattern.matcher(loginHtml);

    if (matcher.find()) {
      String csrfTokenString = matcher.group(1);

      logger.debug("CSRF token from login html: {}", csrfTokenString);
      return new DefaultCsrfToken(CSRF_HEADER_NAME, CSRF_PARAM_NAME, csrfTokenString);
    } else {
      throw new SessionAuthenticationException("Could not find CSRF_TOKEN variable on login page");
    }
  }

  /**
   * Use the CSRF token endpoint to get the CSRF token corresponding to this session
   *
   * @param csrfTokenUrl The full URL to which the CSRF token can be obtained
   * @return
   */
  protected CsrfToken getCsrfTokenFromEndpoint(String csrfTokenUrl)
      throws SessionAuthenticationException {
    ResponseEntity<String> csrfTokenEntity =
        restTemplateForAuthenticationFlow.getForEntity(csrfTokenUrl, String.class, "");
    logger.debug("CSRF token from {} is {}", csrfTokenUrl, csrfTokenEntity.getBody());
    if (csrfTokenEntity.getStatusCode().isError()) {
      throw new SessionAuthenticationException(
          "Authentication failed.  GET login status code = "
              + csrfTokenEntity.getStatusCode()
              + ", location = ["
              + csrfTokenUrl
              + "]");
    }

    return new DefaultCsrfToken(CSRF_HEADER_NAME, CSRF_PARAM_NAME, csrfTokenEntity.getBody());
  }

  @VisibleForTesting
  public void resetAuthentication() {
    cookieStore.clear();
    latestCsrfToken = null;
  }

  @VisibleForTesting
  public void setCredentialProvider(CredentialProvider credentialProvider) {
    this.credentialProvider = credentialProvider;
  }

  /**
   * Checks if the response status is unauthenticated
   *
   * @param clientHttpResponse
   * @return
   * @throws IOException
   */
  protected boolean isUnauthenticated(ClientHttpResponse clientHttpResponse) throws IOException {
    return (clientHttpResponse.getStatusCode().equals(HttpStatus.FOUND)
            && clientHttpResponse.getHeaders().getLocation().getPath().equals("/login"))
        || (clientHttpResponse.getStatusCode().equals(HttpStatus.UNAUTHORIZED));
  }
}
