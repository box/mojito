package com.box.l10n.mojito.rest.resttemplate;

import com.box.l10n.mojito.rest.resttemplate.stateless.BearerTokenInterceptor;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Wrapper around {@link RestTemplate} to implement authentication and to provide helpers for
 * building URI and perform regular requests.
 *
 * <p>{@link AuthenticatedRestTemplate} delegates all HTTP requests to a {@link RestTemplate}
 * instance.
 *
 * @author wyau
 */
@Component
public class AuthenticatedRestTemplate {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(AuthenticatedRestTemplate.class);

  @Autowired ResttemplateConfig resttemplateConfig;

  /** Will delegate calls to the {@link RestTemplate} instance that was configured */
  @Autowired CookieStoreRestTemplate restTemplate;

  /** Used to intercept requests and inject CSRF token */
  @Autowired
  FormLoginAuthenticationCsrfTokenInterceptor formLoginAuthenticationCsrfTokenInterceptor;

  @Autowired RestTemplateUtil restTemplateUtil;

  @Autowired(required = false)
  BearerTokenInterceptor bearerTokenInterceptor;

  /** Initialize the internal restTemplate instance */
  @PostConstruct
  protected void init() {
    logger.debug("Create the RestTemplate instance that will be wrapped");

    makeRestTemplateWithCustomObjectMapper(restTemplate);
    setErrorHandlerWithLogging(restTemplate);

    ResttemplateConfig.AuthenticationMode mode = resttemplateConfig.getAuthenticationMode();

    switch (mode) {
      case STATELESS -> {
        logger.info("Using stateless Bearer token interceptor");
        if (bearerTokenInterceptor == null) {
          throw new IllegalStateException(
              "Stateless auth selected but BearerTokenInterceptor is not available. Ensure MSAL config is set.");
        }
        List<ClientHttpRequestInterceptor> interceptors =
            Collections.<ClientHttpRequestInterceptor>singletonList(bearerTokenInterceptor);
        restTemplate.setRequestFactory(
            new InterceptingClientHttpRequestFactory(
                restTemplate.getRequestFactory(), interceptors));
      }
      case HEADER -> {
        logger.info("Using header-based authentication");
        Map<String, String> headers = resttemplateConfig.getHeader().getHeaders();
        if (headers == null || headers.isEmpty()) {
          throw new IllegalStateException(
              "Header authentication selected but no headers have been configured (l10n.resttemplate.header.headers)");
        }
        List<ClientHttpRequestInterceptor> interceptors =
            Collections.<ClientHttpRequestInterceptor>singletonList(
                new HeaderAuthenticationInterceptor(headers));
        restTemplate.setRequestFactory(
            new InterceptingClientHttpRequestFactory(
                restTemplate.getRequestFactory(), interceptors));
      }
      case STATEFUL -> {
        logger.info("Using stateful CSRF interceptor");
        List<ClientHttpRequestInterceptor> interceptors =
            Collections.<ClientHttpRequestInterceptor>singletonList(
                formLoginAuthenticationCsrfTokenInterceptor);
        restTemplate.setRequestFactory(
            new InterceptingClientHttpRequestFactory(
                restTemplate.getRequestFactory(), interceptors));
      }
    }
  }

  void setErrorHandlerWithLogging(RestTemplate restTemplate) {
    this.restTemplate.setErrorHandler(
        new DefaultResponseErrorHandler() {
          @Override
          public void handleError(ClientHttpResponse response) throws IOException {
            try {
              super.handleError(response);
            } catch (HttpClientErrorException e) {
              logger.debug(e.getResponseBodyAsString());
              throw e;
            }
          }
        });
  }

  /**
   * Gets a customized {@link MappingJackson2HttpMessageConverter} to process payload from TMS Rest
   * API.
   *
   * @return
   */
  protected void makeRestTemplateWithCustomObjectMapper(RestTemplate restTemplate) {
    logger.debug("Getting new rest template");

    for (HttpMessageConverter<?> httpMessageConverter : restTemplate.getMessageConverters()) {
      if (httpMessageConverter instanceof MappingJackson2HttpMessageConverter) {
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter =
            (MappingJackson2HttpMessageConverter) httpMessageConverter;

        // Bug with application/x-spring-data-compact+json:
        // https://jira.spring.io/browse/DATAREST-404
        logger.debug(
            "Setting supported media type to just JSON. The Accept header will be updated accordingly to application/json");
        mappingJackson2HttpMessageConverter.setSupportedMediaTypes(
            Arrays.asList(MediaType.APPLICATION_JSON));

        logger.debug("Creating custom jackson2 objectmapper with serialization inclusion changes");
        Jackson2ObjectMapperFactoryBean jackson2ObjectMapperFactoryBean =
            new Jackson2ObjectMapperFactoryBean();
        jackson2ObjectMapperFactoryBean.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        jackson2ObjectMapperFactoryBean.afterPropertiesSet();

        ObjectMapper objectMapper = jackson2ObjectMapperFactoryBean.getObject();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // To keep backward compatibility with the Joda output, disable write/reading nano seconds
        // with
        // Java time and ZonedDateTime
        // also see {@link com.box.l10n.mojito.json.ObjectMapper}
        objectMapper.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
        objectMapper.disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
        mappingJackson2HttpMessageConverter.setObjectMapper(objectMapper);
      }

      // The default encoding is set to ISO-8559-1 for String type, which is why we have to override
      // it here
      // For more info: https://jira.spring.io/browse/SPR-9099
      // TODO investigate but this should probalby replaced with setting the right header:
      // headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
      if (httpMessageConverter instanceof StringHttpMessageConverter) {
        StringHttpMessageConverter stringHttpMessageConverter =
            (StringHttpMessageConverter) httpMessageConverter;
        stringHttpMessageConverter.setSupportedMediaTypes(
            Arrays.asList(MediaType.parseMediaType("text/plain;charset=UTF-8"), MediaType.ALL));
      }
    }
  }

  /**
   * Gets the full URI (scheme + host + port + path) to access the REST WS for a given resource
   * path.
   *
   * <p>If the resource path starts with the http scheme it is considered already as a full URI and
   * is returned as it.
   *
   * @param resourcePath the resource path (possibly a full URI)
   * @return full URI of the REST WS
   */
  public String getURIForResource(String resourcePath) {

    return restTemplateUtil.getURIForResource(resourcePath);
  }

  /**
   * Gets the full URI to access the REST WS for a given resource path. It also adds any query
   * parameter.
   *
   * @param resourcePath the resource path (possibly a full URI)
   * @param queryStringParams parameters to construct the URI query string
   * @return full URI of the REST WS
   */
  protected URI getURIForResourceAndQueryStringParams(
      String resourcePath, Map<String, ?> queryStringParams) {

    UriComponentsBuilder uriBuilder =
        UriComponentsBuilder.fromHttpUrl(getURIForResource(resourcePath));

    for (Map.Entry<String, ?> entry : queryStringParams.entrySet()) {
      uriBuilder.queryParam(entry.getKey(), entry.getValue());
    }

    return uriBuilder.build(false).toUri();
  }

  /**
   * Delegate, see {@link RestTemplate#getForObject(String, Class, Object...)}
   *
   * @param resourcePath resource path transformed into final URI by this instance
   * @param responseType
   * @param <T>
   * @return
   * @throws RestClientException
   */
  public <T> T getForObject(String resourcePath, Class<T> responseType) throws RestClientException {
    return restTemplate.getForObject(getURIForResource(resourcePath), responseType);
  }

  protected <T> T getForObjectWithQueryStringParams(
      String resourcePath, Class<T> responseType, Map<String, ?> queryStringParams)
      throws RestClientException {
    return restTemplate.getForObject(
        getURIForResourceAndQueryStringParams(resourcePath, queryStringParams), responseType);
  }

  /**
   * @see AuthenticatedRestTemplate#getForObject(String, Class)
   * @param resourcePath resource path transformed into final URI by this instance
   * @param responseType
   * @param <T>
   * @return
   * @throws RestClientException
   */
  public <T> List<T> getForObjectAsList(String resourcePath, Class<T[]> responseType)
      throws RestClientException {
    return Arrays.asList(getForObject(resourcePath, responseType));
  }

  public <T> List<T> getForObjectAsListWithQueryStringParams(
      String resourcePath, Class<T[]> responseType, Map<String, ?> queryStringParams)
      throws RestClientException {
    return Arrays.asList(
        getForObjectWithQueryStringParams(resourcePath, responseType, queryStringParams));
  }

  /**
   * @see RestTemplate#getForEntity(String, Class, Object...)
   * @param resourcePath resource path transformed into final URI by this instance
   * @param responseType
   * @param <T>
   * @return
   * @throws RestClientException
   */
  public <T> ResponseEntity<T> getForEntity(String resourcePath, Class<T> responseType)
      throws RestClientException {
    return restTemplate.getForEntity(getURIForResource(resourcePath), responseType);
  }

  /**
   * Perform a GET request, using {@link ParameterizedTypeReference} to pass a generic type as
   * return type.
   *
   * @see RestTemplate#exchange(java.lang.String, org.springframework.http.HttpMethod,
   *     org.springframework.http.HttpEntity, org.springframework.core.ParameterizedTypeReference,
   *     java.util.Map)
   * @param <T> response body type
   * @param resourcePath resource path transformed into final URI by this instance
   * @param responseType
   * @param queryStringParams
   * @return
   * @throws RestClientException
   */
  public <T> ResponseEntity<T> getForEntityWithQueryParams(
      String resourcePath,
      ParameterizedTypeReference<T> responseType,
      Map<String, ?> queryStringParams)
      throws RestClientException {
    return restTemplate.exchange(
        getURIForResourceAndQueryStringParams(resourcePath, queryStringParams),
        HttpMethod.GET,
        HttpEntity.EMPTY,
        responseType);
  }

  /**
   * Delegate, see {@link RestTemplate#postForObject(String, Object, Class, Map) }
   *
   * @param resourcePath resource path transformed into final URI by this instance
   * @param request
   * @param responseType
   * @param <T>
   * @return
   * @throws RestClientException
   */
  public <T> T postForObject(String resourcePath, Object request, Class<T> responseType)
      throws RestClientException {
    return restTemplate.postForObject(getURIForResource(resourcePath), request, responseType);
  }

  /**
   * Similar to {@link #postForObject(String, Object, Class)}
   *
   * @param resourcePath
   * @param request
   * @param responseType
   * @param <T>
   * @return
   * @throws RestClientException
   */
  public <T> T putForObject(String resourcePath, Object request, Class<T> responseType)
      throws RestClientException {
    ResponseEntity<T> exchange =
        restTemplate.exchange(
            getURIForResource(resourcePath),
            HttpMethod.PUT,
            new HttpEntity<>(request),
            responseType);
    return exchange.getBody();
  }

  /**
   * Delegate, see {@link RestTemplate#postForEntity(String, Object, Class, Object...)}
   *
   * @param resourcePath resource path transformed into final URI by this instance
   * @param request
   * @param responseType
   * @param <T>
   * @return
   * @throws RestClientException
   */
  public <T> ResponseEntity<T> postForEntity(
      String resourcePath, Object request, Class<T> responseType) throws RestClientException {
    return restTemplate.postForEntity(getURIForResource(resourcePath), request, responseType);
  }

  public <T> T deleteForObject(String resourcePath, HttpEntity request, Class<T> responseType)
      throws RestClientException {
    return restTemplate
        .exchange(getURIForResource(resourcePath), HttpMethod.DELETE, request, responseType)
        .getBody();
  }

  public void delete(String resourcePath, HttpEntity request) throws RestClientException {
    deleteForObject(resourcePath, request, Void.class);
  }

  /**
   * Delegate, see {@link RestTemplate#delete(String)}
   *
   * @param resourcePath resource path transformed into final URI by this instance
   * @throws RestClientException
   */
  public void delete(String resourcePath) throws RestClientException {
    restTemplate.delete(getURIForResource(resourcePath));
  }

  /**
   * Perform a PATCH request, see {@link RestTemplate#exchange(java.lang.String,
   * org.springframework.http.HttpMethod, org.springframework.http.HttpEntity, java.lang.Class,
   * java.util.Map) }
   *
   * @param resourcePath resource path transformed into final URI by this instance
   * @param request
   * @throws RestClientException
   */
  public void patch(String resourcePath, Object request) throws RestClientException {
    restTemplate.exchange(
        getURIForResource(resourcePath), HttpMethod.PATCH, new HttpEntity<>(request), Void.class);
  }

  /**
   * Delegate, see {@link RestTemplate#put(java.net.URI, java.lang.Object)}
   *
   * @param resourcePath
   * @param request
   */
  public void put(String resourcePath, Object request) {
    restTemplate.put(getURIForResource(resourcePath), request);
  }

  @VisibleForTesting
  public CookieStoreRestTemplate getRestTemplate() {
    return restTemplate;
  }
}
