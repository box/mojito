package com.box.l10n.mojito.rest.resttemplate;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * A Rest Template with {@link HttpComponentsClientHttpRequestFactory} that exposes the cookie
 * store.
 *
 * @author wyau
 */
@Component
public class CookieStoreRestTemplate extends RestTemplate {

  CookieStore cookieStore;

  public CookieStoreRestTemplate() {
    super();
    cookieStore = new BasicCookieStore();
    setCookieStoreAndUpdateRequestFactory(cookieStore);
  }

  public void setCookieStoreAndUpdateRequestFactory(CookieStore cookieStore) {
    setCookieStoreAndUpdateRequestFactory(cookieStore, null);
  }

  public void setCookieStoreAndUpdateRequestFactory(
      CookieStore cookieStore, Integer readTimeoutSeconds) {
    this.cookieStore = cookieStore;

    // We have to turn off auto redirect in the rest template because
    // when session expires, it will return a 302 and resttemplate
    // will automatically redirect to /login even before returning
    // the ClientHttpResponse in the interceptor
    HttpClientBuilder builder =
        HttpClientBuilder.create().setDefaultCookieStore(cookieStore).disableRedirectHandling();

    if (readTimeoutSeconds != null && readTimeoutSeconds > 0) {
      RequestConfig requestConfig =
          RequestConfig.custom().setResponseTimeout(Timeout.ofSeconds(readTimeoutSeconds)).build();
      builder.setDefaultRequestConfig(requestConfig);
    }

    HttpClient hc = builder.build();
    setRequestFactory(new HttpComponentsClientHttpRequestFactory(hc));
  }

  public CookieStore getCookieStore() {
    return cookieStore;
  }
}
