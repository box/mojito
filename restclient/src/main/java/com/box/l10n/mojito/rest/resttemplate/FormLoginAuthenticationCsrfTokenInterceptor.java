package com.box.l10n.mojito.rest.resttemplate;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

/**
 * An interceptor that will check to see if there's a valid request csrf.
 *
 * @author wyau
 */
@Component
public class FormLoginAuthenticationCsrfTokenInterceptor implements ClientHttpRequestInterceptor {

  /** logger */
  Logger logger = LoggerFactory.getLogger(FormLoginAuthenticationCsrfTokenInterceptor.class);

  public static final String CSRF_PARAM_NAME = "_csrf";
  public static final String CSRF_HEADER_NAME = "X-CSRF-TOKEN";
  public static final String COOKIE_SESSION_NAME = "JSESSIONID";

  @Autowired FormLoginConfig formLoginConfig;

  /** The {@link AuthenticatedRestTemplate} to which this interceptor is being used. */
  @Autowired AuthenticatedRestTemplate authRestTemplate;

  @Autowired ResttemplateConfig resttemplateConfig;

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

  /** Init */
  @PostConstruct
  protected void init() {

    restTemplateForAuthenticationFlow = new CookieStoreRestTemplate();
    cookieStore = restTemplateForAuthenticationFlow.getCookieStore();

    logger.debug(
        "Inject cookie store used in the rest template for authentication flow into the authRestTemplate so that they will match");
    authRestTemplate.restTemplate.setCookieStoreAndUpdateRequestFactory(cookieStore);

    List<ClientHttpRequestInterceptor> interceptors =
        Collections.<ClientHttpRequestInterceptor>singletonList(
            new ClientHttpRequestInterceptor() {
              @Override
              public ClientHttpResponse intercept(
                  HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
                  throws IOException {
                if (latestCsrfToken != null) {
                  // At the beginning of auth flow, there's no token yet
                  injectCsrfTokenIntoHeader(request, latestCsrfToken);
                }
                return execution.execute(request, body);
              }
            });

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
    if (doesSessionIdInCookieStoreExistAndMatchLatestSessionId()) {
      injectCsrfTokenIntoHeader(request, latestCsrfToken);
    } else {
      startAuthenticationAndInjectCsrfToken(request);
    }

    ClientHttpResponse clientHttpResponse = execution.execute(request, body);

    clientHttpResponse = handleResponse(request, body, execution, clientHttpResponse);

    return clientHttpResponse;
  }

  /**
   * Handle http response from the intercept. It will check to see if the initial response was
   * successful (ie. error status such as 301, 403). If so, it'll try the authentication flow again.
   * If it further encounters an unsuccessful response, then it'll throw a {@link
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
  protected void startAuthenticationAndInjectCsrfToken(HttpRequest request) {
    logger.debug(
        "Authenticate because no session is found in cookie store or it doesn't match with the one used to get the CSRF token we have.");
    startAuthenticationFlow();

    logger.debug("Injecting CSRF token");
    injectCsrfTokenIntoHeader(request, latestCsrfToken);
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

  /** @return null if no sesson id cookie is found */
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
  protected void injectCsrfTokenIntoHeader(HttpRequest request, CsrfToken csrfToken) {
    if (csrfToken == null) {
      throw new SessionAuthenticationException("There is no CSRF token to inject");
    }

    logger.debug(
        "Injecting CSRF token into request {} header: {}", request.getURI(), csrfToken.getToken());
    request.getHeaders().add(csrfToken.getHeaderName(), csrfToken.getToken());
  }

  /**
   * Starts the traditioanl form login authentication flow handshake. Consequencially, the cookie
   * store (which contains the session id) and the CSRF token will be updated.
   *
   * @throws AuthenticationException
   */
  protected synchronized void startAuthenticationFlow() throws AuthenticationException {
    logger.debug("Getting authenticated session");

    logger.debug(
        "Start by loading up the login form to get a valid unauthenticated session and CSRF token");
    ResponseEntity<String> loginResponseEntity =
        restTemplateForAuthenticationFlow.getForEntity(
            authRestTemplate.getURIForResource(formLoginConfig.getLoginFormPath()), String.class);

    latestCsrfToken = getCsrfTokenFromLoginHtml(loginResponseEntity.getBody());
    latestSessionIdForLatestCsrfToken = getAuthenticationSessionIdFromCookieStore();
    logger.debug(
        "Update CSRF token for interceptor ({}) from login form", latestCsrfToken.getToken());

    MultiValueMap<String, Object> loginPostParams = new LinkedMultiValueMap<>();
    loginPostParams.add("username", credentialProvider.getUsername());
    loginPostParams.add("password", credentialProvider.getPassword());

    logger.debug(
        "Post to login url to startAuthenticationFlow with user={}, pwd={}",
        credentialProvider.getUsername(),
        credentialProvider.getPassword());
    ResponseEntity<String> postLoginResponseEntity =
        restTemplateForAuthenticationFlow.postForEntity(
            authRestTemplate.getURIForResource(formLoginConfig.getLoginFormPath()),
            loginPostParams,
            String.class);

    // TODO(P1) This current way of checking if authentication is successful is somewhat
    // hacky. Bascailly it says that authentication is successful if a 302 is returned
    // and the redirect (from location header) maps to the login redirect path from the config.
    URI locationURI = URI.create(postLoginResponseEntity.getHeaders().get("Location").get(0));
    String expectedLocation =
        resttemplateConfig.getContextPath() + "/" + formLoginConfig.getLoginRedirectPath();

    if (postLoginResponseEntity.getStatusCode().equals(HttpStatus.FOUND)
        && expectedLocation.equals(locationURI.getPath())) {

      latestCsrfToken =
          getCsrfTokenFromEndpoint(
              authRestTemplate.getURIForResource(formLoginConfig.getCsrfTokenPath()));
      latestSessionIdForLatestCsrfToken = getAuthenticationSessionIdFromCookieStore();

      logger.debug(
          "Update CSRF token interceptor in AuthRestTempplate ({})", latestCsrfToken.getToken());

    } else {
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

  /**
   * Gets the CSRF token from login html because the CSRF token endpoint needs to be authenticated
   * first.
   *
   * @param loginHtml The login page HTML which contains the csrf token. It is assumed that the CSRF
   *     token is embedded on the page inside an input field with name matching {@link
   *     com.box.l10n.mojito.rest.resttemplate.FormLoginAuthenticationCsrfTokenInterceptor#CSRF_PARAM_NAME}
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
  protected CsrfToken getCsrfTokenFromEndpoint(String csrfTokenUrl) {
    ResponseEntity<String> csrfTokenEntity =
        restTemplateForAuthenticationFlow.getForEntity(csrfTokenUrl, String.class, "");
    logger.debug("CSRF token from {} is {}", csrfTokenUrl, csrfTokenEntity.getBody());
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
